"""Create editable training-plan templates and insert local Suunto results."""

from __future__ import annotations

import argparse
from collections import defaultdict
from datetime import date, datetime, timedelta, timezone
import json
from pathlib import Path
import sys

from openpyxl import Workbook, load_workbook
from openpyxl.styles import Font
import psycopg

from export_excel import (
    DATA, MADRID, add_header, local_time, output_path, query_data,
    rounded, safe_text, save_private,
)


MODALITIES = ("5k", "10k", "21k", "42k")
CONFIG = DATA / "planning" / "config.json"
TEMPLATE_DIR = DATA / "plans" / "templates"
UPDATED_DIR = DATA / "plans" / "updated"
MARKER = "KORE_PLAN_V1"
PLAN_HEADERS = (
    "Semana", "Desde", "Hasta", "Bloque", "Objetivo de la semana",
    "Km planificados", "Sesiones planificadas", "Notas del agente",
    "Km reales carrera", "Sesiones carrera", "Km mínimo referencia",
    "Diferencia vs plan (km)", "Estado semana", "FIT pendientes",
    "Otros deportes (min)", "Tirada más larga (km)",
)


def plan_tag(modality: str, start: date) -> str:
    return f"kore_{modality}_{start:%Y%m%d}"


def read_targets(path: Path = CONFIG) -> dict[str, float]:
    try:
        values = json.loads(path.read_text(encoding="utf-8"))["minimum_weekly_km"]
    except (OSError, ValueError, KeyError, TypeError) as exc:
        raise ValueError("Invalid local planning config; see PLANES.md") from exc
    if set(values) != set(MODALITIES) or any(
        isinstance(values[key], bool) or not isinstance(values[key], (int, float)) or values[key] < 0
        for key in MODALITIES
    ):
        raise ValueError("Planning config must have nonnegative weekly km for 5k, 10k, 21k and 42k")
    return {key: float(values[key]) for key in MODALITIES}


def monday(value: str | None) -> date:
    if value:
        try:
            day = date.fromisoformat(value)
        except ValueError as exc:
            raise ValueError("--start must use YYYY-MM-DD") from exc
    else:
        today = datetime.now(timezone.utc).astimezone(MADRID).date()
        day = today - timedelta(days=today.weekday())
    if day.weekday() != 0:
        raise ValueError("--start must be a Monday")
    return day


def create_template(modality: str, target_km: float, start: date, capacity_weeks: int) -> Workbook:
    if modality not in MODALITIES or not 1 <= capacity_weeks <= 104:
        raise ValueError("Invalid modality or week capacity")
    book = Workbook()
    config = book.active
    config.title = "Configuración"
    config.column_dimensions["A"].width = 32
    config.column_dimensions["B"].width = 28
    for row in (
        (MARKER, None), ("Modalidad", modality), ("Km mínimo semanal indicado", target_km),
        ("Inicio de la plantilla", start), ("Filas semanales disponibles", capacity_weeks),
        ("Nota", "Las filas no fijan la duración óptima del plan"),
        ("Etiqueta Suunto del plan", plan_tag(modality, start)),
        ("Modo de enlace", "fecha"),
    ):
        config.append(row)
    config["B4"].number_format = "dd/mm/yyyy"
    for cell in config["A"]:
        cell.font = Font(bold=True)
    plan = book.create_sheet("Plan semanal")
    add_header(plan, PLAN_HEADERS, (10, 15, 15, 21, 28, 19, 22, 32, 20, 18, 21, 23, 19, 17, 22, 25))
    for index in range(capacity_weeks):
        first = start + timedelta(weeks=index)
        plan.append((index + 1, first, first + timedelta(days=6), None, None,
                     None, None, None, None, None, target_km))
        plan.cell(index + 2, 2).number_format = "dd/mm/yyyy"
        plan.cell(index + 2, 3).number_format = "dd/mm/yyyy"
    plan.auto_filter.ref = plan.dimensions
    book.create_sheet("Suunto")
    return book


def as_date(value) -> date:
    if isinstance(value, datetime):
        return value.date()
    if isinstance(value, date):
        return value
    raise ValueError("Plan week dates must be Excel dates")


def plan_weeks(book: Workbook) -> list[tuple[int, int, date, date, str | None]]:
    if "Configuración" not in book or "Plan semanal" not in book:
        raise ValueError("This is not a Kore plan template")
    if book["Configuración"]["A1"].value != MARKER:
        raise ValueError("Unsupported plan template version")
    sheet = book["Plan semanal"]
    if tuple(cell.value for cell in sheet[1]) != PLAN_HEADERS:
        raise ValueError("Plan columns were changed; cannot map Suunto data safely")
    weeks = []
    previous_end = None
    for row_index in range(2, sheet.max_row + 1):
        number = sheet.cell(row_index, 1).value
        if number is None:
            continue
        first, last = as_date(sheet.cell(row_index, 2).value), as_date(sheet.cell(row_index, 3).value)
        if last != first + timedelta(days=6) or first.weekday() != 0:
            raise ValueError("Each plan week must run from Monday through Sunday")
        if previous_end is not None and first <= previous_end:
            raise ValueError("Plan weeks overlap or are out of order")
        weeks.append((row_index, number, first, last, sheet.cell(row_index, 4).value))
        previous_end = last
    if not weeks:
        raise ValueError("Plan has no dated weeks")
    return weeks


def pace_text(seconds: float | None, meters: float | None) -> str | None:
    if seconds is None or meters is None or meters <= 0:
        return None
    seconds_per_km = round(seconds * 1000 / meters)
    minutes, remaining = divmod(seconds_per_km, 60)
    return f"{minutes}:{remaining:02d}"


def update_plan(book: Workbook, workouts: list[dict], now: datetime | None = None,
                matching: str = "date") -> Workbook:
    if matching not in ("date", "tag"):
        raise ValueError("Matching must be date or tag")
    weeks = plan_weeks(book)
    config = book["Configuración"]
    tag = config["B7"].value or plan_tag(config["B2"].value, as_date(config["B4"].value))
    config["B7"] = tag
    config["B8"] = "etiqueta" if matching == "tag" else "fecha"
    today = (now or datetime.now(timezone.utc)).astimezone(MADRID).date()
    by_monday = {first: (row_index, number, block) for row_index, number, first, _, block in weeks}
    metrics = defaultdict(lambda: {"runs": 0, "running_km": 0.0, "longest_km": 0.0,
                                   "other_seconds": 0.0, "fit_pending": 0, "matched": 0})
    if "Suunto" in book:
        del book["Suunto"]
    suunto = book.create_sheet("Suunto")
    add_header(suunto, ("Semana", "Bloque", "Fecha", "Inicio local", "Deporte",
                        "Duración (min)", "Distancia (km)", "Ritmo (min:seg/km)",
                        "FC media", "TSS", "Estado FIT", "Clave Suunto", "Etiquetas Suunto",
                        "Desnivel + (m)"),
               (11, 22, 16, 22, 18, 19, 18, 22, 14, 12, 22, 30, 35, 19))
    for workout in workouts:
        user_tags = workout.get("user_tags") or []
        if matching == "tag" and tag not in user_tags:
            continue
        started = datetime.fromtimestamp(workout["start_time_ms"] / 1000, timezone.utc).astimezone(MADRID)
        week_start = started.date() - timedelta(days=started.weekday())
        if week_start not in by_monday:
            continue
        row_index, number, block = by_monday[week_start]
        values = metrics[row_index]
        values["matched"] += 1
        decoded = workout["record_count"] is not None
        if not decoded:
            values["fit_pending"] += 1
        elif workout["sport"] == "running":
            distance_km = (workout["distance_meters"] or 0) / 1000
            values["runs"] += 1
            values["running_km"] += distance_km
            values["longest_km"] = max(values["longest_km"], distance_km)
        else:
            values["other_seconds"] += workout["timer_seconds"] or 0
        status = "Decodificado" if decoded else "FIT pendiente"
        suunto.append((number, block, started.date(), started.replace(tzinfo=None),
                       workout["sport"], rounded(workout["timer_seconds"], 60),
                       rounded(workout["distance_meters"], 1000),
                       pace_text(workout["timer_seconds"], workout["distance_meters"])
                       if workout["sport"] == "running" else None,
                       rounded(workout["avg_heart_rate"]), rounded(workout["training_stress_score"]),
                       status, workout["workout_key"], ", ".join(user_tags),
                       rounded(workout.get("ascent_meters"))))
        for cell in suunto[suunto.max_row]:
            safe_text(cell)
        suunto.cell(suunto.max_row, 3).number_format = "dd/mm/yyyy"
        suunto.cell(suunto.max_row, 4).number_format = "dd/mm/yyyy hh:mm"
    suunto.auto_filter.ref = suunto.dimensions
    plan = book["Plan semanal"]
    for row_index, _, first, last, _ in weeks:
        values = metrics[row_index]
        if first > today:
            actual_km = runs = other_minutes = longest = None
            state = "Futura"
        elif matching == "tag" and values["matched"] == 0:
            actual_km = runs = other_minutes = longest = None
            state = "Sin actividades etiquetadas"
        else:
            actual_km = rounded(values["running_km"])
            runs = values["runs"]
            other_minutes = rounded(values["other_seconds"], 60)
            longest = rounded(values["longest_km"])
            state = "En curso" if last >= today else "Completa"
        planned = plan.cell(row_index, 6).value
        difference = round(actual_km - planned, 2) if actual_km is not None and isinstance(planned, (int, float)) else None
        for column, value in ((9, actual_km), (10, runs), (12, difference), (13, state),
                              (14, values["fit_pending"] if actual_km is not None else None),
                              (15, other_minutes), (16, longest)):
            plan.cell(row_index, column).value = value
    return book


def create_all(config_path: Path, start: date, capacity_weeks: int, output_dir: Path) -> list[Path]:
    targets = read_targets(config_path)
    paths = [output_path(start, start + timedelta(weeks=capacity_weeks) - timedelta(days=1),
                         output_dir / f"plan-{modality}-{start}.xlsx") for modality in MODALITIES]
    for modality, path in zip(MODALITIES, paths):
        save_private(create_template(modality, targets[modality], start, capacity_weeks), path)
    return paths


def main() -> int:
    parser = argparse.ArgumentParser(description="Create Kore plan templates or insert Suunto results")
    sub = parser.add_subparsers(dest="action", required=True)
    create = sub.add_parser("create", help="Create four modality templates")
    create.add_argument("--config", type=Path, default=CONFIG)
    create.add_argument("--start", help="Monday YYYY-MM-DD; defaults to current Monday")
    create.add_argument("--weeks", type=int, default=52, help="Blank row capacity, not a plan duration")
    create.add_argument("--output-dir", type=Path, default=TEMPLATE_DIR)
    update = sub.add_parser("update", help="Insert Suunto results into a copy of one plan")
    update.add_argument("plan", type=Path)
    update.add_argument("--output", type=Path)
    update.add_argument("--match", choices=("date", "tag"), default="date",
                        help="Use all workouts in each week or only activities bearing the plan tag")
    args = parser.parse_args()
    try:
        if args.action == "create":
            if not 1 <= args.weeks <= 104:
                raise ValueError("--weeks must be between 1 and 104")
            paths = create_all(args.config, monday(args.start), args.weeks, args.output_dir)
            for path in paths:
                print(f"Template created: {path}")
        else:
            book = load_workbook(args.plan)
            weeks = plan_weeks(book)
            workouts, _ = query_data(weeks[0][2], weeks[-1][3])
            if args.output:
                path = output_path(weeks[0][2], weeks[-1][3], args.output)
            else:
                stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
                path = UPDATED_DIR / f"{args.plan.stem}-suunto-{stamp}.xlsx"
            updated = update_plan(book, workouts, matching=args.match)
            save_private(updated, path)
            print(f"Updated plan created: {path} "
                  f"({updated['Suunto'].max_row - 1} workouts linked by {args.match})")
    except (ValueError, OSError, psycopg.Error) as exc:
        print(f"Plan operation failed: {exc if isinstance(exc, ValueError) else type(exc).__name__}",
              file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
