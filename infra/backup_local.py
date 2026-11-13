"""Private backup and isolated restore check for this machine's Suunto store."""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import secrets
import shutil
import socket
import subprocess
import sys
import tarfile
import tempfile
from urllib.parse import unquote, urlsplit

import psycopg

from suunto_ingest.probe import ROOT, read_dotenv
from suunto_ingest.sync import DEFAULT_DATABASE_URL


DATA = ROOT / "data"
BACKUPS = DATA / "backups"
RUNTIME = DATA / "postgres-runtime" / "usr" / "bin"
TABLES = ("suunto_workouts", "suunto_fit_sessions", "suunto_fit_laps", "suunto_fit_records")


def binary(name: str) -> str:
    candidate = RUNTIME / name
    return str(candidate) if candidate.is_file() else name


def run_command(args: list[str], *, env: dict[str, str] | None = None) -> None:
    result = subprocess.run(args, env=env, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True)
    if result.returncode:
        raise RuntimeError(f"{Path(args[0]).name} failed (exit {result.returncode})")


def source_url() -> str:
    return os.environ.get("KORE_DATABASE_URL") or read_dotenv(ROOT / ".env").get(
        "KORE_DATABASE_URL", DEFAULT_DATABASE_URL
    )


def digest(path: Path) -> str:
    sha = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            sha.update(chunk)
    return sha.hexdigest()


def counts(db: psycopg.Connection) -> dict[str, int]:
    return {table: db.execute(f"SELECT count(*) FROM {table}").fetchone()[0] for table in TABLES}


def create() -> Path:
    BACKUPS.mkdir(parents=True, exist_ok=True, mode=0o700)
    os.chmod(BACKUPS, 0o700)
    token = DATA / "secrets" / "suunto-token.json"
    raw = DATA / "raw"
    if not token.is_file() or not raw.is_dir():
        raise RuntimeError("Suunto raw files or rotated token are missing")
    temporary = Path(tempfile.mkdtemp(prefix=".kore-backup-", dir=BACKUPS))
    destination = BACKUPS / f"kore-{datetime.now(timezone.utc):%Y%m%dT%H%M%SZ}-{secrets.token_hex(3)}"
    try:
        with psycopg.connect(source_url(), autocommit=True, connect_timeout=10) as db:
            for name in ("kore:pipeline", "kore:suunto"):
                if not db.execute("SELECT pg_try_advisory_lock(hashtext(%s))", (name,)).fetchone()[0]:
                    raise RuntimeError("Suunto ingestion is running; retry the backup later")
            url = urlsplit(source_url())
            if url.scheme not in ("postgres", "postgresql") or not url.hostname or not url.path.strip("/"):
                raise RuntimeError("Invalid KORE_DATABASE_URL")
            env = os.environ.copy()
            env["PGPASSWORD"] = unquote(url.password or "")
            run_command([
                binary("pg_dump"), "--format=custom", "--no-owner", "--no-acl",
                "--file", str(temporary / "database.dump"),
                "--host", url.hostname, "--port", str(url.port or 5432),
                "--username", unquote(url.username or ""), unquote(url.path.lstrip("/")),
            ], env=env)
            with tarfile.open(temporary / "raw-and-token.tar.gz", "w:gz") as archive:
                archive.add(raw, arcname="raw", recursive=True)
                archive.add(token, arcname="secrets/suunto-token.json")
            expected = counts(db)
        manifest = {
            "created_at": datetime.now(timezone.utc).isoformat(),
            "counts": expected,
            "sha256": {name: digest(temporary / name) for name in ("database.dump", "raw-and-token.tar.gz")},
        }
        (temporary / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
        for path in temporary.iterdir():
            os.chmod(path, 0o600)
        os.replace(temporary, destination)
        return destination
    finally:
        if temporary.exists():
            shutil.rmtree(temporary)


def free_port() -> int:
    with socket.socket() as listener:
        listener.bind(("127.0.0.1", 0))
        return listener.getsockname()[1]


def verify(backup: Path) -> dict[str, int]:
    backup = backup.resolve()
    manifest = json.loads((backup / "manifest.json").read_text())
    for name in ("database.dump", "raw-and-token.tar.gz"):
        if digest(backup / name) != manifest["sha256"][name]:
            raise RuntimeError(f"Backup checksum mismatch: {name}")
    with tempfile.TemporaryDirectory(prefix="kore-restore-", dir=DATA) as work_dir, \
            tempfile.TemporaryDirectory(prefix="kr-socket-", dir="/tmp") as socket_dir:
        work = Path(work_dir)
        cluster = work / "cluster"
        run_command([binary("initdb"), "-D", str(cluster), "-U", "kore_restore", "--auth=trust", "--no-instructions"])
        port = free_port()
        started = False
        try:
            run_command([binary("pg_ctl"), "-D", str(cluster), "-l", str(work / "server.log"),
                         "-o", f"-c listen_addresses='' -k {socket_dir} -p {port}", "start"])
            started = True
            run_command([binary("pg_restore"), "--exit-on-error", "--no-owner", "--no-acl",
                         "-h", socket_dir, "-p", str(port), "-U", "kore_restore",
                         "-d", "postgres", str(backup / "database.dump")])
            with psycopg.connect(host=socket_dir, port=port, user="kore_restore", dbname="postgres") as db:
                actual = counts(db)
                if actual != manifest["counts"]:
                    raise RuntimeError("Restored table counts differ from the backup manifest")
                fits = db.execute("SELECT workout_key, fit_sha256 FROM suunto_workouts WHERE fit_status = 'ok'").fetchall()
            with tarfile.open(backup / "raw-and-token.tar.gz", "r:gz") as archive:
                archive.extractall(work / "files", filter="data")
            token_path = work / "files" / "secrets" / "suunto-token.json"
            if not json.loads(token_path.read_text()).get("refresh_token"):
                raise RuntimeError("Restored Suunto refresh token is invalid")
            for key, expected_hash in fits:
                path = work / "files" / "raw" / "suunto" / "fit" / f"{key}.fit"
                if not path.is_file() or digest(path) != expected_hash:
                    raise RuntimeError("Restored FIT files do not match the database")
            return actual
        finally:
            if started:
                run_command([binary("pg_ctl"), "-D", str(cluster), "-m", "immediate", "stop"])


def main() -> int:
    parser = argparse.ArgumentParser(description="Back up or verify the local Suunto store")
    sub = parser.add_subparsers(dest="action", required=True)
    create_parser = sub.add_parser("create")
    create_parser.add_argument("--verify", action="store_true", help="Restore-check the new backup")
    check = sub.add_parser("verify")
    check.add_argument("backup", type=Path)
    args = parser.parse_args()
    os.umask(0o077)
    try:
        if args.action == "create":
            path = create()
            print(f"Backup created: {path}")
            if args.verify:
                result = verify(path)
                print(f"Restore verified: {result}")
        else:
            result = verify(args.backup)
            print(f"Restore verified: {result}")
    except Exception as exc:
        print(f"Backup operation failed ({type(exc).__name__})", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
