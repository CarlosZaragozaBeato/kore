"""Local, read-only training API. Run on 127.0.0.1 only."""

from datetime import date, datetime, timedelta, timezone
import os

from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel
import psycopg
from psycopg.rows import dict_row

from suunto_ingest.probe import ROOT, read_dotenv
from suunto_ingest.sync import DEFAULT_DATABASE_URL


app = FastAPI(title="Kore Training API", version="0.1.0", description="Local read-only views of Suunto training data")


class Status(BaseModel):
    workouts: int
    fit_files: int
    decoded_workouts: int


class WorkoutBrief(BaseModel):
    workout_key: str
    start_time: datetime | None
    sport: str | None
    duration_seconds: float | None
    distance_meters: float | None
    avg_heart_rate: float | None
    training_stress_score: float | None


class WorkoutPage(BaseModel):
    total: int
    limit: int
    offset: int
    items: list[WorkoutBrief]


class Lap(BaseModel):
    lap_index: int
    start_time: datetime | None
    duration_seconds: float | None
    distance_meters: float | None
    avg_heart_rate: float | None


class WorkoutDetail(WorkoutBrief):
    record_count: int | None
    laps: list[Lap]


class DaySummary(BaseModel):
    day: date
    sessions: int
    duration_seconds: float
    distance_meters: float
    tss_total: float
    tss_sessions: int


class TrainingContext(BaseModel):
    from_date: date
    through_date: date
    sessions: int
    duration_seconds: float
    distance_meters: float
    tss_total: float
    tss_sessions: int
    days: list[DaySummary]
    recent_workouts: list[WorkoutBrief]


def database_url() -> str:
    if value := os.environ.get("KORE_DATABASE_URL"):
        return value
    config = read_dotenv(ROOT / ".env")
    return config.get("KORE_DATABASE_URL", DEFAULT_DATABASE_URL)


def connect() -> psycopg.Connection:
    return psycopg.connect(database_url(), row_factory=dict_row, connect_timeout=10)


BRIEF_SELECT = """SELECT w.workout_key, s.start_time, s.sport,
    s.timer_seconds AS duration_seconds, s.distance_meters,
    s.avg_heart_rate, s.training_stress_score
    FROM suunto_workouts w
    LEFT JOIN suunto_fit_sessions s ON s.workout_key = w.workout_key AND s.session_index = 0"""


@app.get("/api/v1/status", response_model=Status)
def status():
    with connect() as db:
        row = db.execute("""SELECT count(*) AS workouts,
            count(*) FILTER (WHERE fit_status = 'ok') AS fit_files,
            count(*) FILTER (WHERE fit_parsed_sha256 = fit_sha256) AS decoded_workouts
            FROM suunto_workouts""").fetchone()
    return row


@app.get("/api/v1/workouts", response_model=WorkoutPage)
def workouts(limit: int = Query(20, ge=1, le=100), offset: int = Query(0, ge=0)):
    with connect() as db:
        total = db.execute("SELECT count(*) AS n FROM suunto_workouts").fetchone()["n"]
        rows = db.execute(BRIEF_SELECT + " ORDER BY w.start_time_ms DESC NULLS LAST, w.workout_key LIMIT %s OFFSET %s", (limit, offset)).fetchall()
    return {"total": total, "limit": limit, "offset": offset, "items": rows}


@app.get("/api/v1/workouts/{workout_key}", response_model=WorkoutDetail)
def workout_detail(workout_key: str):
    with connect() as db:
        row = db.execute(BRIEF_SELECT.replace("s.training_stress_score", "s.training_stress_score, w.fit_record_count AS record_count") + " WHERE w.workout_key = %s", (workout_key,)).fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail="Workout not found")
        laps = db.execute("""SELECT lap_index, start_time, timer_seconds AS duration_seconds,
            distance_meters, avg_heart_rate FROM suunto_fit_laps
            WHERE workout_key = %s ORDER BY lap_index""", (workout_key,)).fetchall()
    return {**row, "laps": laps}


@app.get("/api/v1/agent/context", response_model=TrainingContext)
def training_context(days: int = Query(28, ge=1, le=365)):
    through = datetime.now(timezone.utc)
    start = through - timedelta(days=days)
    with connect() as db:
        daily = db.execute("""SELECT (s.start_time AT TIME ZONE 'UTC')::date AS day,
            count(*)::int AS sessions,
            coalesce(sum(s.timer_seconds), 0)::float8 AS duration_seconds,
            coalesce(sum(s.distance_meters), 0)::float8 AS distance_meters,
            coalesce(sum(s.training_stress_score), 0)::float8 AS tss_total,
            count(s.training_stress_score)::int AS tss_sessions
            FROM suunto_fit_sessions s
            WHERE s.start_time >= %s AND s.start_time < %s
            GROUP BY 1 ORDER BY 1""", (start, through)).fetchall()
        recent = db.execute(BRIEF_SELECT + " WHERE s.start_time >= %s AND s.start_time < %s ORDER BY s.start_time DESC LIMIT 10", (start, through)).fetchall()
    return {
        "from_date": start.date(), "through_date": through.date(),
        "sessions": sum(row["sessions"] for row in daily),
        "duration_seconds": sum(row["duration_seconds"] for row in daily),
        "distance_meters": sum(row["distance_meters"] for row in daily),
        "tss_total": sum(row["tss_total"] for row in daily),
        "tss_sessions": sum(row["tss_sessions"] for row in daily),
        "days": daily, "recent_workouts": recent,
    }
