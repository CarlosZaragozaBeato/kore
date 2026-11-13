"""Subjective feedback persistence and explainable weekly actions."""

import unittest
from datetime import date, datetime, timedelta, timezone

from plan_adaptation import suggest_adaptation
from plan_builder import build_book
from plan_engine import PlanInputs, generate
from plan_feedback import FeedbackInput, feedback_entries, upsert_feedback


START = date(2026, 11, 16)


def book():
    inputs = PlanInputs(
        start_date=START, focus_distance="10k", ultimate_distance="42k",
        objective="development", weeks=8, bottleneck="balanced",
        current_weekly_km=80, target_weekly_km=84, recent_long_run_km=18,
        training_days=[0, 1, 2, 3, 5, 6], long_run_day=6,
        quality_sessions_per_week=2, speed_entry_level=2,
        sustained_entry_level=1, max_weekly_increase_km=4,
        max_long_run_increase_km=1, deload_every=4,
        deload_fraction=.8, fatigue="normal",
    )
    return build_book(generate(inputs), active=True)


def add_feedback(workbook, day, *, rpe=6, readiness=4, recovery=4, soreness=2, notes=None):
    return upsert_feedback(
        workbook,
        FeedbackInput(day=day, session_rpe=rpe, readiness_before=readiness,
                      recovery_after=recovery, soreness_pain=soreness, notes=notes),
        datetime(2026, 11, 20, 12, tzinfo=timezone.utc),
    )


def fill_actuals(workbook, through_row=5):
    daily = workbook["Plan diario"]
    for row in range(2, through_row + 1):
        if daily.cell(row, 7).value > 0:
            daily.cell(row, 10).value = daily.cell(row, 7).value
            daily.cell(row, 15).value = "Actividad etiquetada"


class PlanAdaptationTest(unittest.TestCase):
    def test_feedback_upsert_is_private_sheet_and_formula_text_is_safe(self):
        workbook = book()
        add_feedback(workbook, START, notes="=1+1")
        add_feedback(workbook, START, rpe=7, notes="actualizado")
        entries = feedback_entries(workbook)
        self.assertEqual(len(entries), 1)
        self.assertEqual((entries[0].session_rpe, entries[0].notes), (7, "actualizado"))
        self.assertEqual(workbook["Feedback"]["F2"].data_type, "s")

    def test_missing_suunto_is_unknown_instead_of_zero(self):
        suggestion = suggest_adaptation(book(), date(2026, 11, 20))
        self.assertEqual(suggestion.action, "insufficient_data")
        self.assertIsNone(suggestion.comparable_km_ratio)
        self.assertIn("Suunto", " ".join(suggestion.reasons))

    def test_good_completion_allows_only_planned_progression(self):
        workbook = book()
        fill_actuals(workbook)
        for offset in (0, 1, 2):
            add_feedback(workbook, START + timedelta(days=offset))
        suggestion = suggest_adaptation(workbook, date(2026, 11, 20))
        self.assertEqual(suggestion.action, "progress")
        self.assertEqual(suggestion.target_week, 2)
        self.assertIn("no añadir", suggestion.proposed_effect)

    def test_soreness_proposes_reduction_without_mutating_plan(self):
        workbook = book()
        fill_actuals(workbook)
        original = workbook["Plan diario"]["G10"].value
        add_feedback(workbook, START, soreness=8)
        add_feedback(workbook, START + timedelta(days=1), soreness=4)
        suggestion = suggest_adaptation(workbook, date(2026, 11, 20))
        self.assertEqual(suggestion.action, "reduce")
        self.assertEqual(workbook["Plan diario"]["G10"].value, original)

    def test_finished_block_has_no_future_adaptation(self):
        workbook = book()
        suggestion = suggest_adaptation(workbook, date(2027, 1, 20))
        self.assertEqual(suggestion.action, "insufficient_data")
        self.assertIsNone(suggestion.target_week)
        self.assertIn("finalizado", " ".join(suggestion.reasons))


if __name__ == "__main__":
    unittest.main()
