"""Conservative entry-level suggestions from recent, decoded Suunto evidence."""

from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass
from datetime import date, datetime, time, timedelta, timezone
import os
from typing import Literal
from zoneinfo import ZoneInfo

import psycopg
from psycopg.rows import dict_row

from suunto_ingest.decode import PARSER_VERSION
from suunto_ingest.probe import ROOT, read_dotenv
from suunto_ingest.sync import DEFAULT_DATABASE_URL


MADRID = ZoneInfo("Europe/Madrid")


@dataclass(frozen=True)
class ObservedSession:
    workout_key: str
    started_at: datetime
    timer_seconds: float | None
    distance_meters: float | None


@dataclass(frozen=True)
class ObservedLap:
    workout_key: str
    lap_index: int
    timer_seconds: float | None
    distance_meters: float | None


@dataclass(frozen=True)
class LevelSuggestion:
    role: Literal["speed", "sustained"]
    suggested_level: int
    volume_supported_ceiling: int
    detected_structured_level: int | None
    detected_sessions: int
    confidence: Literal["low", "medium"]
    evidence: tuple[str, ...]
    limitations: tuple[str, ...]


@dataclass(frozen=True)
class TrainingAssessment:
    generated_at: datetime
    from_date: date
    through_date: date
    window_days: int
    stored_workouts: int
    decoded_running_sessions: int
    current_weekly_km: float | None
    recent_long_run_km: float | None
    window_long_run_km: float | None
    latest_run_date: date | None
    speed: LevelSuggestion
    sustained: LevelSuggestion


def _pace(seconds: float | None, meters: float | None) -> float | None:
    return seconds / meters if seconds and meters and meters > 0 else None


def _matches_work_lap(lap: ObservedLap, role: str, level: int) -> bool:
    if lap.timer_seconds is None or lap.distance_meters is None:
        return False
    if role == "speed":
        if level == 0:
            return 40 <= lap.timer_seconds <= 100
        if level == 1:
            return 100 < lap.timer_seconds <= 260
        if level == 2:
            return 800 <= lap.distance_meters <= 1200
        return 1400 <= lap.distance_meters <= 1800
    ranges = ((300, 500), (480, 750), (720, 1250))
    return level < 3 and ranges[level][0] <= lap.timer_seconds <= ranges[level][1]


def detect_structured_level(session: ObservedSession, laps: list[ObservedLap],
                            role: Literal["speed", "sustained"]) -> int | None:
    """Require repeated faster-work/slower-recovery pairs; ordinary auto laps do not qualify."""
    session_pace = _pace(session.timer_seconds, session.distance_meters)
    if session_pace is None:
        return None
    highest = None
    max_level = 3 if role == "speed" else 2
    minimum_repetitions = (4, 4, 3, 3) if role == "speed" else (2, 2, 2)
    ordered = sorted(laps, key=lambda lap: lap.lap_index)
    for level in range(max_level + 1):
        matches = []
        for work, recovery in zip(ordered, ordered[1:]):
            work_pace = _pace(work.timer_seconds, work.distance_meters)
            recovery_pace = _pace(recovery.timer_seconds, recovery.distance_meters)
            if not _matches_work_lap(work, role, level) or work_pace is None or recovery_pace is None:
                continue
            if recovery.timer_seconds is None or not 30 <= recovery.timer_seconds <= work.timer_seconds:
                continue
            if work_pace >= recovery_pace * 0.92 or work_pace >= session_pace * 0.98:
                continue
            matches.append(work)
        if len(matches) >= minimum_repetitions[level]:
            highest = level
    return highest


def _volume_ceiling(sessions: list[ObservedSession], role: str) -> tuple[int, list[int]]:
    if role == "speed":
        thresholds = (6_000, 8_000, 10_000, 12_000)
        values = [session.distance_meters or 0 for session in sessions]
    else:
        thresholds = (35 * 60, 45 * 60, 60 * 60, 70 * 60)
        values = [session.timer_seconds or 0 for session in sessions]
    counts = [sum(value >= threshold for value in values) for threshold in thresholds]
    supported = [level for level, count in enumerate(counts) if count >= 3]
    return (max(supported) if supported else 0), counts


def suggest_level(sessions: list[ObservedSession], laps: list[ObservedLap],
                  role: Literal["speed", "sustained"]) -> LevelSuggestion:
    by_workout: dict[str, list[ObservedLap]] = defaultdict(list)
    for lap in laps:
        by_workout[lap.workout_key].append(lap)
    detections = [level for session in sessions
                  if (level := detect_structured_level(session, by_workout[session.workout_key], role)) is not None]
    ceiling, counts = _volume_ceiling(sessions, role)
    detected = max(detections, default=None)
    # One detected session is evidence of exposure, not enough to start at its highest dose.
    if detected is None:
        suggested = 0
    elif len(detections) == 1:
        suggested = min(ceiling, max(0, detected - 1))
    else:
        suggested = min(ceiling, detected)
    unit = "km" if role == "speed" else "min"
    thresholds = (6, 8, 10, 12) if role == "speed" else (35, 45, 60, 70)
    evidence = (
        f"{len(sessions)} carreras decodificadas en la ventana.",
        "Sesiones que alcanzan " + ", ".join(f"{threshold} {unit}: {count}"
                                               for threshold, count in zip(thresholds, counts)) + ".",
        (f"Patrón estructurado conservador detectado en {len(detections)} sesiones; "
         f"nivel máximo observado {detected}." if detected is not None
         else "No se detectó un patrón inequívoco de trabajo/recuperación en las vueltas FIT."),
    )
    limitations = (
        "Las vueltas automáticas pueden ocultar o deformar una sesión estructurada.",
        "Suunto no aporta aquí RPE, objetivo de la sesión ni tolerancia posterior.",
        "La sugerencia es un punto de entrada editable, no una medida de capacidad fisiológica.",
    )
    return LevelSuggestion(role, suggested, ceiling, detected, len(detections),
                           "medium" if len(detections) >= 2 else "low", evidence, limitations)


def assess_history(sessions: list[ObservedSession], laps: list[ObservedLap], *,
                   generated_at: datetime, from_date: date, through_date: date,
                   window_days: int, stored_workouts: int) -> TrainingAssessment:
    recent_start = through_date - timedelta(days=27)
    recent = [session for session in sessions if session.started_at.astimezone(MADRID).date() >= recent_start]
    weekly = round(sum(session.distance_meters or 0 for session in recent) / 1000 / 4, 1) if recent else None
    recent_long = round(max((session.distance_meters or 0 for session in recent), default=0) / 1000, 1) if recent else None
    window_long = round(max((session.distance_meters or 0 for session in sessions), default=0) / 1000, 1) if sessions else None
    latest = max((session.started_at.astimezone(MADRID).date() for session in sessions), default=None)
    return TrainingAssessment(generated_at, from_date, through_date, window_days,
                              stored_workouts, len(sessions), weekly, recent_long,
                              window_long, latest, suggest_level(sessions, laps, "speed"),
                              suggest_level(sessions, laps, "sustained"))


def _database_url() -> str:
    return os.environ.get("KORE_DATABASE_URL") or read_dotenv(ROOT / ".env").get(
        "KORE_DATABASE_URL", DEFAULT_DATABASE_URL
    )


def assess_local_history(window_days: int = 84, now: datetime | None = None) -> TrainingAssessment:
    if not 28 <= window_days <= 365:
        raise ValueError("window_days must be between 28 and 365")
    current = (now or datetime.now(timezone.utc)).astimezone(MADRID)
    through = current.date()
    first = through - timedelta(days=window_days - 1)
    start = datetime.combine(first, time.min, MADRID)
    end = datetime.combine(through + timedelta(days=1), time.min, MADRID)
    with psycopg.connect(_database_url(), row_factory=dict_row, connect_timeout=10,
                         options="-c default_transaction_read_only=on") as db:
        db.execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY")
        stored = db.execute("""SELECT count(*) FROM suunto_workouts
            WHERE start_time_ms >= %s AND start_time_ms < %s""",
            (int(start.timestamp() * 1000), int(end.timestamp() * 1000))).fetchone()["count"]
        session_rows = db.execute("""SELECT s.workout_key, s.start_time AS started_at, s.timer_seconds,
            s.distance_meters FROM suunto_fit_sessions s
            JOIN suunto_workouts w USING (workout_key)
            WHERE s.session_index = 0 AND s.sport = 'running'
              AND s.start_time >= %s AND s.start_time < %s
              AND w.fit_status = 'ok' AND w.fit_parsed_sha256 = w.fit_sha256
              AND w.fit_parser_version = %s
            ORDER BY s.start_time, s.workout_key""", (start, end, PARSER_VERSION)).fetchall()
        lap_rows = db.execute("""SELECT l.workout_key, l.lap_index, l.timer_seconds,
            l.distance_meters FROM suunto_fit_laps l
            JOIN suunto_workouts w USING (workout_key)
            JOIN suunto_fit_sessions s ON s.workout_key = w.workout_key AND s.session_index = 0
            WHERE s.sport = 'running' AND s.start_time >= %s AND s.start_time < %s
              AND w.fit_status = 'ok' AND w.fit_parsed_sha256 = w.fit_sha256
              AND w.fit_parser_version = %s
            ORDER BY l.workout_key, l.lap_index""", (start, end, PARSER_VERSION)).fetchall()
    sessions = [ObservedSession(**row) for row in session_rows]
    laps = [ObservedLap(**row) for row in lap_rows]
    return assess_history(sessions, laps, generated_at=current, from_date=first,
                          through_date=through, window_days=window_days,
                          stored_workouts=stored)
