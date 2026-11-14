"""Preview and explicitly apply safe changes to future plan sessions."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime
from typing import Literal

from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill

from plan_adaptation import AdaptationSuggestion
from plan_daily_sync import as_date, validate


ADJUSTMENT_SHEET = "Adaptaciones"
ADJUSTMENT_HEADERS = (
    "Aplicado", "Semana", "Acción", "Hoja", "Celda", "Campo",
    "Valor anterior", "Valor nuevo",
)
AdjustableAction = Literal["reduce", "hold", "progress"]


@dataclass(frozen=True)
class PlanChange:
    sheet: str
    row: int
    column: int
    day: date
    field: str
    before: object
    after: object

    @property
    def coordinate(self) -> str:
        from openpyxl.utils import get_column_letter
        return f"{get_column_letter(self.column)}{self.row}"


@dataclass(frozen=True)
class AdjustmentProposal:
    action: AdjustableAction
    evaluated_week: int
    target_week: int
    changes: tuple[PlanChange, ...]
    summary: str


def _decision_row(book: Workbook, day: date) -> int | None:
    sheet = book["Decisiones de sesión"]
    return next((row for row in range(2, sheet.max_row + 1)
                 if as_date(sheet.cell(row, 2).value) == day), None)


def _change(changes: list[PlanChange], sheet: str, row: int, column: int,
            day: date, field: str, before, after) -> None:
    if before != after:
        changes.append(PlanChange(sheet, row, column, day, field, before, after))


def _structured_rows(book: Workbook, week: int) -> list[int]:
    daily = book["Plan diario"]
    return [row for row in range(2, daily.max_row + 1)
            if daily.cell(row, 1).value == week
            and daily.cell(row, 6).value in ("speed", "sustained")]


def _previous_role_row(book: Workbook, target_row: int) -> int | None:
    daily = book["Plan diario"]
    target_week = daily.cell(target_row, 1).value
    role = daily.cell(target_row, 6).value
    candidates = [row for row in range(2, target_row)
                  if daily.cell(row, 1).value < target_week
                  and daily.cell(row, 6).value == role]
    return candidates[-1] if candidates else None


def build_adjustment_proposal(book: Workbook, suggestion: AdaptationSuggestion,
                              today: date) -> AdjustmentProposal:
    """Create an inspectable proposal without mutating the workbook."""
    validate(book)
    if suggestion.action == "insufficient_data" or suggestion.target_week is None:
        raise ValueError("No hay una recomendación accionable para una semana futura")
    if suggestion.action not in ("reduce", "hold", "progress"):
        raise ValueError("La acción de adaptación no está soportada")
    target_week = suggestion.target_week
    target_rows = _structured_rows(book, target_week)
    daily = book["Plan diario"]
    if any(as_date(daily.cell(row, 3).value) <= today for row in target_rows):
        raise ValueError("Una adaptación solo puede modificar fechas futuras")
    changes: list[PlanChange] = []

    if suggestion.action == "progress":
        return AdjustmentProposal(
            "progress", suggestion.evaluated_week, target_week, (),
            "La progresión ya está incluida en el plan; no se añade carga ni se escribe el libro.",
        )

    if suggestion.action == "reduce":
        row = next((candidate for candidate in target_rows
                    if daily.cell(candidate, 22).value is not None
                    and daily.cell(candidate, 23).value != daily.cell(candidate, 22).value), None)
        if row is None:
            return AdjustmentProposal(
                "reduce", suggestion.evaluated_week, target_week, (),
                "No hay una sesión estructurada futura cuya dosis pueda reducirse de forma automática.",
            )
        day = as_date(daily.cell(row, 3).value)
        minimum, target = daily.cell(row, 22).value, daily.cell(row, 23).value
        detail = daily.cell(row, 8).value
        reduced_detail = detail.replace(f"Dosis prevista: {target}.", f"Dosis prevista: {minimum}.")
        _change(changes, "Plan diario", row, 8, day, "Detalle", detail, reduced_detail)
        _change(changes, "Plan diario", row, 23, day, "Dosis prevista", target, minimum)
        decision = _decision_row(book, day)
        if decision is not None:
            before = book["Decisiones de sesión"].cell(decision, 6).value
            after = f"Adaptación confirmada: dosis mínima por recomendación de reducción. {before}"
            _change(changes, "Decisiones de sesión", decision, 6, day,
                    "Motivo de selección", before, after)
        summary = "Reducir a la dosis mínima la primera sesión estructurada de la semana objetivo."
    else:
        copy_columns = (
            (5, "Sesión"), (8, "Detalle"), (21, "Plantilla"),
            (22, "Dosis mínima"), (23, "Dosis prevista"), (24, "Dosis máxima"),
        )
        for row in target_rows:
            source = _previous_role_row(book, row)
            if source is None:
                continue
            day = as_date(daily.cell(row, 3).value)
            for column, field in copy_columns:
                _change(changes, "Plan diario", row, column, day, field,
                        daily.cell(row, column).value, daily.cell(source, column).value)
            decision = _decision_row(book, day)
            if decision is not None:
                decisions = book["Decisiones de sesión"]
                desired = {
                    3: daily.cell(source, 21).value,
                    4: daily.cell(source, 5).value,
                    5: daily.cell(source, 6).value,
                    6: "Adaptación confirmada: mantener el recurso y la dosis del nivel anterior.",
                }
                for column, after in desired.items():
                    _change(changes, "Decisiones de sesión", decision, column, day,
                            "Motivo de selección" if column == 6 else decisions.cell(1, column).value,
                            decisions.cell(decision, column).value, after)
        summary = "Mantener en la semana objetivo los recursos y dosis del nivel anterior, sin cambiar kilómetros."

    return AdjustmentProposal(suggestion.action, suggestion.evaluated_week,
                              target_week, tuple(changes), summary)


def _ensure_audit_sheet(book: Workbook):
    if ADJUSTMENT_SHEET in book:
        sheet = book[ADJUSTMENT_SHEET]
        if tuple(cell.value for cell in sheet[1]) != ADJUSTMENT_HEADERS:
            raise ValueError("Las columnas de auditoría de adaptaciones han cambiado")
        return sheet
    sheet = book.create_sheet(ADJUSTMENT_SHEET)
    sheet.append(ADJUSTMENT_HEADERS)
    for cell in sheet[1]:
        cell.fill = PatternFill("solid", fgColor="1F4E78")
        cell.font = Font(color="FFFFFF", bold=True)
    for column, width in {"A": 22, "B": 12, "C": 16, "D": 24, "E": 12,
                          "F": 24, "G": 55, "H": 55}.items():
        sheet.column_dimensions[column].width = width
    sheet.freeze_panes = "A2"
    return sheet


def apply_adjustment(book: Workbook, proposal: AdjustmentProposal,
                     now: datetime) -> None:
    """Apply a reviewed proposal if every source cell is still unchanged."""
    validate(book)
    if not proposal.changes:
        raise ValueError("La propuesta no contiene cambios que aplicar")
    for change in proposal.changes:
        if change.day <= now.date():
            raise ValueError("No se pueden adaptar fechas actuales ni pasadas")
        current = book[change.sheet].cell(change.row, change.column).value
        if current != change.before:
            raise ValueError(f"El plan cambió tras la vista previa en {change.sheet}!{change.coordinate}")
    audit = _ensure_audit_sheet(book)
    for change in proposal.changes:
        cell = book[change.sheet].cell(change.row, change.column)
        cell.value = change.after
        if isinstance(change.after, str):
            cell.data_type = "s"
        audit.append((now.replace(tzinfo=None), proposal.target_week, proposal.action,
                      change.sheet, change.coordinate, change.field,
                      str(change.before) if change.before is not None else None,
                      str(change.after) if change.after is not None else None))
        for cell in audit[audit.max_row]:
            if isinstance(cell.value, str):
                cell.data_type = "s"
        audit.cell(audit.max_row, 1).number_format = "dd/mm/yyyy hh:mm"
    audit.auto_filter.ref = audit.dimensions
