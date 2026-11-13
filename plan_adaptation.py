"""Explainable weekly adaptation suggestions; never mutates the plan."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import date
from statistics import mean
from typing import Literal

from openpyxl import Workbook

from plan_daily_sync import as_date, validate
from plan_feedback import FeedbackEntry, feedback_entries


Action = Literal["insufficient_data", "reduce", "hold", "progress"]


@dataclass(frozen=True)
class AdaptationSuggestion:
    action: Action
    evaluated_week: int | None
    target_week: int | None
    confidence: Literal["low", "medium"]
    completed_planned_days: int
    days_with_suunto: int
    days_with_feedback: int
    comparable_km_ratio: float | None
    average_rpe: float | None
    minimum_readiness: int | None
    minimum_recovery: int | None
    maximum_soreness_pain: int | None
    reasons: tuple[str, ...]
    proposed_effect: str


def _averaged(values: list[int]) -> float | None:
    return round(mean(values), 1) if values else None


def suggest_adaptation(book: Workbook, today: date) -> AdaptationSuggestion:
    start, end, _ = validate(book)
    weeks = book["Configuración"]["B6"].value
    if today < start:
        return AdaptationSuggestion(
            "insufficient_data", None, 1, "low", 0, 0, 0, None,
            None, None, None, None,
            ("El bloque todavía no ha comenzado.",),
            "Mantener el borrador y registrar sensaciones cuando empiece.",
        )
    if today > end:
        return AdaptationSuggestion(
            "insufficient_data", weeks, None, "low", 0, 0, 0, None,
            None, None, None, None,
            ("El bloque ya ha finalizado; no existe una semana futura que adaptar.",),
            "Cerrar el bloque y revisar sus resultados antes de crear el siguiente.",
        )
    evaluated_week = min(weeks, (today - start).days // 7 + 1)
    target_week = evaluated_week + 1 if evaluated_week < weeks else None
    daily = book["Plan diario"]
    rows = [row for row in daily.iter_rows(min_row=2, values_only=True)
            if row[0] == evaluated_week and as_date(row[2]) < today and row[6] > 0]
    entries = {entry.day: entry for entry in feedback_entries(book)}
    with_suunto = [row for row in rows if isinstance(row[9], (int, float))]
    feedback = [entries[as_date(row[2])] for row in rows if as_date(row[2]) in entries]
    comparable_planned = sum(row[6] for row in with_suunto)
    comparable_actual = sum(row[9] for row in with_suunto)
    ratio = round(comparable_actual / comparable_planned, 2) if comparable_planned else None
    rpes = [entry.session_rpe for entry in feedback if entry.session_rpe is not None]
    readiness = [entry.readiness_before for entry in feedback if entry.readiness_before is not None]
    recovery = [entry.recovery_after for entry in feedback if entry.recovery_after is not None]
    soreness = [entry.soreness_pain for entry in feedback if entry.soreness_pain is not None]
    avg_rpe = _averaged(rpes)
    min_readiness = min(readiness, default=None)
    min_recovery = min(recovery, default=None)
    max_soreness = max(soreness, default=None)
    metrics = dict(
        evaluated_week=evaluated_week, target_week=target_week,
        completed_planned_days=len(rows), days_with_suunto=len(with_suunto),
        days_with_feedback=len(feedback), comparable_km_ratio=ratio,
        average_rpe=avg_rpe, minimum_readiness=min_readiness,
        minimum_recovery=min_recovery, maximum_soreness_pain=max_soreness,
    )
    if not rows:
        return AdaptationSuggestion(
            "insufficient_data", confidence="low", reasons=("No hay días planificados ya transcurridos en la semana evaluada.",),
            proposed_effect="No cambiar la semana siguiente.", **metrics,
        )
    if len(with_suunto) / len(rows) < 0.6:
        return AdaptationSuggestion(
            "insufficient_data", confidence="low",
            reasons=("Menos del 60% de los días planificados transcurridos tiene métricas válidas de Suunto.",
                     "Una actividad sin etiqueta o FIT pendiente no se interpreta como descanso."),
            proposed_effect="Revisar sincronización y etiquetas antes de cambiar la carga.", **metrics,
        )
    if len(feedback) < 2:
        return AdaptationSuggestion(
            "insufficient_data", confidence="low",
            reasons=("Hay menos de dos días con información subjetiva en la semana evaluada.",),
            proposed_effect="Mantener lo previsto y registrar RPE, recuperación y molestias.", **metrics,
        )
    reduce_reasons = []
    if max_soreness is not None and max_soreness >= 7:
        reduce_reasons.append(f"Molestia/dolor máximo {max_soreness}/10.")
    if min_recovery is not None and min_recovery <= 2:
        reduce_reasons.append(f"Recuperación mínima {min_recovery}/5.")
    if min_readiness is not None and min_readiness <= 2:
        reduce_reasons.append(f"Disponibilidad mínima {min_readiness}/5.")
    if sum(value >= 9 for value in rpes) >= 2:
        reduce_reasons.append("Dos o más sesiones fueron valoradas con RPE 9–10.")
    if reduce_reasons:
        return AdaptationSuggestion(
            "reduce", confidence="medium", reasons=tuple(reduce_reasons),
            proposed_effect=("Proponer la dosis mínima o una regresión para la próxima sesión estructurada; "
                             "no recuperar kilómetros perdidos."), **metrics,
        )
    if (len(feedback) >= 3 and ratio is not None and 0.9 <= ratio <= 1.1
        and avg_rpe is not None and avg_rpe <= 7
        and min_recovery is not None and min_recovery >= 4
        and max_soreness is not None and max_soreness <= 3):
        return AdaptationSuggestion(
            "progress", confidence="medium",
            reasons=("Al menos tres días tienen feedback.",
                     "Los kilómetros comparables están dentro del 90–110% de lo previsto.",
                     "RPE medio, recuperación y molestias cumplen los límites configurados."),
            proposed_effect="Permitir la progresión ya prevista por el bloque; no añadir una progresión extra.",
            **metrics,
        )
    return AdaptationSuggestion(
        "hold", confidence="medium",
        reasons=("La evidencia permite evaluar la semana, pero no cumple todos los criterios de progresión ni reducción.",),
        proposed_effect="Mantener el nivel y la dosis previstos para la siguiente sesión estructurada.",
        **metrics,
    )
