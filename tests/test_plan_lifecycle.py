"""Recoverable completion and discard of private active workbooks."""

import unittest
from datetime import datetime, timezone
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

from openpyxl import load_workbook

from plan_builder import build_book
from plan_engine import PlanInputs, generate
from plan_lifecycle import complete_active, discard_active, inspect_active, stored_books
from plan_daily_sync import refresh_active


def active_book():
    return build_book(generate(PlanInputs(
        start_date="2026-11-16", focus_distance="10k", ultimate_distance="42k",
        objective="development", weeks=2, bottleneck="balanced",
        current_weekly_km=80, target_weekly_km=82, recent_long_run_km=16,
        training_days=[0, 1, 2, 3, 5, 6], long_run_day=6,
        quality_sessions_per_week=2, speed_entry_level=1,
        sustained_entry_level=1, max_weekly_increase_km=2,
        max_long_run_increase_km=1, deload_every=4,
        deload_fraction=.8, fatigue="normal",
    )), active=True)


class PlanLifecycleTest(unittest.TestCase):
    def paths(self, folder):
        root = Path(folder)
        return root / "active" / "plan-activo.xlsx", root / "completed", root / "discarded"

    def test_completion_moves_and_marks_the_workbook(self):
        with TemporaryDirectory() as folder:
            active, completed, discarded = self.paths(folder)
            active.parent.mkdir()
            active_book().save(active)
            now = datetime(2026, 12, 1, 12, tzinfo=timezone.utc)
            with patch("plan_lifecycle.ACTIVE", active), \
                 patch("plan_lifecycle.COMPLETED", completed), \
                 patch("plan_lifecycle.DISCARDED", discarded):
                result = complete_active(now, reason="Fin de prueba")
                self.assertFalse(active.exists())
                self.assertEqual(result.destination.parent, completed)
                self.assertEqual(stored_books(completed), (result.destination,))
            book = load_workbook(result.destination, read_only=True, data_only=True)
            self.assertEqual(book["Configuración"]["B8"].value, "Finalizado")
            config = dict(book["Configuración"].iter_rows(values_only=True))
            self.assertEqual(config["Motivo de cierre"], "Fin de prueba")
            book.close()

    def test_automatic_completion_waits_until_after_end_date(self):
        with TemporaryDirectory() as folder:
            active, completed, _ = self.paths(folder)
            active.parent.mkdir()
            active_book().save(active)
            with patch("plan_lifecycle.ACTIVE", active), \
                 patch("plan_lifecycle.COMPLETED", completed):
                with self.assertRaisesRegex(ValueError, "todavía no ha superado"):
                    complete_active(datetime(2026, 11, 29, 12, tzinfo=timezone.utc),
                                    reason="Fin de calendario")
                self.assertTrue(active.exists())

    def test_discard_is_an_atomic_recoverable_move(self):
        with TemporaryDirectory() as folder:
            active, completed, discarded = self.paths(folder)
            active.parent.mkdir()
            active_book().save(active)
            original = active.read_bytes()
            with patch("plan_lifecycle.ACTIVE", active), \
                 patch("plan_lifecycle.COMPLETED", completed), \
                 patch("plan_lifecycle.DISCARDED", discarded):
                self.assertIsNotNone(inspect_active())
                result = discard_active(datetime(2026, 11, 14, 12,
                                                   tzinfo=timezone.utc))
                self.assertFalse(active.exists())
                self.assertEqual(result.destination.read_bytes(), original)
                self.assertEqual(stored_books(discarded), (result.destination,))

    def test_daily_refresh_auto_completes_an_expired_block(self):
        with TemporaryDirectory() as folder:
            active, completed, discarded = self.paths(folder)
            active.parent.mkdir()
            active_book().save(active)

            def save_without_global_archive(book, path):
                book.save(path)

            with patch("plan_daily_sync.ACTIVE", active), \
                 patch("plan_daily_sync.query_workouts", return_value=[]), \
                 patch("plan_daily_sync.save_with_backup", side_effect=save_without_global_archive), \
                 patch("plan_lifecycle.ACTIVE", active), \
                 patch("plan_lifecycle.COMPLETED", completed), \
                 patch("plan_lifecycle.DISCARDED", discarded):
                count = refresh_active(now=datetime(2026, 12, 1, 12,
                                                    tzinfo=timezone.utc))
                self.assertEqual(count, 0)
                self.assertFalse(active.exists())
                self.assertEqual(len(stored_books(completed)), 1)


if __name__ == "__main__":
    unittest.main()
