"""Synthetic daily matching cases; no private Suunto records are used."""

import unittest
from datetime import date, datetime, timezone

from plan_daily_sync import update_book
from plan_excel import make_book


START = date(2026, 11, 16)
TAG = "kore_10k_20261116"


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
        self.book = make_book("10k", START, active=True, no_race=True)
        self.now = datetime(2026, 11, 20, 12, tzinfo=timezone.utc)

    def test_labeled_monday_updates_same_row_and_is_idempotent(self):
        # 23:30 UTC on Sunday is 00:30 Monday in Europe/Madrid.
        monday = workout("run-1", datetime(2026, 11, 15, 23, 30, tzinfo=timezone.utc))
        ignored = workout("no-tag", datetime(2026, 11, 16, 8, tzinfo=timezone.utc), tags=[])
        update_book(self.book, [monday, monday, ignored], now=self.now)
        row = self.book["Plan diario"][2]
        self.assertEqual((row[0].value, row[3].value, row[4].value, row[6].value),
                         (1, "lunes", "Recovery", 10))
        self.assertEqual(row[9].value, 9.8)
        self.assertEqual(row[15].value, "run-1")
        self.assertEqual(self.book["Suunto"].max_row, 2)
        self.assertEqual(self.book["Plan semanal"]["E2"].value, 74)
        self.assertEqual(self.book["Plan semanal"]["F2"].value, 9.8)
        update_book(self.book, [monday, ignored], now=self.now)
        self.assertEqual(self.book["Suunto"].max_row, 2)
        self.assertEqual((row[6].value, row[9].value), (10, 9.8))
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
        base = make_book("10k", START)
        with self.assertRaises(ValueError):
            update_book(base, [], now=self.now)

    def test_three_progressive_key_sessions_with_dose_ranges(self):
        daily = self.book["Plan diario"]
        first = [r for r in daily.iter_rows(min_row=2, max_row=8, values_only=True) if r[20] is not None]
        self.assertEqual(len(first), 3)
        self.assertEqual(first[0][4], "Series 1 min")
        week_seven = [r for r in daily.iter_rows(min_row=44, max_row=50, values_only=True) if r[20] is not None]
        self.assertEqual([r[4] for r in week_seven], ["Series 1 km", "Progresivo", "Tirada + 10k"])
        self.assertEqual([r[22] for r in week_seven], ["6×1 km", "8 km", "3×2 km"])
        self.assertTrue(all(r[21] and r[23] for r in week_seven))
        self.assertTrue(all(self.book["Plan semanal"].cell(n, 12).value == 3 for n in range(2, 14)))

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
