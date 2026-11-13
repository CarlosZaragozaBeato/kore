"""Subjective daily feedback stored inside the private active workbook."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime

from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill
from pydantic import BaseModel, Field, model_validator

from plan_daily_sync import as_date, validate


FEEDBACK_SHEET = "Feedback"
FEEDBACK_HEADERS = (
    "Fecha", "RPE sesión (1-10)", "Disponibilidad antes (1-5)",
    "Recuperación posterior (1-5)", "Molestia/dolor (0-10)",
    "Notas", "Actualizado",
)


class FeedbackInput(BaseModel):
    day: date
    session_rpe: int | None = Field(default=None, ge=1, le=10)
    readiness_before: int | None = Field(default=None, ge=1, le=5)
    recovery_after: int | None = Field(default=None, ge=1, le=5)
    soreness_pain: int | None = Field(default=None, ge=0, le=10)
    notes: str | None = Field(default=None, max_length=1000)

    @model_validator(mode="after")
    def has_observation(self):
        if all(value is None for value in (
            self.session_rpe, self.readiness_before, self.recovery_after,
            self.soreness_pain, self.notes.strip() if self.notes else None,
        )):
            raise ValueError("at least one feedback value is required")
        self.notes = self.notes.strip() if self.notes else None
        return self


@dataclass(frozen=True)
class FeedbackEntry:
    day: date
    session_rpe: int | None
    readiness_before: int | None
    recovery_after: int | None
    soreness_pain: int | None
    notes: str | None
    updated_at: datetime | None


def ensure_feedback_sheet(book: Workbook):
    if FEEDBACK_SHEET in book:
        sheet = book[FEEDBACK_SHEET]
        if tuple(cell.value for cell in sheet[1]) != FEEDBACK_HEADERS:
            raise ValueError("Feedback columns changed")
        return sheet
    sheet = book.create_sheet(FEEDBACK_SHEET)
    sheet.append(FEEDBACK_HEADERS)
    for cell in sheet[1]:
        cell.fill = PatternFill("solid", fgColor="1F4E78")
        cell.font = Font(color="FFFFFF", bold=True)
    for column, width in {"A": 16, "B": 20, "C": 25, "D": 29,
                          "E": 24, "F": 70, "G": 22}.items():
        sheet.column_dimensions[column].width = width
    sheet.freeze_panes = "A2"
    sheet.auto_filter.ref = sheet.dimensions
    return sheet


def feedback_entries(book: Workbook) -> list[FeedbackEntry]:
    if FEEDBACK_SHEET not in book:
        return []
    sheet = ensure_feedback_sheet(book)
    entries = []
    for row in sheet.iter_rows(min_row=2, values_only=True):
        if row[0] is None:
            continue
        entries.append(FeedbackEntry(as_date(row[0]), row[1], row[2], row[3],
                                     row[4], row[5], row[6]))
    return sorted(entries, key=lambda entry: entry.day)


def upsert_feedback(book: Workbook, feedback: FeedbackInput,
                    now: datetime) -> FeedbackEntry:
    start, end, _ = validate(book)
    if feedback.day > now.date():
        raise ValueError("feedback date cannot be in the future")
    if not start <= feedback.day <= end:
        raise ValueError("feedback date is outside the active block")
    sheet = ensure_feedback_sheet(book)
    row_index = next((row for row in range(2, sheet.max_row + 1)
                      if as_date(sheet.cell(row, 1).value) == feedback.day),
                     sheet.max_row + 1)
    values = (feedback.day, feedback.session_rpe, feedback.readiness_before,
              feedback.recovery_after, feedback.soreness_pain,
              feedback.notes, now.replace(tzinfo=None))
    for column, value in enumerate(values, 1):
        sheet.cell(row_index, column).value = value
        if isinstance(value, str):
            sheet.cell(row_index, column).data_type = "s"
    sheet.cell(row_index, 1).number_format = "dd/mm/yyyy"
    sheet.cell(row_index, 7).number_format = "dd/mm/yyyy hh:mm"
    sheet.auto_filter.ref = sheet.dimensions
    return FeedbackEntry(feedback.day, feedback.session_rpe,
                         feedback.readiness_before, feedback.recovery_after,
                         feedback.soreness_pain, feedback.notes, now)
