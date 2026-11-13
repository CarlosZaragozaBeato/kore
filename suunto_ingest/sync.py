"""Synchronize Suunto workout summaries into local PostgreSQL."""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import secrets
import sys
import time
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

import psycopg
from psycopg.types.json import Jsonb

from .probe import (
    ROOT,
    WORKOUTS_URL,
    ProbeError,
    atomic_private_write,
    current_refresh_token,
    private_directory,
    read_dotenv,
    renew_token,
)


DEFAULT_DATABASE_URL = "postgresql://kore_user:kore_password@127.0.0.1:5432/kore_db"
OVERLAP_MS = 24 * 60 * 60 * 1000
MAX_RETRIES = 3


def ensure_schema(connection: psycopg.Connection) -> None:
    with connection.transaction():
        connection.execute("""
            CREATE TABLE IF NOT EXISTS suunto_workouts (
                workout_key TEXT PRIMARY KEY,
                last_modified_ms BIGINT,
                start_time_ms BIGINT,
                raw_payload JSONB NOT NULL,
                fetched_at TIMESTAMPTZ NOT NULL
            )
        """)
        connection.execute("""
            CREATE TABLE IF NOT EXISTS suunto_sync_state (
                source TEXT PRIMARY KEY,
                cursor_ms BIGINT NOT NULL,
                updated_at TIMESTAMPTZ NOT NULL
            )
        """)
        connection.execute("ALTER TABLE suunto_workouts ADD COLUMN IF NOT EXISTS fit_status TEXT")
        connection.execute("""
            INSERT INTO suunto_sync_state (source, cursor_ms, updated_at)
            VALUES ('workouts', 0, NOW())
            ON CONFLICT (source) DO NOTHING
        """)


def read_cursor(connection: psycopg.Connection) -> int:
    row = connection.execute(
        "SELECT cursor_ms FROM suunto_sync_state WHERE source = 'workouts'"
    ).fetchone()
    if row is None:
        raise ProbeError("Suunto sync state is missing")
    return row[0]


def fetch_page(access_token: str, subscription_key: str, since_ms: int, until_ms: int, limit: int, offset: int) -> bytes:
    query = urlencode({
        "since": since_ms,
        "until": until_ms,
        "limit": limit,
        "offset": offset,
        "filter-by-modification-time": "true",
    })
    request = Request(
        f"{WORKOUTS_URL}?{query}",
        headers={
            "Authorization": f"Bearer {access_token}",
            "Ocp-Apim-Subscription-Key": subscription_key,
            "Accept": "application/json",
        },
    )
    for attempt in range(MAX_RETRIES + 1):
        try:
            with urlopen(request, timeout=30) as response:
                return response.read()
        except HTTPError as exc:
            if exc.code not in (429, 500, 502, 503, 504) or attempt == MAX_RETRIES:
                raise ProbeError(f"Suunto returned HTTP {exc.code} at offset {offset}") from exc
        except (URLError, TimeoutError) as exc:
            if attempt == MAX_RETRIES:
                raise ProbeError(f"Suunto connection failed at offset {offset} ({type(exc).__name__})") from exc
        time.sleep(min(2 ** attempt, 8))
    raise AssertionError("Retry loop did not return or raise")


def parse_page(raw: bytes) -> list[dict]:
    try:
        response = json.loads(raw)
        if response.get("error"):
            raise ProbeError("Suunto reported an error in the workout response")
        payload = response["payload"]
    except (ValueError, KeyError, TypeError, AttributeError) as exc:
        raise ProbeError("Invalid Suunto workout response") from exc
    if not isinstance(payload, list):
        raise ProbeError("Suunto workout payload is not a list")
    for workout in payload:
        if not isinstance(workout, dict) or not isinstance(workout.get("workoutKey"), str) or not workout["workoutKey"]:
            raise ProbeError("Suunto workout has no valid workoutKey")
    return payload


def save_raw_page(root: Path, raw: bytes, since_ms: int, until_ms: int, limit: int, offset: int, count: int) -> Path:
    retrieved_at = datetime.now(timezone.utc)
    filename = f"{retrieved_at.strftime('%Y%m%dT%H%M%SZ')}-{secrets.token_hex(4)}.json"
    path = root / "data" / "raw" / "suunto" / "workouts" / filename
    atomic_private_write(path, raw)
    atomic_private_write(path.with_suffix(".meta.json"), json.dumps({
        "source": "suunto",
        "endpoint": "/v3/workouts",
        "retrieved_at": retrieved_at.isoformat(),
        "since_ms": since_ms,
        "until_ms": until_ms,
        "limit": limit,
        "offset": offset,
        "filter_by_modification_time": True,
        "count": count,
    }).encode())
    return path


def store_page(connection: psycopg.Connection, workouts: list[dict]) -> None:
    fetched_at = datetime.now(timezone.utc)
    with connection.transaction():
        for workout in workouts:
            connection.execute("""
                INSERT INTO suunto_workouts
                    (workout_key, last_modified_ms, start_time_ms, raw_payload, fetched_at)
                VALUES (%s, %s, %s, %s, %s)
                ON CONFLICT (workout_key) DO UPDATE SET
                    last_modified_ms = EXCLUDED.last_modified_ms,
                    start_time_ms = EXCLUDED.start_time_ms,
                    raw_payload = EXCLUDED.raw_payload,
                    fetched_at = EXCLUDED.fetched_at,
                    fit_status = CASE
                        WHEN suunto_workouts.last_modified_ms IS DISTINCT FROM EXCLUDED.last_modified_ms
                        THEN NULL ELSE suunto_workouts.fit_status END
            """, (
                workout["workoutKey"],
                workout.get("lastModified"),
                workout.get("startTime"),
                Jsonb(workout),
                fetched_at,
            ))


def advance_cursor(connection: psycopg.Connection, until_ms: int) -> None:
    with connection.transaction():
        connection.execute("""
            UPDATE suunto_sync_state
            SET cursor_ms = GREATEST(cursor_ms, %s), updated_at = NOW()
            WHERE source = 'workouts'
        """, (until_ms,))


def run(limit: int = 50, root: Path = ROOT, fetcher=fetch_page) -> tuple[int, int]:
    config = read_dotenv(root / ".env")
    required = ("SUUNTO_CLIENT_ID", "SUUNTO_CLIENT_SECRET", "SUUNTO_SUBSCRIPTION_KEY", "SUUNTO_REFRESH_TOKEN")
    if any(not config.get(key) for key in required):
        raise ProbeError("Missing Suunto configuration in .env")
    private_directory(root / "data")
    database_url = os.environ.get("KORE_DATABASE_URL") or config.get("KORE_DATABASE_URL", DEFAULT_DATABASE_URL)
    with psycopg.connect(database_url, autocommit=True, connect_timeout=10) as connection:
        ensure_schema(connection)
        locked = connection.execute(
            "SELECT pg_try_advisory_lock(hashtext('kore:suunto'))"
        ).fetchone()[0]
        if not locked:
            raise ProbeError("A Suunto synchronization is already running")
        cursor_ms = read_cursor(connection)
        since_ms = max(0, cursor_ms - OVERLAP_MS) if cursor_ms else 0
        until_ms = int(datetime.now(timezone.utc).timestamp() * 1000)

        token_path = root / "data" / "secrets" / "suunto-token.json"
        refresh_token = current_refresh_token(token_path, config["SUUNTO_REFRESH_TOKEN"])
        access_token = renew_token(config["SUUNTO_CLIENT_ID"], config["SUUNTO_CLIENT_SECRET"], refresh_token, token_path)

        offset = 0
        total = 0
        previous_page_keys: tuple[str, ...] | None = None
        while True:
            raw = fetcher(access_token, config["SUUNTO_SUBSCRIPTION_KEY"], since_ms, until_ms, limit, offset)
            workouts = parse_page(raw)
            page_keys = tuple(workout["workoutKey"] for workout in workouts)
            if workouts and page_keys == previous_page_keys:
                raise ProbeError("Suunto returned the same page twice; cursor was not advanced")
            previous_page_keys = page_keys
            save_raw_page(root, raw, since_ms, until_ms, limit, offset, len(workouts))
            store_page(connection, workouts)
            total += len(workouts)
            print(f"Stored page at offset {offset}: {len(workouts)} workouts", flush=True)
            if len(workouts) < limit:
                break
            offset += len(workouts)
        advance_cursor(connection, until_ms)
        return total, until_ms


def main() -> int:
    parser = argparse.ArgumentParser(description="Synchronize Suunto workouts into local PostgreSQL")
    parser.add_argument("--limit", type=int, default=50, help="Page size (1 to 50)")
    args = parser.parse_args()
    if not 1 <= args.limit <= 50:
        parser.error("--limit must be between 1 and 50")
    try:
        total, _ = run(args.limit)
    except (ProbeError, OSError, psycopg.Error) as exc:
        if isinstance(exc, psycopg.Error):
            print(f"Synchronization failed: PostgreSQL {exc.__class__.__name__}", file=sys.stderr)
        else:
            print(f"Synchronization failed: {exc}", file=sys.stderr)
        return 1
    print(f"Synchronization complete: {total} workouts processed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
