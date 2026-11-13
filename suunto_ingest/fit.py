"""Download Suunto FIT files in resumable batches."""

from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import re
import sys
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

import psycopg

from .probe import (
    ROOT,
    ProbeError,
    atomic_private_write,
    current_refresh_token,
    private_directory,
    read_dotenv,
    renew_token,
)
from .sync import DEFAULT_DATABASE_URL


KEY_PATTERN = re.compile(r"[0-9a-fA-F]{24}\Z")
REQUEST_DELAY_SECONDS = 1.0
UNAUTHORIZED_COOLDOWN_SECONDS = 60


class UnauthorizedFit(ProbeError):
    """The current Suunto access token was rejected."""


def ensure_fit_columns(connection: psycopg.Connection) -> None:
    with connection.transaction():
        connection.execute("ALTER TABLE suunto_workouts ADD COLUMN IF NOT EXISTS fit_sha256 TEXT")
        connection.execute("ALTER TABLE suunto_workouts ADD COLUMN IF NOT EXISTS fit_size BIGINT")
        connection.execute("ALTER TABLE suunto_workouts ADD COLUMN IF NOT EXISTS fit_status TEXT")
        connection.execute("ALTER TABLE suunto_workouts ADD COLUMN IF NOT EXISTS fit_fetched_at TIMESTAMPTZ")


def fetch_fit(access_token: str, subscription_key: str, key: str) -> bytes | None:
    request = Request(
        f"https://cloudapi.suunto.com/v3/workouts/{key}/fit",
        headers={
            "Authorization": f"Bearer {access_token}",
            "Ocp-Apim-Subscription-Key": subscription_key,
            "Accept": "application/octet-stream",
        },
    )
    for attempt in range(3):
        try:
            with urlopen(request, timeout=60) as response:
                return response.read()
        except HTTPError as exc:
            if exc.code == 404:
                return None
            if exc.code == 401:
                raise UnauthorizedFit("Suunto FIT request returned HTTP 401") from exc
            if exc.code not in (429, 500, 502, 503, 504) or attempt == 2:
                raise ProbeError(f"Suunto FIT request returned HTTP {exc.code}") from exc
            retry_after = exc.headers.get("Retry-After")
            delay = min(int(retry_after), 30) if retry_after and retry_after.isdigit() else 2 ** attempt
        except (URLError, TimeoutError) as exc:
            if attempt == 2:
                raise ProbeError(f"Suunto FIT connection failed ({type(exc).__name__})") from exc
            delay = 2 ** attempt
        time.sleep(delay)
    raise AssertionError("Retry loop did not return or raise")


def validate_fit(data: bytes) -> None:
    if len(data) < 14 or data[8:12] != b".FIT":
        raise ProbeError("Suunto returned a file without a FIT signature")
    declared_size = int.from_bytes(data[4:8], "little")
    header_size = data[0]
    if header_size not in (12, 14) or len(data) < header_size + declared_size + 2:
        raise ProbeError("Suunto returned a truncated FIT file")


def pending_keys(connection: psycopg.Connection, limit: int) -> list[str]:
    rows = connection.execute("""
        SELECT workout_key FROM suunto_workouts
        WHERE fit_status IS NULL
        ORDER BY start_time_ms DESC NULLS LAST, workout_key
        LIMIT %s
    """, (limit,)).fetchall()
    return [row[0] for row in rows]


def mark_fit(connection: psycopg.Connection, key: str, data: bytes | None) -> None:
    with connection.transaction():
        if data is None:
            connection.execute("""
                UPDATE suunto_workouts SET fit_status = 'missing', fit_fetched_at = NOW()
                WHERE workout_key = %s
            """, (key,))
        else:
            connection.execute("""
                UPDATE suunto_workouts
                SET fit_status = 'ok', fit_sha256 = %s, fit_size = %s, fit_fetched_at = NOW()
                WHERE workout_key = %s
            """, (hashlib.sha256(data).hexdigest(), len(data), key))


def run(max_files: int = 50, root: Path = ROOT, fetcher=fetch_fit) -> tuple[int, int]:
    config = read_dotenv(root / ".env")
    required = ("SUUNTO_CLIENT_ID", "SUUNTO_CLIENT_SECRET", "SUUNTO_SUBSCRIPTION_KEY", "SUUNTO_REFRESH_TOKEN")
    if any(not config.get(key) for key in required):
        raise ProbeError("Missing Suunto configuration in .env")
    private_directory(root / "data")
    database_url = os.environ.get("KORE_DATABASE_URL") or config.get("KORE_DATABASE_URL", DEFAULT_DATABASE_URL)
    with psycopg.connect(database_url, autocommit=True, connect_timeout=10) as connection:
        ensure_fit_columns(connection)
        locked = connection.execute(
            "SELECT pg_try_advisory_lock(hashtext('kore:suunto'))"
        ).fetchone()[0]
        if not locked:
            raise ProbeError("A Suunto FIT download is already running")
        keys = pending_keys(connection, max_files)
        if not keys:
            return 0, 0

        token_path = root / "data" / "secrets" / "suunto-token.json"
        refresh_token = current_refresh_token(token_path, config["SUUNTO_REFRESH_TOKEN"])
        access_token = renew_token(config["SUUNTO_CLIENT_ID"], config["SUUNTO_CLIENT_SECRET"], refresh_token, token_path)
        downloaded = 0
        missing = 0
        for index, key in enumerate(keys, 1):
            if not KEY_PATTERN.fullmatch(key):
                raise ProbeError("Database contains an invalid workout key")
            path = root / "data" / "raw" / "suunto" / "fit" / f"{key}.fit"
            if path.exists():
                data = path.read_bytes()
            else:
                try:
                    data = fetcher(access_token, config["SUUNTO_SUBSCRIPTION_KEY"], key)
                except UnauthorizedFit:
                    time.sleep(UNAUTHORIZED_COOLDOWN_SECONDS)
                    refresh_token = current_refresh_token(token_path, config["SUUNTO_REFRESH_TOKEN"])
                    access_token = renew_token(config["SUUNTO_CLIENT_ID"], config["SUUNTO_CLIENT_SECRET"], refresh_token, token_path)
                    data = fetcher(access_token, config["SUUNTO_SUBSCRIPTION_KEY"], key)
            if data is not None:
                validate_fit(data)
                if not path.exists():
                    atomic_private_write(path, data)
                downloaded += 1
            else:
                missing += 1
            mark_fit(connection, key, data)
            if index % 25 == 0 or index == len(keys):
                print(f"FIT progress: {index}/{len(keys)} ({downloaded} saved, {missing} unavailable)", flush=True)
            if index < len(keys):
                time.sleep(REQUEST_DELAY_SECONDS)
        return downloaded, missing


def main() -> int:
    parser = argparse.ArgumentParser(description="Download a batch of Suunto FIT files")
    parser.add_argument("--max-files", type=int, default=50, help="Maximum FIT files to process (default: 50)")
    args = parser.parse_args()
    if args.max_files < 1:
        parser.error("--max-files must be positive")
    try:
        downloaded, missing = run(args.max_files)
    except (ProbeError, OSError, psycopg.Error) as exc:
        if isinstance(exc, psycopg.Error):
            print(f"FIT download failed: PostgreSQL {exc.__class__.__name__}", file=sys.stderr)
        else:
            print(f"FIT download failed: {exc}", file=sys.stderr)
        return 1
    print(f"FIT batch complete: {downloaded} saved, {missing} unavailable")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
