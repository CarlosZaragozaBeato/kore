import tempfile
import unittest
from datetime import date, datetime, timezone
from pathlib import Path

from openpyxl import load_workbook

from export_excel import build_workbook, millisecond_bounds, output_path, save_private, select_dates


class ExportExcelTest(unittest.TestCase):
    def test_iso_week_and_inclusive_range(self):
        self.assertEqual(select_dates("2020-W53", None, None),
                         (date(2020, 12, 28), date(2021, 1, 3)))
        self.assertEqual(select_dates(None, "2026-11-10", "2026-11-12"),
                         (date(2026, 11, 10), date(2026, 11, 12)))
        self.assertEqual(select_dates(None, None, None, datetime(2026, 11, 13, tzinfo=timezone.utc)),
                         (date(2026, 11, 9), date(2026, 11, 15)))
        with self.assertRaises(ValueError):
            select_dates(None, "2026-11-12", "2026-11-10")

    def test_workbook_keeps_missing_fit_visible_and_zero_fills_days(self):
        first, last = date(2026, 11, 9), date(2026, 11, 11)
        workouts = [
            {"workout_key": "a" * 24, "start_time_ms": 1794214800000,
             "fit_status": "ok", "sport": "running", "timer_seconds": 3600,
             "distance_meters": 10000, "avg_heart_rate": 145, "max_heart_rate": 170,
             "avg_power": 250, "training_stress_score": 80, "record_count": 100,
             "user_tags": ["=1+1"]},
            {"workout_key": "b" * 24, "start_time_ms": 1794301200000,
             "fit_status": None, "sport": None, "timer_seconds": None,
             "distance_meters": None, "avg_heart_rate": None, "max_heart_rate": None,
             "avg_power": None, "training_stress_score": None, "record_count": None},
        ]
        book = build_workbook(first, last, workouts, [])
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "test.xlsx"
            save_private(book, path)
            restored = load_workbook(path, read_only=True, data_only=True)
            self.assertEqual(restored.sheetnames,
                             ["Resumen", "Entrenamientos", "Resumen diario", "Vueltas"])
            rows = list(restored["Entrenamientos"].values)
            self.assertEqual(rows[1][3:5], (60, 10))
            self.assertIsNone(rows[2][3])
            self.assertEqual(rows[2][9], "Pendiente de descarga")
            daily = list(restored["Resumen diario"].values)
            self.assertEqual([row[1] for row in daily[1:]], [1, 1, 0])
            self.assertEqual([row[6] for row in daily[1:]], [0, 1, 0])
            self.assertEqual(load_workbook(path)["Entrenamientos"]["M2"].data_type, "s")

    def test_date_bounds_follow_madrid_clock_change(self):
        first, end = millisecond_bounds(date(2026, 3, 29), date(2026, 3, 29))
        self.assertEqual(end - first, 23 * 60 * 60 * 1000)

    def test_export_path_must_stay_out_of_versioned_repo(self):
        with self.assertRaises(ValueError):
            output_path(date(2026, 11, 9), date(2026, 11, 15), Path("leak.xlsx"))


if __name__ == "__main__":
    unittest.main()
