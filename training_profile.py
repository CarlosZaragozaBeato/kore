"""Describe observed training history for a future plan-design agent."""

from __future__ import annotations

import argparse
from datetime import date, datetime, timedelta, timezone
import json
from pathlib import Path
from statistics import median
import sys

import psycopg

from analysis_training import history_bounds, weekly_rows
from export_excel import DATA, MADRID, ROOT, query_data
from plan_excel import CONFIG, MODALITIES, read_targets
from suunto_ingest.probe import atomic_private_write


REPORT_DIR = DATA / "analysis"
WEEKDAYS = ("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
DISTANCE_BINS = (("menos de 5 km", 0, 5), ("5 a menos de 10 km", 5, 10),
                 ("10 a menos de 21 km", 10, 21), ("21 km o más", 21, float("inf")))


def workout_day(row: dict) -> date:
    return datetime.fromtimestamp(row["start_time_ms"] / 1000, timezone.utc).astimezone(MADRID).date()


def is_run(row: dict) -> bool:
    return row["record_count"] is not None and row["sport"] == "running"


def period_summary(workouts: list[dict], first: date, last: date) -> dict:
    included = [row for row in workouts if first <= workout_day(row) <= last]
    runs = [row for row in included if is_run(row)]
    km = sum((row["distance_meters"] or 0) / 1000 for row in runs)
    tss_values = [row["training_stress_score"] for row in runs
                  if row["training_stress_score"] is not None]
    other = [row for row in included if row["record_count"] is not None and row["sport"] != "running"]
    return {
        "from_date": first.isoformat(), "through_date": last.isoformat(),
        "running_sessions": len(runs), "running_km": round(km, 1),
        "running_hours": round(sum(row["timer_seconds"] or 0 for row in runs) / 3600, 1),
        "running_days": len({workout_day(row) for row in runs}),
        "longest_run_km": round(max(((row["distance_meters"] or 0) / 1000 for row in runs), default=0), 1),
        "ascent_meters": round(sum(row.get("ascent_meters") or 0 for row in runs)),
        "tss_total": round(sum(tss_values), 1), "tss_sessions": len(tss_values),
        "tss_coverage": round(len(tss_values) / len(runs), 3) if runs else None,
        "other_sport_sessions": len(other),
        "other_sport_hours": round(sum(row["timer_seconds"] or 0 for row in other) / 3600, 1),
        "workouts_without_current_fit": sum(row["record_count"] is None for row in included),
    }


def recent_window(workouts: list[dict], today: date, days: int, offset_days: int = 0) -> dict:
    last = today - timedelta(days=1 + offset_days)
    first = last - timedelta(days=days - 1)
    return period_summary(workouts, first, last)


def distance_distribution(runs: list[dict]) -> list[dict]:
    result = []
    for label, low, high in DISTANCE_BINS:
        group = [row for row in runs if row["distance_meters"] is not None
                 and low <= row["distance_meters"] / 1000 < high]
        paces = [row["timer_seconds"] * 1000 / row["distance_meters"]
                 for row in group if row["timer_seconds"] is not None and row["distance_meters"] > 0]
        hearts = [row["avg_heart_rate"] for row in group if row["avg_heart_rate"] is not None]
        result.append({
            "distance_band": label, "sessions": len(group),
            "median_pace_seconds_per_km": round(median(paces)) if paces else None,
            "median_average_heart_rate": round(median(hearts)) if hearts else None,
        })
    return result


def compute_profile(first: date, today: date, workouts: list[dict], weeks: list[dict],
                    targets: dict[str, float], last_success: datetime | None,
                    subjective: dict | None = None) -> dict:
    completed = [row for row in weeks if row["complete"]]
    recent_12 = completed[-12:]
    recent_12_start = recent_12[0]["week_start"] if recent_12 else today
    recent_runs = [row for row in workouts if is_run(row) and recent_12_start <= workout_day(row) < today]
    all_runs = [row for row in workouts if is_run(row)]
    weekdays = []
    for index, name in enumerate(WEEKDAYS):
        group = [row for row in recent_runs if workout_day(row).weekday() == index]
        weekdays.append({"day": name, "sessions": len(group),
                         "running_km": round(sum((row["distance_meters"] or 0) / 1000 for row in group), 1)})

    def weekly_average(count: int) -> float | None:
        return round(sum(row["km"] for row in completed[-count:]) / count, 1) if len(completed) >= count else None

    last_run = max((workout_day(row) for row in all_runs), default=None)
    return {
        "generated_at": datetime.now(timezone.utc).astimezone(MADRID).isoformat(timespec="seconds"),
        "timezone": "Europe/Madrid", "first_workout_date": first.isoformat(),
        "through_date": (today - timedelta(days=1)).isoformat(),
        "last_workout_date": max((workout_day(row) for row in workouts), default=None).isoformat()
            if workouts else None,
        "last_run_date": last_run.isoformat() if last_run else None,
        "days_since_last_run": (today - last_run).days if last_run else None,
        "last_successful_ingestion_at": last_success.isoformat() if last_success else None,
        "total_workouts": len(workouts), "decoded_running_sessions": len(all_runs),
        "workouts_without_current_fit": sum(row["record_count"] is None for row in workouts),
        "running_tss_sessions": sum(row["training_stress_score"] is not None for row in all_runs),
        "completed_calendar_weeks": len(completed),
        "recent_completed_weekly_km": [
            {"week_start": row["week_start"].isoformat(), "running_km": round(row["km"], 1),
             "running_sessions": row["runs"], "longest_run_km": round(row["longest_km"], 1),
             "ascent_meters": round(row["ascent_meters"]), "tss_sessions": row["tss_runs"],
             "workouts_without_current_fit": row["pending_fit"]}
            for row in recent_12
        ],
        "weekly_km_averages": {str(n): weekly_average(n) for n in (4, 8, 12)},
        "recent_12_week_median_km": round(median(row["km"] for row in recent_12), 1)
            if recent_12 else None,
        "recent_12_week_peak_km": round(max(row["km"] for row in recent_12), 1)
            if recent_12 else None,
        "recent_12_week_median_runs": median(row["runs"] for row in recent_12)
            if recent_12 else None,
        "all_time_peak_week_km": round(max((row["km"] for row in completed), default=0), 1),
        "all_time_longest_run_km": round(max(((row["distance_meters"] or 0) / 1000
                                               for row in all_runs), default=0), 1),
        "last_12_week_longest_run_km": round(max(((row["distance_meters"] or 0) / 1000
                                                   for row in recent_runs), default=0), 1),
        "windows": {
            "last_7_days": recent_window(workouts, today, 7),
            "previous_7_days": recent_window(workouts, today, 7, 7),
            "last_28_days": recent_window(workouts, today, 28),
            "previous_28_days": recent_window(workouts, today, 28, 28),
            "last_84_days": recent_window(workouts, today, 84),
        },
        "recent_12_week_day_pattern": weekdays,
        "recent_12_week_run_distance_distribution": distance_distribution(recent_runs),
        "declared_minimum_weekly_km": targets,
        "completed_weeks_meeting_declared_minimum": {
            modality: {"recent_12": sum(row["km"] >= target for row in recent_12),
                       "all_history": sum(row["km"] >= target for row in completed)}
            for modality, target in targets.items()
        },
        "subjective_status": subjective or {"reported_on": None, "fatigue": None, "limitations": None},
    }


def pace_label(seconds: int | None) -> str:
    if seconds is None:
        return "sin dato"
    minutes, remainder = divmod(seconds, 60)
    return f"{minutes}:{remainder:02d} min/km"


def render_markdown(profile: dict, history_xlsx: Path) -> str:
    averages = profile["weekly_km_averages"]
    current = profile["windows"]["last_28_days"]
    previous = profile["windows"]["previous_28_days"]
    today = profile["through_date"]
    lines = [
        "# Perfil de entrenamiento observado para el agente de Kore",
        "",
        f"Generado: {profile['generated_at']}. Zona horaria: Europe/Madrid.",
        f"Análisis detallado: `{history_xlsx}`.",
        "",
        "## Alcance",
        "",
        "Este perfil describe entrenamientos guardados en Suunto. No mide por sí solo salud, "
        "recuperación, estado de forma actual ni capacidad de competición. "
        "El agente debe combinarlo con sensaciones, molestias, disponibilidad y objetivos actuales.",
        "",
        "## Vigencia y cobertura",
        "",
        f"- Histórico observado: {profile['first_workout_date']} a {today}; "
        f"último entrenamiento: {profile['last_workout_date'] or 'sin dato'}.",
        f"- Última ingesta completa: {profile['last_successful_ingestion_at'] or 'sin dato'}.",
        f"- {profile['total_workouts']} entrenamientos; "
        f"{profile['decoded_running_sessions']} carreras con FIT vigente; "
        f"{profile['workouts_without_current_fit']} entrenamientos sin FIT vigente.",
        f"- TSS disponible en {profile['running_tss_sessions']} de "
        f"{profile['decoded_running_sessions']} carreras. Un TSS ausente no equivale a cero.",
        f"- Última carrera: {profile['last_run_date'] or 'sin dato'} "
        f"({profile['days_since_last_run']} días desde entonces).",
        "",
        "## Volumen y frecuencia observados",
        "",
        f"- Semanas completas analizables: {profile['completed_calendar_weeks']}. "
        "La semana actual queda excluida de las comparaciones.",
        f"- Media de kilómetros de carrera: últimas 4 semanas completas "
        f"{averages['4']} km/semana; 8 semanas {averages['8']}; 12 semanas {averages['12']}.",
        f"- Últimas 12 semanas completas: mediana {profile['recent_12_week_median_km']} km/semana, "
        f"máximo {profile['recent_12_week_peak_km']} km y mediana "
        f"{profile['recent_12_week_median_runs']} carreras/semana.",
        f"- Tirada más larga: {profile['last_12_week_longest_run_km']} km en las últimas "
        f"12 semanas completas; {profile['all_time_longest_run_km']} km en todo el histórico.",
        "",
        "## Ventanas móviles de días completos",
        "",
        "| Ventana | Carreras | Km carrera | Días con carrera | Tirada más larga | Otros deportes | TSS con cobertura |",
        "|---|---:|---:|---:|---:|---:|---|",
    ]
    for label, key in (("Últimos 7 días", "last_7_days"), ("7 días anteriores", "previous_7_days"),
                       ("Últimos 28 días", "last_28_days"), ("28 días anteriores", "previous_28_days"),
                       ("Últimos 84 días", "last_84_days")):
        row = profile["windows"][key]
        lines.append(f"| {label} ({row['from_date']}–{row['through_date']}) | "
                     f"{row['running_sessions']} | {row['running_km']:.1f} | {row['running_days']} | "
                     f"{row['longest_run_km']:.1f} km | {row['other_sport_sessions']} | "
                     f"{row['tss_sessions']}/{row['running_sessions']} carreras |")
    lines.extend([
        "",
        f"Cambio observado entre los dos periodos de 28 días: "
        f"{current['running_km'] - previous['running_km']:+.1f} km y "
        f"{current['running_sessions'] - previous['running_sessions']:+d} carreras. "
        "Es una diferencia descriptiva, no una evaluación de adaptación o fatiga.",
        "",
        "## Patrón de las últimas 12 semanas completas",
        "",
        "| Día | Carreras | Km acumulados |",
        "|---|---:|---:|",
    ])
    for row in profile["recent_12_week_day_pattern"]:
        lines.append(f"| {row['day']} | {row['sessions']} | {row['running_km']:.1f} |")
    lines.extend(["", "### Distancias de las carreras", "",
                  "| Distancia | Sesiones | Mediana del ritmo bruto | Mediana FC media |",
                  "|---|---:|---|---:|"])
    for row in profile["recent_12_week_run_distance_distribution"]:
        lines.append(f"| {row['distance_band']} | {row['sessions']} | "
                     f"{pace_label(row['median_pace_seconds_per_km'])} | "
                     f"{row['median_average_heart_rate'] if row['median_average_heart_rate'] is not None else 'sin dato'} |")
    lines.extend([
        "",
        "Los ritmos se agrupan solo por distancia; terreno, desnivel, clima y objetivo de la sesión "
        "pueden variar. La FC media no equivale a una zona de intensidad sin umbrales personales.",
        "",
        "## Objetivos semanales declarados",
        "",
        "| Modalidad | Mínimo declarado | Semanas de las últimas 12 que alcanzaron esa cifra |",
        "|---|---:|---:|",
    ])
    for modality in MODALITIES:
        lines.append(f"| {modality} | {profile['declared_minimum_weekly_km'][modality]:g} km | "
                     f"{profile['completed_weeks_meeting_declared_minimum'][modality]['recent_12']}/12 |")
    subjective = profile["subjective_status"]
    lines.extend([
        "",
        "Estas cifras son objetivos del usuario, no umbrales universales ni recomendaciones del sistema.",
        "",
        "## Estado subjetivo y límites del dato",
        "",
        f"- Estado comunicado el {subjective.get('reported_on') or 'día no indicado'}: "
        f"fatiga {subjective.get('fatigue') or 'no proporcionada'}; "
        f"molestias o limitaciones {subjective.get('limitations') or 'no proporcionadas'}.",
        "- Esta declaración refleja lo comunicado en esa fecha; debe actualizarse antes de planificar.",
        "- No hay en este perfil mediciones validadas de sueño, HRV, lesiones, esfuerzo percibido "
        "ni umbrales personales para interpretar la frecuencia cardíaca.",
        "- El agente debe preguntar por el estado actual antes de decidir la carga de una sesión o bloque.",
        "",
        "## Uso por el agente de planes",
        "",
        "Tomar las semanas completas y la cobertura como línea base descriptiva. Revisar el Excel histórico "
        "para los detalles de cada sesión y vuelta. Definir con el usuario la modalidad prioritaria, "
        "fecha objetivo, disponibilidad, duración del plan y bloques. No presentar los mínimos "
        "declarados como carga adecuada sin contrastarlos con el historial y el estado actual. "
        "Escribir el plan en las columnas editables de la plantilla; Kore insertará después "
        "los entrenamientos reales desde Suunto.",
        "",
    ])
    return "\n".join(lines)


def private_path(path: Path, extension: str) -> Path:
    path = path.expanduser().resolve()
    if path.suffix.lower() != extension:
        raise ValueError(f"Output must use {extension}")
    if ROOT in path.parents and DATA not in path.parents:
        raise ValueError("Private profile inside the repository must be under data/")
    return path


def main() -> int:
    parser = argparse.ArgumentParser(description="Describe observed Suunto training for a planning agent")
    parser.add_argument("--config", type=Path, default=CONFIG)
    parser.add_argument("--analysis-xlsx", type=Path, help="Detailed workbook referenced by the profile")
    parser.add_argument("--output-md", type=Path, default=REPORT_DIR / "estado-entrenamiento.md")
    parser.add_argument("--output-json", type=Path, default=REPORT_DIR / "estado-entrenamiento.json")
    parser.add_argument("--fatigue", help="Current self-reported fatigue, stored only in private outputs")
    parser.add_argument("--limitations", help="Current self-reported limitations, stored only in private outputs")
    args = parser.parse_args()
    try:
        first, today, last_success = history_bounds()
        workouts, _ = query_data(first, today)
        subjective = {"reported_on": today.isoformat(), "fatigue": args.fatigue,
                      "limitations": args.limitations}
        profile = compute_profile(first, today, workouts, weekly_rows(first, today, workouts),
                                  read_targets(args.config), last_success, subjective)
        detail = args.analysis_xlsx or REPORT_DIR / "training-history.xlsx"
        markdown = private_path(args.output_md, ".md")
        structured = private_path(args.output_json, ".json")
        atomic_private_write(markdown, render_markdown(profile, detail).encode("utf-8"))
        atomic_private_write(structured, (json.dumps(profile, indent=2, ensure_ascii=False) + "\n").encode("utf-8"))
    except (ValueError, OSError, psycopg.Error) as exc:
        print(f"Training profile failed: {exc if isinstance(exc, ValueError) else type(exc).__name__}",
              file=sys.stderr)
        return 1
    print(f"Training profile created: {markdown}")
    print(f"Structured profile created: {structured}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
