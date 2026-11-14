"""Render parameter-driven drafts in the existing Kore daily workbook format."""

from __future__ import annotations

from datetime import timedelta
from pathlib import Path

from openpyxl import Workbook, load_workbook

from plan_engine import PlanDraft, WEEKDAYS
from plan_workbook import ACTIVE, DAILY_HEADERS, SUUNTO_HEADERS, WEEK_HEADERS, save_with_backup, style_table
from session_catalog import default_catalog


def build_book(draft: PlanDraft, *, active: bool = False) -> Workbook:
    inputs = draft.inputs
    tag = f"kore_{inputs.focus_distance}_{inputs.start_date:%Y%m%d}" if active else None
    book = Workbook()
    daily = book.active
    daily.title = "Plan diario"
    daily.append(DAILY_HEADERS)
    weekly = book.create_sheet("Plan semanal")
    weekly.append(WEEK_HEADERS)
    config = book.create_sheet("Configuración")
    end = inputs.start_date + timedelta(weeks=inputs.weeks, days=-1)
    values = (
        ("KORE_PLAN_DAILY_V2", None),
        ("Modalidad", inputs.focus_distance),
        ("Km objetivo semanal", inputs.target_weekly_km),
        ("Fecha de inicio", inputs.start_date),
        ("Fecha final", end),
        ("Semanas del bloque", inputs.weeks),
        ("Etiqueta Suunto exacta", tag),
        ("Estado del bloque", "Activo" if active else "Borrador"),
        ("Identificador estable", tag),
        ("Día actual", None), ("Semana actual", None), ("Días restantes", None),
        ("Objetivo final", inputs.ultimate_distance),
        ("Tipo de objetivo", inputs.objective),
        ("Fecha de competición", inputs.event_date),
        ("Limitación principal", inputs.bottleneck),
        ("Km semanales actuales", inputs.current_weekly_km),
        ("Tirada larga reciente", inputs.recent_long_run_km),
        ("Fatiga declarada", inputs.fatigue),
        ("Nivel velocidad sugerido por Suunto", inputs.suggested_speed_level),
        ("Confianza sugerencia velocidad", inputs.speed_suggestion_confidence),
        ("Nivel sostenido sugerido por Suunto", inputs.suggested_sustained_level),
        ("Confianza sugerencia sostenido", inputs.sustained_suggestion_confidence),
        ("Evaluación Suunto vigente a", inputs.assessment_as_of),
        ("Ventana de evaluación (días)", inputs.assessment_window_days),
        ("Versión del catálogo", default_catalog().catalog_version),
    )
    for row in values:
        config.append(row)
    for index in (4, 5, 15, 24):
        config.cell(index, 2).number_format = "dd/mm/yyyy"
    config.column_dimensions["A"].width = 32
    config.column_dimensions["B"].width = 48

    for session in draft.sessions:
        daily.append((
            session.week, session.phase, session.day, WEEKDAYS[session.day.weekday()],
            session.name, session.role, session.km, session.description,
            "Por calibrar con prueba o esfuerzo percibido" if session.km else "—",
            None, None, None, None, None,
            "Pendiente" if session.km else "Descanso sin confirmar",
            None, None, None, "Sin versión de ritmo", None,
            1 if session.role in ("speed", "sustained", "long", "race") else None,
            session.dose_min, session.dose_target, session.dose_max,
        ))
        daily.cell(daily.max_row, 3).number_format = "dd/mm/yyyy"
    for week in range(1, inputs.weeks + 1):
        days = draft.sessions[(week - 1) * 7:week * 7]
        start = inputs.start_date + timedelta(weeks=week - 1)
        weekly.append((
            week, start, start + timedelta(days=6), days[0].phase,
            draft.weekly_km[week - 1], None, sum(day.km > 0 for day in days),
            None, "Pendiente", None,
            "Borrador parametrizado; revisar con la respuesta real al entrenamiento.",
            sum(day.role in ("speed", "sustained", "long", "race") for day in days),
        ))
        for column in (2, 3):
            weekly.cell(weekly.max_row, column).number_format = "dd/mm/yyyy"
    suunto = book.create_sheet("Suunto")
    suunto.append(SUUNTO_HEADERS)
    rules = book.create_sheet("Reglas y decisiones")
    rules.append(("Parámetro", "Valor"))
    for key, value in inputs.model_dump(mode="json").items():
        rules.append((key, str(value)))
    rules.append(("Notas", " | ".join(draft.notes)))
    rules.column_dimensions["A"].width = 32
    rules.column_dimensions["B"].width = 120
    decisions = book.create_sheet("Decisiones de sesión")
    decisions.append(("Semana", "Fecha", "Plantilla", "Sesión", "Función", "Motivo de selección"))
    for session in draft.sessions:
        decisions.append((session.week, session.day, session.template_id, session.name,
                          session.role, session.selection_reason))
        decisions.cell(decisions.max_row, 2).number_format = "dd/mm/yyyy"
    decisions.freeze_panes = "A2"
    decisions.auto_filter.ref = decisions.dimensions
    for column, width in {"A": 11, "B": 16, "C": 24, "D": 28, "E": 18, "F": 95}.items():
        decisions.column_dimensions[column].width = width
    style_table(daily, {"A": 10, "B": 17, "C": 16, "D": 13, "E": 26,
                        "F": 18, "G": 14, "H": 80, "I": 35, "J": 14,
                        "K": 18, "L": 16, "M": 13, "N": 12, "O": 27,
                        "P": 38, "Q": 16, "R": 25, "S": 22, "T": 20,
                        "U": 15, "V": 18, "W": 18, "X": 18})
    style_table(weekly, {"A": 12, "B": 16, "C": 16, "D": 18,
                         "E": 17, "F": 17, "G": 18, "H": 17,
                         "I": 27, "J": 17, "K": 80, "L": 18})
    style_table(suunto, {"A": 30, "B": 16, "C": 22, "D": 18, "E": 14,
                         "F": 19, "G": 15, "H": 14, "I": 12, "J": 19, "K": 45})
    return book


def activate_draft(draft: PlanDraft, *, replace_existing: bool = False) -> Path:
    """Explicit activation; preserve previous workbook via the existing archive."""
    if ACTIVE.exists():
        previous = load_workbook(ACTIVE, read_only=True, data_only=True)
        try:
            has_results = previous["Suunto"].max_row > 1 or any(
                any(value is not None for value in row[9:14] + row[15:18]) or row[19] is not None
                for row in previous["Plan diario"].iter_rows(min_row=2, values_only=True)
            )
        finally:
            previous.close()
        if has_results and not replace_existing:
            raise ValueError("The active book has Suunto results; archive and confirm replacement explicitly")
    book = build_book(draft, active=True)
    from plan_daily_sync import validate
    validate(book)
    save_with_backup(book, ACTIVE)
    return ACTIVE
