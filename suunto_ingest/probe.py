"""Fetch one Suunto workout page and keep the raw response locally."""

from __future__ import annotations

import argparse
import base64
from datetime import datetime, timedelta, timezone
import json
import os
from pathlib import Path
import secrets
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[1]
OAUTH_URL = "https://cloudapi-oauth.suunto.com/oauth/token"
WORKOUTS_URL = "https://cloudapi.suunto.com/v3/workouts"
TIMEOUT_SECONDS = 20


class ProbeError(Exception):
    """An expected failure that can be reported without exposing secrets."""


def read_dotenv(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    try:
        for line in path.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            key, separator, value = line.partition("=")
            if not separator:
                raise ProbeError("Invalid .env format")
            values[key.strip()] = value.strip()
    except OSError as exc:
        raise ProbeError("Cannot read local .env") from exc
    return values


def private_directory(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True, mode=0o700)
    os.chmod(path, 0o700)


def atomic_private_write(path: Path, data: bytes) -> None:
    private_directory(path.parent)
    temporary = path.with_name(f".{path.name}.{secrets.token_hex(8)}.tmp")
    try:
        fd = os.open(temporary, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, "wb") as handle:
            handle.write(data)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, path)
        os.chmod(path, 0o600)
        directory_fd = os.open(path.parent, os.O_RDONLY | os.O_DIRECTORY)
        try:
            os.fsync(directory_fd)
        finally:
            os.close(directory_fd)
    finally:
        temporary.unlink(missing_ok=True)


def request_bytes(request: Request) -> bytes:
    try:
        with urlopen(request, timeout=TIMEOUT_SECONDS) as response:
            return response.read()
    except HTTPError as exc:
        raise ProbeError(f"Suunto returned HTTP {exc.code}") from exc
    except (URLError, TimeoutError) as exc:
        raise ProbeError(f"Suunto connection failed ({type(exc).__name__})") from exc


def current_refresh_token(token_path: Path, initial_token: str) -> str:
    if not token_path.exists():
        return initial_token
    try:
        token = json.loads(token_path.read_text(encoding="utf-8"))["refresh_token"]
    except (OSError, ValueError, KeyError, TypeError) as exc:
        raise ProbeError("Local refresh token file is invalid") from exc
    if not isinstance(token, str) or not token:
        raise ProbeError("Local refresh token file is invalid")
    return token


def renew_token(client_id: str, client_secret: str, refresh_token: str, token_path: Path) -> str:
    credentials = base64.b64encode(f"{client_id}:{client_secret}".encode()).decode("ascii")
    request = Request(
        OAUTH_URL,
        data=urlencode({"grant_type": "refresh_token", "refresh_token": refresh_token}).encode(),
        headers={"Authorization": f"Basic {credentials}", "Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
    )
    try:
        response = json.loads(request_bytes(request))
        access_token = response["access_token"]
        new_refresh_token = response["refresh_token"]
        if not isinstance(access_token, str) or not access_token or not isinstance(new_refresh_token, str) or not new_refresh_token:
            raise ValueError("empty token")
    except (ValueError, KeyError, TypeError) as exc:
        raise ProbeError("Suunto token response is invalid") from exc

    # Save the rotated token before any other API request.
    atomic_private_write(
        token_path,
        json.dumps({"refresh_token": new_refresh_token, "updated_at": datetime.now(timezone.utc).isoformat()}).encode(),
    )
    return access_token


def fetch_workouts(access_token: str, subscription_key: str, since_ms: int, until_ms: int, limit: int) -> bytes:
    query = urlencode({
        "since": since_ms,
        "until": until_ms,
        "limit": limit,
        "offset": 0,
        "filter-by-modification-time": "true",
    })
    request = Request(
        f"{WORKOUTS_URL}?{query}",
        headers={"Authorization": f"Bearer {access_token}", "Ocp-Apim-Subscription-Key": subscription_key, "Accept": "application/json"},
    )
    return request_bytes(request)


def parse_workout_count(raw: bytes) -> int:
    try:
        payload = json.loads(raw)["payload"]
    except (ValueError, KeyError, TypeError) as exc:
        raise ProbeError("Suunto workout response has no valid payload") from exc
    if not isinstance(payload, list):
        raise ProbeError("Suunto workout payload is not a list")
    return len(payload)


def run(days: int, limit: int, root: Path = ROOT) -> tuple[int, Path]:
    config = read_dotenv(root / ".env")
    required = ("SUUNTO_CLIENT_ID", "SUUNTO_CLIENT_SECRET", "SUUNTO_SUBSCRIPTION_KEY", "SUUNTO_REFRESH_TOKEN")
    if any(not config.get(key) for key in required):
        raise ProbeError("Missing Suunto configuration in .env")

    private_directory(root / "data")
    token_path = root / "data" / "secrets" / "suunto-token.json"
    refresh_token = current_refresh_token(token_path, config["SUUNTO_REFRESH_TOKEN"])
    access_token = renew_token(config["SUUNTO_CLIENT_ID"], config["SUUNTO_CLIENT_SECRET"], refresh_token, token_path)

    until = datetime.now(timezone.utc)
    since = until - timedelta(days=days)
    raw = fetch_workouts(access_token, config["SUUNTO_SUBSCRIPTION_KEY"], int(since.timestamp() * 1000), int(until.timestamp() * 1000), limit)
    count = parse_workout_count(raw)
    filename = f"{until.strftime('%Y%m%dT%H%M%SZ')}-{secrets.token_hex(4)}.json"
    raw_path = root / "data" / "raw" / "suunto" / "workouts" / filename
    atomic_private_write(raw_path, raw)
    atomic_private_write(
        raw_path.with_suffix(".meta.json"),
        json.dumps({
            "source": "suunto",
            "endpoint": "/v3/workouts",
            "retrieved_at": until.isoformat(),
            "since_ms": int(since.timestamp() * 1000),
            "until_ms": int(until.timestamp() * 1000),
            "limit": limit,
            "offset": 0,
            "filter_by_modification_time": True,
            "count": count,
        }).encode(),
    )
    return count, raw_path


def main() -> int:
    parser = argparse.ArgumentParser(description="Fetch and store one page of Suunto workout summaries")
    parser.add_argument("--days", type=int, default=30, help="Lookback window (default: 30)")
    parser.add_argument("--limit", type=int, default=50, help="Maximum workouts (default: 50)")
    args = parser.parse_args()
    if args.days < 1 or not 1 <= args.limit <= 50:
        parser.error("--days must be positive and --limit must be between 1 and 50")
    try:
        count, path = run(args.days, args.limit)
    except (ProbeError, OSError) as exc:
        print(f"Extraction failed: {exc}", file=sys.stderr)
        return 1
    print(f"Extraction complete: {count} workouts; raw response: {path.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
