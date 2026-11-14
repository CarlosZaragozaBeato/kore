"""Run and record the complete, resumable Suunto ingestion pipeline."""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import os
import sys
from uuid import uuid4

import psycopg

from . import decode, fit, sync
from .probe import ROOT, read_dotenv


def database_url() -> str:
    return os.environ.get("KORE_DATABASE_URL") or read_dotenv(ROOT / ".env").get(
        "KORE_DATABASE_URL", sync.DEFAULT_DATABASE_URL
    )


def ensure_run_table(db: psycopg.Connection) -> None:
    db.execute("""CREATE TABLE IF NOT EXISTS suunto_pipeline_runs (
        run_id UUID PRIMARY KEY,
        started_at TIMESTAMPTZ NOT NULL,
        finished_at TIMESTAMPTZ,
        status TEXT NOT NULL CHECK (status IN ('running', 'succeeded', 'failed', 'interrupted')),
        stage TEXT NOT NULL,
        summaries INTEGER NOT NULL DEFAULT 0,
        fit_downloaded INTEGER NOT NULL DEFAULT 0,
        fit_missing INTEGER NOT NULL DEFAULT 0,
        fit_decoded INTEGER NOT NULL DEFAULT 0,
        error_type TEXT
    )""")


def run(batch_size: int = 50) -> dict[str, int]:
    if batch_size < 1:
        raise ValueError("batch_size must be positive")
    with psycopg.connect(database_url(), autocommit=True, connect_timeout=10) as db:
        ensure_run_table(db)
        if not db.execute("SELECT pg_try_advisory_lock(hashtext('kore:pipeline'))").fetchone()[0]:
            raise RuntimeError("A Suunto pipeline is already running")
        db.execute("""UPDATE suunto_pipeline_runs SET status = 'interrupted',
            finished_at = NOW(), error_type = 'Interrupted'
            WHERE status = 'running'""")
        run_id = uuid4()
        db.execute("""INSERT INTO suunto_pipeline_runs (run_id, started_at, status, stage)
            VALUES (%s, %s, 'running', 'summaries')""", (run_id, datetime.now(timezone.utc)))
        counts = {"summaries": 0, "fit_downloaded": 0, "fit_missing": 0, "fit_decoded": 0}
        stage = "summaries"
        try:
            counts["summaries"], _ = sync.run()
            stage = "fit"
            db.execute("UPDATE suunto_pipeline_runs SET stage = %s, summaries = %s WHERE run_id = %s",
                       (stage, counts["summaries"], run_id))
            while True:
                downloaded, missing = fit.run(batch_size)
                counts["fit_downloaded"] += downloaded
                counts["fit_missing"] += missing
                db.execute("""UPDATE suunto_pipeline_runs SET fit_downloaded = %s,
                    fit_missing = %s WHERE run_id = %s""",
                    (counts["fit_downloaded"], counts["fit_missing"], run_id))
                if downloaded + missing < batch_size:
                    break
            stage = "decode"
            db.execute("UPDATE suunto_pipeline_runs SET stage = %s WHERE run_id = %s", (stage, run_id))
            while True:
                decoded, _, _ = decode.run(batch_size)
                counts["fit_decoded"] += decoded
                db.execute("UPDATE suunto_pipeline_runs SET fit_decoded = %s WHERE run_id = %s",
                           (counts["fit_decoded"], run_id))
                if decoded < batch_size:
                    break
            stage = "plan"
            db.execute("UPDATE suunto_pipeline_runs SET stage = %s WHERE run_id = %s", (stage, run_id))
            from plan_daily_sync import refresh_active
            from plan_workbook import ACTIVE
            counts["plan_workouts"] = refresh_active() if ACTIVE.exists() else 0
        except Exception as exc:
            db.execute("""UPDATE suunto_pipeline_runs SET status = 'failed', stage = %s,
                finished_at = NOW(), error_type = %s WHERE run_id = %s""",
                (stage, type(exc).__name__, run_id))
            raise
        db.execute("""UPDATE suunto_pipeline_runs SET status = 'succeeded', stage = 'complete',
            finished_at = NOW() WHERE run_id = %s""", (run_id,))
        return counts


def main() -> int:
    parser = argparse.ArgumentParser(description="Run the complete Suunto ingestion pipeline")
    parser.add_argument("--batch-size", type=int, default=50)
    args = parser.parse_args()
    if args.batch_size < 1:
        parser.error("--batch-size must be positive")
    try:
        counts = run(args.batch_size)
    except Exception as exc:
        print(f"Suunto pipeline failed ({type(exc).__name__}); see /api/v1/status", file=sys.stderr)
        return 1
    print(f"Suunto pipeline complete: {counts}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
