"""Local, read-only training API. Run on 127.0.0.1 only."""

from datetime import date, datetime, time, timedelta, timezone
import os
from zoneinfo import ZoneInfo

from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel
import psycopg
from psycopg.rows import dict_row

from suunto_ingest.probe import ROOT, read_dotenv
from suunto_ingest.sync import DEFAULT_DATABASE_URL
from suunto_ingest.decode import PARSER_VERSION


app = FastAPI(title="Kore Training API", version="0.1.0", description="Local read-only views of Suunto training data")
LOCAL_TIMEZONE = ZoneInfo("Europe/Madrid")


class PipelineRun(BaseModel):
    started_at: datetime
    finished_at: datetime | None
    status: str
    stage: str
    summaries: int
    fit_downloaded: int
    fit_missing: int
    fit_decoded: int
    error_type: str | None


class Status(BaseModel):
    workouts: int
    fit_files: int
    decoded_workouts: int
    latest_run: PipelineRun | None = None
    latest_success_at: datetime | None = None


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
    partial: bool
    sessions: int
    duration_seconds: float
    distance_meters: float
    tss_total: float
    tss_sessions: int


class WeekSummary(BaseModel):
    week_start: date
    complete: bool
    sessions: int
    duration_seconds: float
    distance_meters: float
    tss_total: float
    tss_sessions: int


class SportSummary(BaseModel):
    sport: str | None
    sessions: int
    duration_seconds: float
    distance_meters: float
    tss_total: float
    tss_sessions: int
    sessions_with_heart_rate: int
    sessions_with_power: int


class DataAvailability(BaseModel):
    as_of: datetime
    last_successful_ingestion_at: datetime | None = None
    latest_ingestion_status: str | None = None
    latest_summary_at: datetime | None
    latest_decoded_at: datetime | None
    pending_fit: int
    unavailable_fit: int
    pending_decode: int


class TrainingContext(BaseModel):
    from_date: date
    through_date: date
    timezone: str
    sessions: int
    duration_seconds: float
    distance_meters: float
    tss_total: float
    tss_sessions: int
    tss_coverage: float
    days: list[DaySummary]
    weeks: list[WeekSummary]
    sports: list[SportSummary]
    availability: DataAvailability
    recent_workouts: list[WorkoutBrief]


def calendar_bounds(days: int, now: datetime | None = None) -> tuple[date, date, datetime, datetime]:
    today = (now or datetime.now(timezone.utc)).astimezone(LOCAL_TIMEZONE).date()
    first = today - timedelta(days=days - 1)
    start = datetime.combine(first, time.min, LOCAL_TIMEZONE)
    end = datetime.combine(today + timedelta(days=1), time.min, LOCAL_TIMEZONE)
    return first, today, start, end


def database_url() -> str:
    if value := os.environ.get("KORE_DATABASE_URL"):
        return value
    config = read_dotenv(ROOT / ".env")
    return config.get("KORE_DATABASE_URL", DEFAULT_DATABASE_URL)


def connect() -> psycopg.Connection:
    return psycopg.connect(database_url(), row_factory=dict_row, connect_timeout=10)


BRIEF_SELECT = f"""SELECT w.workout_key, s.start_time, s.sport,
    s.timer_seconds AS duration_seconds, s.distance_meters,
    s.avg_heart_rate, s.training_stress_score
    FROM suunto_workouts w
    LEFT JOIN suunto_fit_sessions s ON s.workout_key = w.workout_key AND s.session_index = 0
        AND w.fit_status = 'ok' AND w.fit_parsed_sha256 = w.fit_sha256
        AND w.fit_parser_version = {PARSER_VERSION}"""


@app.get("/api/v1/status", response_model=Status)
def status():
    with connect() as db:
        row = db.execute("""SELECT count(*) AS workouts,
            count(*) FILTER (WHERE fit_status = 'ok') AS fit_files,
            count(*) FILTER (WHERE fit_status = 'ok' AND fit_parsed_sha256 = fit_sha256
                AND fit_parser_version = %s) AS decoded_workouts
            FROM suunto_workouts""", (PARSER_VERSION,)).fetchone()
        if db.execute("SELECT to_regclass('suunto_pipeline_runs') AS name").fetchone()["name"]:
            row["latest_run"] = db.execute("""SELECT started_at, finished_at, status, stage,
                summaries, fit_downloaded, fit_missing, fit_decoded, error_type
                FROM suunto_pipeline_runs ORDER BY started_at DESC LIMIT 1""").fetchone()
            row["latest_success_at"] = db.execute("""SELECT max(finished_at) AS at
                FROM suunto_pipeline_runs WHERE status = 'succeeded'""").fetchone()["at"]
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
        row = db.execute(BRIEF_SELECT.replace(
            "s.training_stress_score",
            "s.training_stress_score, CASE WHEN s.workout_key IS NOT NULL THEN w.fit_record_count ELSE NULL END AS record_count",
        ) + " WHERE w.workout_key = %s", (workout_key,)).fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail="Workout not found")
        laps = db.execute("""SELECT l.lap_index, l.start_time, l.timer_seconds AS duration_seconds,
            l.distance_meters, l.avg_heart_rate FROM suunto_fit_laps l
            JOIN suunto_workouts w ON w.workout_key = l.workout_key
            WHERE l.workout_key = %s AND w.fit_status = 'ok'
                AND w.fit_parsed_sha256 = w.fit_sha256 AND w.fit_parser_version = %s
            ORDER BY l.lap_index""", (workout_key, PARSER_VERSION)).fetchall()
    return {**row, "laps": laps}


@app.get("/api/v1/agent/context", response_model=TrainingContext)
def training_context(days: int = Query(28, ge=1, le=365)):
    as_of = datetime.now(timezone.utc)
    first, today, start, end = calendar_bounds(days, as_of)
    with connect() as db:
        daily = db.execute("""SELECT (s.start_time AT TIME ZONE 'Europe/Madrid')::date AS day,
            count(*)::int AS sessions,
            coalesce(sum(s.timer_seconds), 0)::float8 AS duration_seconds,
            coalesce(sum(s.distance_meters), 0)::float8 AS distance_meters,
            coalesce(sum(s.training_stress_score), 0)::float8 AS tss_total,
            count(s.training_stress_score)::int AS tss_sessions
            FROM suunto_fit_sessions s
            JOIN suunto_workouts w ON w.workout_key = s.workout_key
            WHERE s.start_time >= %s AND s.start_time < %s
                AND w.fit_status = 'ok' AND w.fit_parsed_sha256 = w.fit_sha256
                AND w.fit_parser_version = %s
            GROUP BY 1 ORDER BY 1""", (start, end, PARSER_VERSION)).fetchall()
        sports = db.execute("""SELECT s.sport, count(*)::int AS sessions,
            coalesce(sum(s.timer_seconds), 0)::float8 AS duration_seconds,
            coalesce(sum(s.distance_meters), 0)::float8 AS distance_meters,
            coalesce(sum(s.training_stress_score), 0)::float8 AS tss_total,
            count(s.training_stress_score)::int AS tss_sessions,
            count(s.avg_heart_rate)::int AS sessions_with_heart_rate,
            count(s.avg_power)::int AS sessions_with_power
            FROM suunto_fit_sessions s
            JOIN suunto_workouts w ON w.workout_key = s.workout_key
            WHERE s.start_time >= %s AND s.start_time < %s
                AND w.fit_status = 'ok' AND w.fit_parsed_sha256 = w.fit_sha256
                AND w.fit_parser_version = %s
            GROUP BY s.sport ORDER BY sessions DESC, s.sport""", (start, end, PARSER_VERSION)).fetchall()
        availability = db.execute("""SELECT
            to_timestamp(max(start_time_ms) / 1000.0) AS latest_summary_at,
            count(*) FILTER (WHERE fit_status IS NULL)::int AS pending_fit,
            count(*) FILTER (WHERE fit_status = 'missing')::int AS unavailable_fit,
            count(*) FILTER (WHERE fit_status = 'ok' AND
                (fit_parsed_sha256 IS DISTINCT FROM fit_sha256 OR
                 fit_parser_version IS DISTINCT FROM %s))::int AS pending_decode
            FROM suunto_workouts""", (PARSER_VERSION,)).fetchone()
        availability["latest_decoded_at"] = db.execute("""SELECT max(s.start_time) AS at
            FROM suunto_fit_sessions s JOIN suunto_workouts w ON w.workout_key = s.workout_key
            WHERE w.fit_status = 'ok' AND w.fit_parsed_sha256 = w.fit_sha256
                AND w.fit_parser_version = %s""", (PARSER_VERSION,)).fetchone()["at"]
        if db.execute("SELECT to_regclass('suunto_pipeline_runs') AS name").fetchone()["name"]:
            latest_run = db.execute("""SELECT status
                FROM suunto_pipeline_runs ORDER BY started_at DESC LIMIT 1""").fetchone()
            availability["latest_ingestion_status"] = latest_run["status"] if latest_run else None
            availability["last_successful_ingestion_at"] = db.execute("""SELECT max(finished_at) AS at
                FROM suunto_pipeline_runs WHERE status = 'succeeded'""").fetchone()["at"]
        availability["as_of"] = as_of
        recent = db.execute(BRIEF_SELECT + " WHERE s.start_time >= %s AND s.start_time < %s ORDER BY s.start_time DESC LIMIT 10", (start, end)).fetchall()
    by_day = {row["day"]: row for row in daily}
    all_days = []
    for index in range(days):
        day = first + timedelta(days=index)
        summary = by_day.get(day, {
            "day": day, "sessions": 0, "duration_seconds": 0.0,
            "distance_meters": 0.0, "tss_total": 0.0, "tss_sessions": 0,
        })
        all_days.append({**summary, "partial": day == today})
    weekly = {}
    for row in all_days:
        monday = row["day"] - timedelta(days=row["day"].weekday())
        totals = weekly.setdefault(monday, {
            "week_start": monday,
            "complete": monday >= first and monday + timedelta(days=6) < today,
            "sessions": 0, "duration_seconds": 0.0,
            "distance_meters": 0.0, "tss_total": 0.0, "tss_sessions": 0,
        })
        for field in ("sessions", "duration_seconds", "distance_meters", "tss_total", "tss_sessions"):
            totals[field] += row[field]
    session_count = sum(row["sessions"] for row in all_days)
    tss_count = sum(row["tss_sessions"] for row in all_days)
    return {
        "from_date": first, "through_date": today, "timezone": "Europe/Madrid",
        "sessions": session_count,
        "duration_seconds": sum(row["duration_seconds"] for row in all_days),
        "distance_meters": sum(row["distance_meters"] for row in all_days),
        "tss_total": sum(row["tss_total"] for row in all_days),
        "tss_sessions": tss_count,
        "tss_coverage": tss_count / session_count if session_count else 0.0,
        "days": all_days, "weeks": list(weekly.values()), "sports": sports,
        "availability": availability, "recent_workouts": recent,
    }
