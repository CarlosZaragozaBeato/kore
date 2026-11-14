"""Match Suunto workouts to one daily row by exact block tag and local date."""

from __future__ import annotations

from collections import defaultdict
from datetime import date, datetime, time, timedelta, timezone
import os
from pathlib import Path
from zoneinfo import ZoneInfo

from openpyxl import Workbook, load_workbook

from plan_workbook import ACTIVE, DAILY_HEADERS, SUUNTO_HEADERS, WEEK_HEADERS, save_with_backup


MADRID = ZoneInfo("Europe/Madrid")


def as_date(value) -> date:
    if isinstance(value, datetime):
        return value.date()
    if isinstance(value, date):
        return value
    raise ValueError("Invalid plan date")


def validate(book: Workbook) -> tuple[date, date, str]:
    if not {"Plan diario", "Plan semanal", "Configuración", "Suunto"} <= set(book.sheetnames):
        raise ValueError("Missing daily plan sheets")
    config = book["Configuración"]
    if config["A1"].value != "KORE_PLAN_DAILY_V2" or config["B8"].value not in ("Activo", "Finalizado"):
        raise ValueError("Only a confirmed V2 active block can be updated")
    if tuple(c.value for c in book["Plan diario"][1]) != DAILY_HEADERS:
        raise ValueError("Daily plan columns changed")
    if tuple(c.value for c in book["Plan semanal"][1]) != WEEK_HEADERS:
        raise ValueError("Weekly plan columns changed")
    start, end = as_date(config["B4"].value), as_date(config["B5"].value)
    tag = config["B7"].value
    if start.weekday() != 0 or end != start + timedelta(weeks=config["B6"].value, days=-1):
        raise ValueError("Active block date range is inconsistent")
    if tag != f"kore_{config['B2'].value}_{start:%Y%m%d}" or config["B9"].value != tag:
        raise ValueError("Active block tag or identifier is inconsistent")
    daily = book["Plan diario"]
    if daily.max_row - 1 != config["B6"].value * 7:
        raise ValueError("Daily plan must have one row per date")
    for index, row in enumerate(daily.iter_rows(min_row=2, values_only=True)):
        expected = start + timedelta(days=index)
        if as_date(row[2]) != expected or row[0] != index // 7 + 1:
            raise ValueError("Daily plan dates or weeks changed")
    return start, end, tag


def query_workouts(start: date, end: date) -> list[dict]:
    """Use the same freshness conditions as codex/excel-export export_excel.py."""
    import psycopg
    from psycopg.rows import dict_row
    from suunto_ingest.decode import PARSER_VERSION
    from suunto_ingest.probe import ROOT, read_dotenv
    from suunto_ingest.sync import DEFAULT_DATABASE_URL

    start_ms = int(datetime.combine(start, time.min, MADRID).timestamp() * 1000)
    end_ms = int(datetime.combine(end + timedelta(days=1), time.min, MADRID).timestamp() * 1000)
    url = os.environ.get("KORE_DATABASE_URL") or read_dotenv(ROOT / ".env").get(
        "KORE_DATABASE_URL", DEFAULT_DATABASE_URL
    )
    with psycopg.connect(url, row_factory=dict_row, connect_timeout=10,
                         options="-c default_transaction_read_only=on") as db:
        db.execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY")
        return db.execute("""SELECT w.workout_key, w.start_time_ms, w.fit_status,
            w.raw_payload->'userTags' AS user_tags,
            s.sport, s.timer_seconds, s.distance_meters, s.avg_heart_rate,
            s.training_stress_score,
            CASE WHEN s.workout_key IS NOT NULL THEN w.fit_record_count END AS record_count
            FROM suunto_workouts w
            LEFT JOIN suunto_fit_sessions s ON s.workout_key = w.workout_key
                AND s.session_index = 0 AND w.fit_status = 'ok'
                AND w.fit_parsed_sha256 = w.fit_sha256
                AND w.fit_parser_version = %s
            WHERE w.start_time_ms >= %s AND w.start_time_ms < %s
            ORDER BY w.start_time_ms, w.workout_key""",
            (PARSER_VERSION, start_ms, end_ms)).fetchall()


def _pace(seconds: float | None, meters: float | None) -> str | None:
    if seconds is None or meters is None or meters <= 0:
        return None
    sec = round(seconds * 1000 / meters)
    return f"{sec // 60}:{sec % 60:02d}"


def _safe(cell):
    if isinstance(cell.value, str):
        cell.data_type = "s"  # Workout text must not become an Excel formula.


def update_book(book: Workbook, workouts: list[dict], *, now: datetime | None = None) -> Workbook:
    start, end, tag = validate(book)
    today = (now or datetime.now(timezone.utc)).astimezone(MADRID).date()
    # A repeated DB row or a rerun cannot double count an activity.
    unique = {row["workout_key"]: row for row in workouts}
    by_date = defaultdict(list)
    for workout in unique.values():
        tags = workout.get("user_tags") or []
        if not isinstance(tags, list) or tag not in tags:
            continue
        local = datetime.fromtimestamp(workout["start_time_ms"] / 1000, timezone.utc).astimezone(MADRID)
        if start <= local.date() <= end:
            by_date[local.date()].append((local, workout))
    for values in by_date.values():
        values.sort(key=lambda item: (item[0], item[1]["workout_key"]))

    suunto = book["Suunto"]
    if tuple(c.value for c in suunto[1]) != SUUNTO_HEADERS:
        raise ValueError("Suunto detail columns changed")
    if suunto.max_row > 1:
        suunto.delete_rows(2, suunto.max_row - 1)
    for day in sorted(by_date):
        for local, row in by_date[day]:
            decoded = row["record_count"] is not None
            values = (row["workout_key"], day, local.replace(tzinfo=None),
                      row["sport"] if decoded else None,
                      round(row["distance_meters"] / 1000, 2) if decoded and row["distance_meters"] is not None else None,
                      round(row["timer_seconds"] / 60, 2) if decoded and row["timer_seconds"] is not None else None,
                      _pace(row["timer_seconds"], row["distance_meters"]) if decoded else None,
                      round(row["avg_heart_rate"]) if decoded and row["avg_heart_rate"] is not None else None,
                      round(row["training_stress_score"], 1) if decoded and row["training_stress_score"] is not None else None,
                      "Decodificado" if decoded else "FIT pendiente",
                      ", ".join(row["user_tags"]))
            suunto.append(values)
            for cell in suunto[suunto.max_row]: _safe(cell)
            suunto.cell(suunto.max_row, 2).number_format = "dd/mm/yyyy"
            suunto.cell(suunto.max_row, 3).number_format = "dd/mm/yyyy hh:mm"
    suunto.auto_filter.ref = suunto.dimensions

    daily = book["Plan diario"]
    for sheet_row in range(2, daily.max_row + 1):
        day = as_date(daily.cell(sheet_row, 3).value)
        matched = by_date.get(day, [])
        valid_runs = [r for _, r in matched if r["record_count"] is not None and r["sport"] == "running"]
        pending = sum(r["record_count"] is None for _, r in matched)
        other = sorted({r["sport"] for _, r in matched if r["record_count"] is not None and r["sport"] != "running" and r["sport"]})
        distances = [r["distance_meters"] for r in valid_runs if r["distance_meters"] is not None]
        durations = [r["timer_seconds"] for r in valid_runs if r["timer_seconds"] is not None]
        complete_distance = bool(valid_runs) and pending == 0 and len(distances) == len(valid_runs)
        complete_duration = bool(valid_runs) and pending == 0 and len(durations) == len(valid_runs)
        km = round(sum(distances) / 1000, 2) if complete_distance else None
        mins = round(sum(durations) / 60, 2) if complete_duration else None
        pace = _pace(sum(durations), sum(distances)) if complete_distance and complete_duration else None
        hr_values = [(r["avg_heart_rate"], r["timer_seconds"]) for r in valid_runs
                     if r["avg_heart_rate"] is not None and r["timer_seconds"] is not None]
        hr = round(sum(h * seconds for h, seconds in hr_values) / sum(seconds for _, seconds in hr_values)) if pending == 0 and len(hr_values) == len(valid_runs) and hr_values and sum(seconds for _, seconds in hr_values) else None
        tss_values = [r["training_stress_score"] for r in valid_runs if r["training_stress_score"] is not None]
        tss = round(sum(tss_values), 1) if pending == 0 and valid_runs and len(tss_values) == len(valid_runs) else None
        rest = daily.cell(sheet_row, 7).value == 0
        confirmed = daily.cell(sheet_row, 20).value is True
        if pending:
            state = "FIT pendiente (parcial)" if valid_runs else "FIT pendiente"
        elif valid_runs and not (complete_distance and complete_duration):
            state = "Métricas incompletas"
        elif valid_runs:
            state = "Actividad etiquetada"
        elif other:
            state = "Otro deporte etiquetado"
        elif rest and confirmed and day <= today:
            state = "Descanso cumplido"
        elif day > today:
            state = "Pendiente"
        elif rest:
            state = "Descanso sin confirmar"
        else:
            state = "Sin actividad etiquetada"
        for column, value in {
            10: km, 11: mins, 12: pace, 13: hr, 14: tss, 15: state,
            16: ", ".join(r["workout_key"] for _, r in matched) or None,
            17: pending or None, 18: ", ".join(other) or None,
        }.items():
            daily.cell(sheet_row, column).value = value
            _safe(daily.cell(sheet_row, column))

    weekly = book["Plan semanal"]
    for row in range(2, weekly.max_row + 1):
        week = weekly.cell(row, 1).value
        entries = [r for r in daily.iter_rows(min_row=2, values_only=True) if r[0] == week]
        planned = sum(r[6] for r in entries)
        actuals = [r[9] for r in entries if isinstance(r[9], (int, float))]
        actual = round(sum(actuals), 2) if actuals else None
        first = as_date(weekly.cell(row, 2).value)
        count = sum(
            r["record_count"] is not None and r["sport"] == "running"
            for offset in range(7)
            for _, r in by_date.get(first + timedelta(days=offset), [])
        )
        pending = sum(r[16] or 0 for r in entries)
        states = {r[14] for r in entries}
        state = "FIT pendiente" if pending else "Con actividad" if count else "Sin actividad etiquetada" if as_date(weekly.cell(row, 2).value) <= today else "Pendiente"
        for column, value in ((5, planned), (6, actual), (8, count or None),
                              (9, state), (10, pending or None)):
            weekly.cell(row, column).value = value
    config = book["Configuración"]
    total_days = (end - start).days + 1
    if today < start:
        config["B10"] = f"Día 0 de {total_days} (antes del inicio)"
        config["B11"] = f"Semana 0 de {config['B6'].value}"
        config["B12"] = total_days
    elif today > end:
        config["B10"] = f"Bloque terminado ({total_days} días)"
        config["B11"] = f"Semana {config['B6'].value} de {config['B6'].value}"
        config["B12"] = 0
        config["B8"] = "Finalizado"
    else:
        elapsed = (today - start).days
        config["B10"] = f"Día {elapsed + 1} de {total_days}"
        config["B11"] = f"Semana {elapsed // 7 + 1} de {config['B6'].value}"
        config["B12"] = (end - today).days
    return book


def refresh_active(*, now: datetime | None = None) -> int:
    """Refresh the confirmed active workbook from the local database."""
    if not ACTIVE.exists():
        raise FileNotFoundError("No hay un bloque activo")
    book = load_workbook(ACTIVE)
    current = (now or datetime.now(timezone.utc)).astimezone(MADRID)
    try:
        start, end, _ = validate(book)
        workouts = query_workouts(start, end)
        update_book(book, workouts, now=current)
        finalized = book["Configuración"]["B8"].value == "Finalizado"
        save_with_backup(book, ACTIVE)
    finally:
        book.close()
    if finalized:
        from plan_lifecycle import complete_active
        complete_active(current, reason="Fin de calendario", allow_early=False)
    return len(workouts)


if __name__ == "__main__":
    count = refresh_active()
    print(f"Plan activo actualizado con {count} actividades consultadas")
