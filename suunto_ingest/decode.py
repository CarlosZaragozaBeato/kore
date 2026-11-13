"""Decode local FIT files into queryable PostgreSQL tables."""

from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import sys

from garmin_fit_sdk import Decoder, Stream
import psycopg

from .fit import KEY_PATTERN, validate_fit
from .probe import ROOT, ProbeError, read_dotenv
from .sync import DEFAULT_DATABASE_URL


PARSER_VERSION = 2


def semicircles_to_degrees(value: int | float | None) -> float | None:
    return None if value is None else value * 180.0 / (2 ** 31)


def ensure_schema(db: psycopg.Connection) -> None:
    with db.transaction():
        db.execute("ALTER TABLE suunto_workouts ADD COLUMN IF NOT EXISTS fit_parsed_sha256 TEXT")
        db.execute("ALTER TABLE suunto_workouts ADD COLUMN IF NOT EXISTS fit_record_count INTEGER")
        db.execute("ALTER TABLE suunto_workouts ADD COLUMN IF NOT EXISTS fit_lap_count INTEGER")
        db.execute("ALTER TABLE suunto_workouts ADD COLUMN IF NOT EXISTS fit_session_count INTEGER")
        db.execute("ALTER TABLE suunto_workouts ADD COLUMN IF NOT EXISTS fit_parser_version INTEGER")
        db.execute("""CREATE TABLE IF NOT EXISTS suunto_fit_sessions (
            workout_key TEXT NOT NULL REFERENCES suunto_workouts(workout_key),
            session_index INTEGER NOT NULL,
            sport TEXT, start_time TIMESTAMPTZ,
            timer_seconds DOUBLE PRECISION, distance_meters DOUBLE PRECISION,
            avg_heart_rate DOUBLE PRECISION, max_heart_rate DOUBLE PRECISION,
            avg_power DOUBLE PRECISION, training_stress_score DOUBLE PRECISION,
            PRIMARY KEY (workout_key, session_index))""")
        db.execute("""CREATE TABLE IF NOT EXISTS suunto_fit_laps (
            workout_key TEXT NOT NULL REFERENCES suunto_workouts(workout_key),
            lap_index INTEGER NOT NULL,
            start_time TIMESTAMPTZ,
            timer_seconds DOUBLE PRECISION, distance_meters DOUBLE PRECISION,
            avg_heart_rate DOUBLE PRECISION, max_heart_rate DOUBLE PRECISION,
            avg_power DOUBLE PRECISION,
            PRIMARY KEY (workout_key, lap_index))""")
        db.execute("""CREATE TABLE IF NOT EXISTS suunto_fit_records (
            workout_key TEXT NOT NULL REFERENCES suunto_workouts(workout_key),
            record_index INTEGER NOT NULL,
            observed_at TIMESTAMPTZ,
            heart_rate DOUBLE PRECISION, power_watts DOUBLE PRECISION,
            cadence DOUBLE PRECISION, speed_mps DOUBLE PRECISION,
            distance_meters DOUBLE PRECISION, altitude_meters DOUBLE PRECISION,
            position_lat DOUBLE PRECISION, position_long DOUBLE PRECISION,
            PRIMARY KEY (workout_key, record_index))""")
        db.execute("CREATE INDEX IF NOT EXISTS suunto_fit_records_time_idx ON suunto_fit_records (observed_at)")


def decode_file(path: Path) -> tuple[list[dict], list[dict], list[dict]]:
    validate_fit(path.read_bytes())
    try:
        messages, errors = Decoder(Stream.from_file(str(path))).read()
    except Exception as exc:
        raise ProbeError("Unable to decode a local FIT file") from exc
    if errors:
        raise ProbeError(f"FIT decoder reported {len(errors)} errors")
    return (messages.get("session_mesgs", []), messages.get("lap_mesgs", []), messages.get("record_mesgs", []))


def store_workout(db: psycopg.Connection, key: str, digest: str, sessions: list[dict], laps: list[dict], records: list[dict]) -> None:
    with db.transaction():
        for table in ("suunto_fit_records", "suunto_fit_laps", "suunto_fit_sessions"):
            db.execute(f"DELETE FROM {table} WHERE workout_key = %s", (key,))
        with db.cursor() as cursor:
            with cursor.copy("""COPY suunto_fit_sessions
                (workout_key, session_index, sport, start_time, timer_seconds,
                 distance_meters, avg_heart_rate, max_heart_rate, avg_power, training_stress_score)
                FROM STDIN""") as copy:
                for i, row in enumerate(sessions):
                    copy.write_row((key, i, row.get("sport"), row.get("start_time"), row.get("total_timer_time"),
                                    row.get("total_distance"), row.get("avg_heart_rate"), row.get("max_heart_rate"),
                                    row.get("avg_power"), row.get("training_stress_score")))
            with cursor.copy("""COPY suunto_fit_laps
                (workout_key, lap_index, start_time, timer_seconds, distance_meters,
                 avg_heart_rate, max_heart_rate, avg_power) FROM STDIN""") as copy:
                for i, row in enumerate(laps):
                    copy.write_row((key, i, row.get("start_time"), row.get("total_timer_time"), row.get("total_distance"),
                                    row.get("avg_heart_rate"), row.get("max_heart_rate"), row.get("avg_power")))
            with cursor.copy("""COPY suunto_fit_records
                (workout_key, record_index, observed_at, heart_rate, power_watts,
                 cadence, speed_mps, distance_meters, altitude_meters, position_lat, position_long)
                FROM STDIN""") as copy:
                for i, row in enumerate(records):
                    copy.write_row((key, i, row.get("timestamp"), row.get("heart_rate"), row.get("power"),
                                    row.get("cadence"), row.get("enhanced_speed", row.get("speed")), row.get("distance"),
                                    row.get("enhanced_altitude", row.get("altitude")),
                                    semicircles_to_degrees(row.get("position_lat")),
                                    semicircles_to_degrees(row.get("position_long"))))
        db.execute("""UPDATE suunto_workouts SET fit_parsed_sha256 = %s,
            fit_record_count = %s, fit_lap_count = %s, fit_session_count = %s,
            fit_parser_version = %s WHERE workout_key = %s""",
            (digest, len(records), len(laps), len(sessions), PARSER_VERSION, key))


def run(max_files: int = 50, root: Path = ROOT) -> tuple[int, int, int]:
    config = read_dotenv(root / ".env")
    database_url = os.environ.get("KORE_DATABASE_URL") or config.get("KORE_DATABASE_URL", DEFAULT_DATABASE_URL)
    with psycopg.connect(database_url, autocommit=True, connect_timeout=10) as db:
        ensure_schema(db)
        rows = db.execute("""SELECT workout_key, fit_sha256 FROM suunto_workouts
            WHERE fit_status = 'ok' AND
                (fit_parsed_sha256 IS DISTINCT FROM fit_sha256 OR fit_parser_version IS DISTINCT FROM %s)
            ORDER BY start_time_ms DESC NULLS LAST LIMIT %s""", (PARSER_VERSION, max_files)).fetchall()
        record_total = lap_total = 0
        for i, (key, digest) in enumerate(rows, 1):
            if not KEY_PATTERN.fullmatch(key):
                raise ProbeError("Database contains an invalid workout key")
            path = root / "data" / "raw" / "suunto" / "fit" / f"{key}.fit"
            data = path.read_bytes()
            if hashlib.sha256(data).hexdigest() != digest:
                raise ProbeError("Local FIT hash does not match PostgreSQL")
            sessions, laps, records = decode_file(path)
            store_workout(db, key, digest, sessions, laps, records)
            record_total += len(records)
            lap_total += len(laps)
            if i % 25 == 0 or i == len(rows):
                print(f"Decoded {i}/{len(rows)} FIT files", flush=True)
        return len(rows), record_total, lap_total


def main() -> int:
    parser = argparse.ArgumentParser(description="Decode local Suunto FIT files into PostgreSQL")
    parser.add_argument("--max-files", type=int, default=50)
    args = parser.parse_args()
    if args.max_files < 1:
        parser.error("--max-files must be positive")
    try:
        count, records, laps = run(args.max_files)
    except (ProbeError, OSError, psycopg.Error) as exc:
        print(f"FIT decoding failed: {type(exc).__name__}", file=sys.stderr)
        return 1
    print(f"Decoded {count} FIT files: {records} records, {laps} laps")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
