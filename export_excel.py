"""Export local Suunto workouts to a private Excel workbook."""

from __future__ import annotations

import argparse
from collections import defaultdict
from datetime import date, datetime, time, timedelta, timezone
import os
from pathlib import Path
import re
import secrets
import sys
import tempfile
from zoneinfo import ZoneInfo

from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
import psycopg
from psycopg.rows import dict_row

from suunto_ingest.decode import PARSER_VERSION
from suunto_ingest.probe import ROOT, read_dotenv
from suunto_ingest.sync import DEFAULT_DATABASE_URL


MADRID = ZoneInfo("Europe/Madrid")
DATA = ROOT / "data"
EXPORTS = DATA / "exports"
HEADER_FILL = PatternFill("solid", fgColor="1F4E78")


def select_dates(week: str | None, from_date: str | None, to_date: str | None,
                 now: datetime | None = None) -> tuple[date, date]:
    if week and (from_date or to_date):
        raise ValueError("--week cannot be combined with --from or --to")
    if week:
        match = re.fullmatch(r"(\d{4})-W(\d{2})", week)
        if not match:
            raise ValueError("--week must use YYYY-Www, for example 2026-W46")
        try:
            first = date.fromisocalendar(int(match[1]), int(match[2]), 1)
        except ValueError as exc:
            raise ValueError("Invalid ISO week") from exc
        return first, first + timedelta(days=6)
    if from_date or to_date:
        if not from_date or not to_date:
            raise ValueError("--from and --to must be used together")
        try:
            first, last = date.fromisoformat(from_date), date.fromisoformat(to_date)
        except ValueError as exc:
            raise ValueError("Dates must use YYYY-MM-DD") from exc
        if first > last:
            raise ValueError("--from must be on or before --to")
        return first, last
    today = (now or datetime.now(timezone.utc)).astimezone(MADRID).date()
    first = today - timedelta(days=today.weekday())
    return first, first + timedelta(days=6)


def database_url() -> str:
    return os.environ.get("KORE_DATABASE_URL") or read_dotenv(ROOT / ".env").get(
        "KORE_DATABASE_URL", DEFAULT_DATABASE_URL
    )


def millisecond_bounds(first: date, last: date) -> tuple[int, int]:
    start = datetime.combine(first, time.min, MADRID)
    end = datetime.combine(last + timedelta(days=1), time.min, MADRID)
    return int(start.timestamp() * 1000), int(end.timestamp() * 1000)


def query_data(first: date, last: date) -> tuple[list[dict], list[dict]]:
    start_ms, end_ms = millisecond_bounds(first, last)
    with psycopg.connect(database_url(), row_factory=dict_row, connect_timeout=10,
                         options="-c default_transaction_read_only=on") as db:
        workouts = db.execute("""SELECT w.workout_key, w.start_time_ms, w.fit_status,
            s.sport, s.timer_seconds, s.distance_meters, s.avg_heart_rate,
            s.max_heart_rate, s.avg_power, s.training_stress_score,
            CASE WHEN s.workout_key IS NOT NULL THEN w.fit_record_count END AS record_count
            FROM suunto_workouts w
            LEFT JOIN suunto_fit_sessions s ON s.workout_key = w.workout_key
                AND s.session_index = 0 AND w.fit_status = 'ok'
                AND w.fit_parsed_sha256 = w.fit_sha256
                AND w.fit_parser_version = %s
            WHERE w.start_time_ms >= %s AND w.start_time_ms < %s
            ORDER BY w.start_time_ms, w.workout_key""",
            (PARSER_VERSION, start_ms, end_ms)).fetchall()
        laps = db.execute("""SELECT l.workout_key, l.lap_index, l.start_time,
            l.timer_seconds, l.distance_meters, l.avg_heart_rate, l.max_heart_rate,
            l.avg_power FROM suunto_fit_laps l
            JOIN suunto_workouts w ON w.workout_key = l.workout_key
            WHERE w.start_time_ms >= %s AND w.start_time_ms < %s
                AND w.fit_status = 'ok' AND w.fit_parsed_sha256 = w.fit_sha256
                AND w.fit_parser_version = %s
            ORDER BY w.start_time_ms, l.workout_key, l.lap_index""",
            (start_ms, end_ms, PARSER_VERSION)).fetchall()
    return workouts, laps


def local_time(timestamp: datetime | None) -> datetime | None:
    return timestamp.astimezone(MADRID).replace(tzinfo=None) if timestamp else None


def rounded(value: float | None, scale: float = 1.0) -> float | None:
    return round(value / scale, 2) if value is not None else None


def add_header(sheet, headings: tuple[str, ...], widths: tuple[int, ...]) -> None:
    sheet.append(headings)
    for cell in sheet[1]:
        cell.font = Font(bold=True, color="FFFFFF")
        cell.fill = HEADER_FILL
        cell.alignment = Alignment(vertical="center")
    for index, width in enumerate(widths, 1):
        sheet.column_dimensions[sheet.cell(1, index).column_letter].width = width
    sheet.freeze_panes = "A2"


def safe_text(cell) -> None:
    if isinstance(cell.value, str):
        cell.data_type = "s"


def build_workbook(first: date, last: date, workouts: list[dict], laps: list[dict]) -> Workbook:
    book = Workbook()
    summary = book.active
    summary.title = "Resumen"
    summary.column_dimensions["A"].width = 34
    summary.column_dimensions["B"].width = 24
    valid = [row for row in workouts if row["record_count"] is not None]
    totals = (
        ("Desde", first), ("Hasta", last), ("Zona horaria", "Europe/Madrid"),
        ("Entrenamientos", len(workouts)), ("FIT decodificados", len(valid)),
        ("FIT pendientes o no disponibles", len(workouts) - len(valid)),
        ("Duración total (min)", rounded(sum(row["timer_seconds"] or 0 for row in valid), 60)),
        ("Distancia total (km)", rounded(sum(row["distance_meters"] or 0 for row in valid), 1000)),
        ("TSS total", rounded(sum(row["training_stress_score"] or 0 for row in valid))),
        ("Entrenamientos con TSS", sum(row["training_stress_score"] is not None for row in valid)),
    )
    for key, value in totals:
        summary.append((key, value))
    summary["B1"].number_format = "dd/mm/yyyy"
    summary["B2"].number_format = "dd/mm/yyyy"
    for cell in summary["A"]:
        cell.font = Font(bold=True)

    sessions = book.create_sheet("Entrenamientos")
    add_header(sessions, (
        "Fecha", "Inicio local", "Deporte", "Duración (min)", "Distancia (km)",
        "FC media", "FC máxima", "Potencia media (W)", "TSS", "Estado FIT",
        "Muestras", "Clave Suunto",
    ), (16, 22, 18, 20, 19, 14, 14, 22, 12, 18, 13, 30))
    daily = defaultdict(lambda: {"sessions": 0, "duration": 0.0, "distance": 0.0,
                                 "tss": 0.0, "tss_sessions": 0, "fit_missing": 0})
    for row in workouts:
        started = datetime.fromtimestamp(row["start_time_ms"] / 1000, timezone.utc).astimezone(MADRID)
        day = started.date()
        decoded = row["record_count"] is not None
        fit_state = "Decodificado" if decoded else {
            None: "Pendiente de descarga",
            "missing": "No disponible",
            "ok": "Pendiente de procesar",
        }.get(row["fit_status"], "Estado desconocido")
        sessions.append((
            day, started.replace(tzinfo=None), row["sport"],
            rounded(row["timer_seconds"], 60), rounded(row["distance_meters"], 1000),
            rounded(row["avg_heart_rate"]), rounded(row["max_heart_rate"]),
            rounded(row["avg_power"]), rounded(row["training_stress_score"]),
            fit_state, row["record_count"], row["workout_key"],
        ))
        for cell in sessions[sessions.max_row]:
            safe_text(cell)
        sessions.cell(sessions.max_row, 1).number_format = "dd/mm/yyyy"
        sessions.cell(sessions.max_row, 2).number_format = "dd/mm/yyyy hh:mm"
        day_total = daily[day]
        day_total["sessions"] += 1
        day_total["duration"] += row["timer_seconds"] or 0
        day_total["distance"] += row["distance_meters"] or 0
        day_total["tss"] += row["training_stress_score"] or 0
        day_total["tss_sessions"] += row["training_stress_score"] is not None
        day_total["fit_missing"] += not decoded
    sessions.auto_filter.ref = sessions.dimensions

    days = book.create_sheet("Resumen diario")
    add_header(days, ("Fecha", "Entrenamientos", "Duración (min)", "Distancia (km)",
                      "TSS", "Con TSS", "Sin FIT decodificado"), (16, 20, 20, 20, 14, 15, 25))
    for index in range((last - first).days + 1):
        day = first + timedelta(days=index)
        values = daily[day]
        days.append((day, values["sessions"], rounded(values["duration"], 60),
                     rounded(values["distance"], 1000), rounded(values["tss"]),
                     values["tss_sessions"], values["fit_missing"]))
        days.cell(days.max_row, 1).number_format = "dd/mm/yyyy"
    days.auto_filter.ref = days.dimensions

    lap_sheet = book.create_sheet("Vueltas")
    add_header(lap_sheet, ("Clave Suunto", "Vuelta", "Inicio local", "Duración (min)",
                           "Distancia (km)", "FC media", "FC máxima", "Potencia media (W)"),
               (30, 10, 22, 20, 20, 14, 14, 22))
    for row in laps:
        lap_sheet.append((row["workout_key"], row["lap_index"] + 1, local_time(row["start_time"]),
                          rounded(row["timer_seconds"], 60), rounded(row["distance_meters"], 1000),
                          rounded(row["avg_heart_rate"]), rounded(row["max_heart_rate"]),
                          rounded(row["avg_power"])))
        safe_text(lap_sheet.cell(lap_sheet.max_row, 1))
        lap_sheet.cell(lap_sheet.max_row, 3).number_format = "dd/mm/yyyy hh:mm"
    lap_sheet.auto_filter.ref = lap_sheet.dimensions
    return book


def output_path(first: date, last: date, requested: Path | None) -> Path:
    if requested:
        path = requested.expanduser().resolve()
    else:
        suffix = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S") + "-" + secrets.token_hex(2)
        path = EXPORTS / f"suunto-{first}_{last}-{suffix}.xlsx"
    if path.suffix.lower() != ".xlsx":
        raise ValueError("Output path must end in .xlsx")
    if ROOT in path.parents and DATA not in path.parents:
        raise ValueError("Output inside the repository must be under data/")
    if path.exists():
        raise ValueError("Output already exists; choose a different filename")
    return path


def save_private(book: Workbook, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    if path.parent == EXPORTS:
        os.chmod(path.parent, 0o700)
    fd, temporary = tempfile.mkstemp(prefix=".suunto-", suffix=".xlsx", dir=path.parent)
    os.close(fd)
    try:
        book.save(temporary)
        os.chmod(temporary, 0o600)
        if path.exists():
            raise ValueError("Output already exists; choose a different filename")
        os.replace(temporary, path)
    finally:
        Path(temporary).unlink(missing_ok=True)


def main() -> int:
    parser = argparse.ArgumentParser(description="Export Suunto training data to Excel")
    parser.add_argument("--week", help="ISO week, for example 2026-W46; defaults to current week")
    parser.add_argument("--from", dest="from_date", help="First day, YYYY-MM-DD")
    parser.add_argument("--to", dest="to_date", help="Last day, YYYY-MM-DD (inclusive)")
    parser.add_argument("--output", type=Path, help="Private .xlsx destination")
    args = parser.parse_args()
    os.umask(0o077)
    try:
        first, last = select_dates(args.week, args.from_date, args.to_date)
        path = output_path(first, last, args.output)
        workouts, laps = query_data(first, last)
        save_private(build_workbook(first, last, workouts, laps), path)
    except (ValueError, OSError, psycopg.Error) as exc:
        print(f"Excel export failed: {exc if isinstance(exc, ValueError) else type(exc).__name__}",
              file=sys.stderr)
        return 1
    print(f"Excel created: {path} ({len(workouts)} workouts, {len(laps)} laps)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
