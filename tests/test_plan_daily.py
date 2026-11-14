"""Synthetic daily matching cases; no private Suunto records are used."""

import unittest
from datetime import date, datetime, timezone

from plan_builder import build_book
from plan_daily_sync import update_book
from plan_engine import PlanInputs, generate


START = date(2026, 11, 16)
TAG = "kore_10k_20261116"


def active_book():
    draft = generate(PlanInputs(
        start_date=START, focus_distance="10k", ultimate_distance="42k",
        objective="development", weeks=12, bottleneck="balanced",
        current_weekly_km=74, target_weekly_km=86, recent_long_run_km=15,
        training_days=[0, 1, 2, 3, 5, 6], long_run_day=6,
        quality_sessions_per_week=2, speed_entry_level=0,
        sustained_entry_level=0, max_weekly_increase_km=2,
        max_long_run_increase_km=1, deload_every=4,
        deload_fraction=.8, fatigue="normal",
    ))
    return draft, build_book(draft, active=True)


def workout(key, when, *, tags=None, km=9.8, sport="running", decoded=True):
    return {
        "workout_key": key,
        "start_time_ms": int(when.timestamp() * 1000),
        "user_tags": tags if tags is not None else [TAG],
        "record_count": 100 if decoded else None,
        "fit_status": "ok" if decoded else "missing",
        "sport": sport if decoded else None,
        "distance_meters": km * 1000 if decoded else None,
        "timer_seconds": km * 300 if decoded else None,
        "avg_heart_rate": 140 if decoded else None,
        "training_stress_score": 42 if decoded else None,
    }


class DailyPlanTests(unittest.TestCase):
    def setUp(self):
        self.draft, self.book = active_book()
        self.now = datetime(2026, 11, 20, 12, tzinfo=timezone.utc)

    def test_labeled_monday_updates_same_row_and_is_idempotent(self):
        # 23:30 UTC on Sunday is 00:30 Monday in Europe/Madrid.
        monday = workout("run-1", datetime(2026, 11, 15, 23, 30, tzinfo=timezone.utc))
        ignored = workout("no-tag", datetime(2026, 11, 16, 8, tzinfo=timezone.utc), tags=[])
        update_book(self.book, [monday, monday, ignored], now=self.now)
        row = self.book["Plan diario"][2]
        self.assertEqual((row[0].value, row[3].value, row[4].value, row[6].value),
                         (1, "lunes", self.draft.sessions[0].name, self.draft.sessions[0].km))
        self.assertEqual(row[9].value, 9.8)
        self.assertEqual(row[15].value, "run-1")
        self.assertEqual(self.book["Suunto"].max_row, 2)
        self.assertEqual(self.book["Plan semanal"]["E2"].value, self.draft.weekly_km[0])
        self.assertEqual(self.book["Plan semanal"]["F2"].value, 9.8)
        update_book(self.book, [monday, ignored], now=self.now)
        self.assertEqual(self.book["Suunto"].max_row, 2)
        self.assertEqual((row[6].value, row[9].value),
                         (self.draft.sessions[0].km, 9.8))
        self.assertEqual(self.book["Configuración"]["B10"].value, "Día 5 de 84")
        self.assertEqual(self.book["Configuración"]["B11"].value, "Semana 1 de 12")

    def test_multiple_activities_no_tag_rest_and_fit_pending(self):
        a = workout("a", datetime(2026, 11, 16, 7, tzinfo=timezone.utc), km=9.8)
        b = workout("b", datetime(2026, 11, 16, 18, tzinfo=timezone.utc), km=2.0)
        pending = workout("pending", datetime(2026, 11, 17, 8, tzinfo=timezone.utc), decoded=False)
        unrelated = workout("unrelated", datetime(2026, 11, 18, 8, tzinfo=timezone.utc), tags=[TAG + "_other"])
        update_book(self.book, [a, b, pending, unrelated], now=self.now)
        daily = self.book["Plan diario"]
        self.assertEqual(daily["J2"].value, 11.8)
        self.assertEqual(daily["P2"].value, "a, b")
        self.assertEqual(self.book["Plan semanal"]["H2"].value, 2)
        self.assertEqual(self.book["Suunto"].max_row, 4)
        self.assertIsNone(daily["J3"].value)
        self.assertEqual(daily["O3"].value, "FIT pendiente")
        self.assertEqual(daily["O4"].value, "Sin actividad etiquetada")
        self.assertEqual(daily["O6"].value, "Descanso sin confirmar")
        daily["T6"] = True
        update_book(self.book, [a, b, pending, unrelated], now=self.now)
        self.assertEqual(daily["O6"].value, "Descanso cumplido")
        self.assertEqual(self.book["Suunto"].max_row, 4)

    def test_rejects_template_as_active(self):
        base = build_book(self.draft)
        with self.assertRaises(ValueError):
            update_book(base, [], now=self.now)

    def test_key_sessions_have_dose_ranges(self):
        daily = self.book["Plan diario"]
        first = [r for r in daily.iter_rows(min_row=2, max_row=8, values_only=True) if r[20] is not None]
        self.assertEqual(len(first), 3)
        structured = [row for row in first if row[5] in ("speed", "sustained")]
        self.assertTrue(all(row[21] and row[22] and row[23] for row in structured))
        expected = [sum(session.role in ("speed", "sustained", "long", "race")
                        for session in self.draft.sessions[week * 7:(week + 1) * 7])
                    for week in range(12)]
        self.assertEqual([self.book["Plan semanal"].cell(row, 12).value
                          for row in range(2, 14)], expected)

    def test_missing_metrics_do_not_become_partial_daily_totals(self):
        complete = workout("complete", datetime(2026, 11, 16, 7, tzinfo=timezone.utc))
        partial = workout("partial", datetime(2026, 11, 16, 18, tzinfo=timezone.utc))
        partial["distance_meters"] = None
        partial["training_stress_score"] = None
        update_book(self.book, [complete, partial], now=self.now)
        daily = self.book["Plan diario"]
        self.assertIsNone(daily["J2"].value)
        self.assertIsNone(daily["N2"].value)
        self.assertEqual(daily["O2"].value, "Métricas incompletas")
        self.assertEqual(self.book["Suunto"].max_row, 3)


if __name__ == "__main__":
    unittest.main()
