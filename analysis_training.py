"""Generate private historical training analysis and planner context."""

from __future__ import annotations

import argparse
from collections import defaultdict
from datetime import date, datetime, timedelta, timezone
from pathlib import Path
from statistics import median
import sys

import psycopg

from export_excel import (
    DATA, MADRID, ROOT, add_header, build_workbook, database_url, output_path,
    query_data, rounded, save_private,
)
from plan_excel import CONFIG, MODALITIES, pace_text, read_targets
from suunto_ingest.probe import atomic_private_write


ANALYSIS_DIR = DATA / "analysis"


def history_bounds() -> tuple[date, date, datetime | None]:
    with psycopg.connect(database_url(), connect_timeout=10,
                         options="-c default_transaction_read_only=on") as db:
        first_ms, last_ms = db.execute(
            "SELECT min(start_time_ms), max(start_time_ms) FROM suunto_workouts"
        ).fetchone()
        if first_ms is None or last_ms is None:
            raise ValueError("No Suunto workouts are stored yet")
        last_success = None
        if db.execute("SELECT to_regclass('suunto_pipeline_runs')").fetchone()[0]:
            last_success = db.execute("""SELECT max(finished_at)
                FROM suunto_pipeline_runs WHERE status = 'succeeded'""").fetchone()[0]
    first = datetime.fromtimestamp(first_ms / 1000, timezone.utc).astimezone(MADRID).date()
    today = datetime.now(timezone.utc).astimezone(MADRID).date()
    return first, today, last_success


def weekly_rows(first: date, today: date, workouts: list[dict]) -> list[dict]:
    by_week = defaultdict(lambda: {"runs": 0, "km": 0.0, "run_seconds": 0.0,
                                   "longest_km": 0.0, "ascent_meters": 0.0,
                                   "run_tss": 0.0, "tss_runs": 0,
                                   "other_sessions": 0, "other_seconds": 0.0,
                                   "pending_fit": 0})
    for workout in workouts:
        day = datetime.fromtimestamp(workout["start_time_ms"] / 1000, timezone.utc).astimezone(MADRID).date()
        monday = day - timedelta(days=day.weekday())
        values = by_week[monday]
        if workout["record_count"] is None:
            values["pending_fit"] += 1
        elif workout["sport"] == "running":
            km = (workout["distance_meters"] or 0) / 1000
            values["runs"] += 1
            values["km"] += km
            values["run_seconds"] += workout["timer_seconds"] or 0
            values["longest_km"] = max(values["longest_km"], km)
            values["ascent_meters"] += workout.get("ascent_meters") or 0
            if workout["training_stress_score"] is not None:
                values["run_tss"] += workout["training_stress_score"]
                values["tss_runs"] += 1
        else:
            values["other_sessions"] += 1
            values["other_seconds"] += workout["timer_seconds"] or 0
    rows = []
    week = first - timedelta(days=first.weekday())
    completed_km = []
    while week <= today:
        values = by_week[week]
        complete = week >= first and week + timedelta(days=6) < today
        if complete:
            completed_km.append(values["km"])
        rows.append({
            "week_start": week, "week_end": week + timedelta(days=6),
            "complete": complete, **values,
            "average_4w_km": sum(completed_km[-4:]) / 4 if complete and len(completed_km) >= 4 else None,
        })
        week += timedelta(days=7)
    return rows


def build_analysis(first: date, today: date, workouts: list[dict], laps: list[dict],
                   weeks: list[dict], targets: dict[str, float]):
    book = build_workbook(first, today, workouts, laps)
    weekly = book.create_sheet("Semanas carrera", 1)
    add_header(weekly, (
        "Semana desde", "Semana hasta", "Completa", "Sesiones carrera", "Km carrera",
        "Duración carrera (min)", "Tirada más larga (km)", "Desnivel + (m)",
        "TSS carrera", "Carreras con TSS", "Otros deportes", "Otros deportes (min)",
        "FIT pendientes", "Media 4 semanas (km)",
    ), (17, 17, 14, 19, 16, 25, 24, 19, 17, 20, 17, 22, 17, 24))
    for row in weeks:
        weekly.append((row["week_start"], row["week_end"], row["complete"], row["runs"],
                       rounded(row["km"]), rounded(row["run_seconds"], 60),
                       rounded(row["longest_km"]), rounded(row["ascent_meters"]),
                       rounded(row["run_tss"]), row["tss_runs"],
                       row["other_sessions"], rounded(row["other_seconds"], 60),
                       row["pending_fit"], rounded(row["average_4w_km"])))
        weekly.cell(weekly.max_row, 1).number_format = "dd/mm/yyyy"
        weekly.cell(weekly.max_row, 2).number_format = "dd/mm/yyyy"
    weekly.auto_filter.ref = weekly.dimensions

    running = book.create_sheet("Carreras", 2)
    add_header(running, ("Fecha", "Inicio local", "Distancia (km)", "Duración (min)",
                         "Ritmo (min:seg/km)", "FC media", "FC máxima", "Potencia media (W)",
                         "TSS", "Desnivel + (m)", "Clave Suunto"),
               (16, 22, 19, 20, 23, 14, 14, 22, 12, 19, 30))
    for workout in workouts:
        if workout["sport"] != "running" or workout["record_count"] is None:
            continue
        started = datetime.fromtimestamp(workout["start_time_ms"] / 1000, timezone.utc).astimezone(MADRID)
        running.append((started.date(), started.replace(tzinfo=None),
                        rounded(workout["distance_meters"], 1000),
                        rounded(workout["timer_seconds"], 60),
                        pace_text(workout["timer_seconds"], workout["distance_meters"]),
                        rounded(workout["avg_heart_rate"]), rounded(workout["max_heart_rate"]),
                        rounded(workout["avg_power"]), rounded(workout["training_stress_score"]),
                        rounded(workout.get("ascent_meters")), workout["workout_key"]))
        running.cell(running.max_row, 1).number_format = "dd/mm/yyyy"
        running.cell(running.max_row, 2).number_format = "dd/mm/yyyy hh:mm"
    running.auto_filter.ref = running.dimensions

    references = book.create_sheet("Objetivos declarados", 3)
    add_header(references, ("Modalidad", "Km mínimos semanales indicados", "Semanas completas con ese km"),
               (18, 30, 31))
    complete = [row for row in weeks if row["complete"]]
    for modality in MODALITIES:
        references.append((modality, targets[modality],
                           sum(row["km"] >= targets[modality] for row in complete)))
    references.auto_filter.ref = references.dimensions
    return book


def context_text(first: date, today: date, workouts: list[dict], weeks: list[dict],
                 targets: dict[str, float], last_success: datetime | None,
                 analysis_path: Path) -> str:
    complete = [row for row in weeks if row["complete"]]
    completed_km = [row["km"] for row in complete]
    runs = [row for row in workouts if row["sport"] == "running" and row["record_count"] is not None]
    pending = sum(row["record_count"] is None for row in workouts)
    tss_runs = sum(row["training_stress_score"] is not None for row in runs)
    last_workout = max((datetime.fromtimestamp(row["start_time_ms"] / 1000, timezone.utc)
                        .astimezone(MADRID).date() for row in workouts), default=None)

    def average_last(number: int) -> str:
        if len(completed_km) < number:
            return "sin suficientes semanas completas"
        return f"{sum(completed_km[-number:]) / number:.1f} km/semana"

    lines = [
        "# Contexto privado para el agente de planificación de Kore",
        "",
        f"Generado: {datetime.now(timezone.utc).astimezone(MADRID).isoformat(timespec='minutes')}",
        f"Análisis detallado: `{analysis_path}`",
        "",
        "## Rol y límites",
        "",
        "Diseñar los bloques y semanas de los planes en las plantillas Excel de Kore. "
        "La duración del plan y las cargas planificadas las decide el agente con el usuario; "
        "las filas de la plantilla son capacidad editable, no una recomendación.",
        "Los mínimos siguientes son objetivos declarados, no umbrales validados ni prueba de que sean apropiados.",
        "No interpretar TSS ausente como cero ni una semana en curso como semana completa.",
        "",
        "## Objetivos declarados de kilometraje semanal",
        "",
    ]
    lines.extend(f"- {modality}: {targets[modality]:g} km mínimos por semana." for modality in MODALITIES)
    lines.extend([
        "",
        "## Evidencia observada de Suunto",
        "",
        f"- Periodo disponible: {first} a {today} (Europe/Madrid).",
        f"- Último entrenamiento registrado: {last_workout or 'sin dato'}.",
        f"- Última ingesta completa: {last_success.isoformat() if last_success else 'sin dato'}.",
        f"- Entrenamientos totales: {len(workouts)}; carreras decodificadas: {len(runs)}; "
        f"entrenamientos sin FIT vigente decodificado: {pending}.",
        f"- TSS disponible en {tss_runs} de {len(runs)} carreras decodificadas.",
        f"- Semanas completas analizables: {len(complete)}; promedio de km de carrera en las últimas 4: "
        f"{average_last(4)}; últimas 8: {average_last(8)}; últimas 12: {average_last(12)}.",
        f"- Mediana semanal completa: {median(completed_km):.1f} km; máximo semanal completo: "
        f"{max(completed_km):.1f} km." if completed_km else "- No hay semanas completas analizables.",
        f"- Tirada más larga observada: {max(((row['distance_meters'] or 0) / 1000 for row in runs), default=0):.1f} km.",
        "",
        "## Cómo rellenar el plan",
        "",
        "Cada libro tiene `Configuración` y `Plan semanal`. Rellenar únicamente las columnas "
        "Bloque, Objetivo de la semana, Km planificados, Sesiones planificadas y Notas del agente "
        "para las semanas elegidas. Mantener Desde/Hasta como lunes/domingo. "
        "Las columnas de km y sesiones reales y la hoja Suunto se actualizan con `plan_excel.py update`.",
        "Consultar `Semanas carrera`, `Carreras`, `Entrenamientos` y `Vueltas` del análisis. "
        "Las semanas incompletas o con FIT pendiente requieren cautela.",
        "",
        "## Información que debe pedir antes de cerrar un plan",
        "",
        "Fecha objetivo, días disponibles, experiencia reciente, preferencias de intensidad, "
        "fatiga o molestias actuales y cualquier restricción relevante. "
        "Contrastar los objetivos semanales declarados con el historial observado; "
        "si no encajan, pedir confirmación y explicar la diferencia.",
        "",
    ])
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description="Build private Suunto history analysis for a plan agent")
    parser.add_argument("--config", type=Path, default=CONFIG)
    parser.add_argument("--output-xlsx", type=Path)
    parser.add_argument("--output-context", type=Path)
    args = parser.parse_args()
    try:
        targets = read_targets(args.config)
        first, today, last_success = history_bounds()
        workouts, laps = query_data(first, today)
        weeks = weekly_rows(first, today, workouts)
        stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
        xlsx = output_path(first, today, args.output_xlsx or ANALYSIS_DIR / f"training-history-{stamp}.xlsx")
        context = args.output_context or ANALYSIS_DIR / "agent-context.md"
        if context.suffix.lower() != ".md":
            raise ValueError("Context output must be a .md file")
        if ROOT in context.resolve().parents and DATA not in context.resolve().parents:
            raise ValueError("Private context inside the repository must be under data/")
        save_private(build_analysis(first, today, workouts, laps, weeks, targets), xlsx)
        atomic_private_write(context, context_text(first, today, workouts, weeks,
                                                   targets, last_success, xlsx).encode("utf-8"))
    except (ValueError, OSError, psycopg.Error) as exc:
        print(f"Training analysis failed: {exc if isinstance(exc, ValueError) else type(exc).__name__}",
              file=sys.stderr)
        return 1
    print(f"Analysis created: {xlsx}")
    print(f"Agent context created: {context}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
