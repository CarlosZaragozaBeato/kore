import json
import tempfile
import unittest
from datetime import date, datetime, timezone
from pathlib import Path

from plan_excel import create_template, plan_tag, plan_weeks, read_targets, update_plan


class PlanExcelTest(unittest.TestCase):
    def test_template_uses_local_targets_without_prescribing_duration(self):
        with tempfile.TemporaryDirectory() as temp:
            config = Path(temp) / "config.json"
            config.write_text(json.dumps({"minimum_weekly_km": {
                "5k": 10, "10k": 20, "21k": 30, "42k": 40,
            }}))
            target = read_targets(config)["21k"]
            book = create_template("21k", target, date(2026, 11, 9), 2)
        self.assertEqual(book["Configuración"]["B3"].value, 30)
        self.assertEqual(book["Plan semanal"]["K2"].value, 30)
        self.assertEqual(book["Configuración"]["B7"].value, "kore_21k_20261109")
        self.assertIsNone(book["Plan semanal"]["F2"].value)
        self.assertEqual(len(plan_weeks(book)), 2)

    def test_update_keeps_plan_and_maps_suunto_to_blocks(self):
        book = create_template("5k", 10, date(2026, 11, 9), 2)
        plan = book["Plan semanal"]
        plan["D2"] = "Base"
        plan["F2"] = 12
        workouts = [
            {"workout_key": "a" * 24, "start_time_ms": 1794214800000,
             "fit_status": "ok", "sport": "running", "timer_seconds": 3600,
             "distance_meters": 10000, "avg_heart_rate": 145,
             "training_stress_score": 80, "record_count": 100},
            {"workout_key": "b" * 24, "start_time_ms": 1794301200000,
             "fit_status": None, "sport": None, "timer_seconds": None,
             "distance_meters": None, "avg_heart_rate": None,
             "training_stress_score": None, "record_count": None},
        ]
        update_plan(book, workouts, datetime(2026, 11, 17, 12, tzinfo=timezone.utc))
        self.assertEqual(plan["D2"].value, "Base")
        self.assertEqual(plan["I2"].value, 10)
        self.assertEqual(plan["J2"].value, 1)
        self.assertEqual(plan["L2"].value, -2)
        self.assertEqual(plan["M2"].value, "Completa")
        self.assertEqual(plan["N2"].value, 1)
        self.assertEqual(plan["M3"].value, "En curso")
        self.assertEqual(book["Suunto"]["B2"].value, "Base")
        self.assertEqual(book["Suunto"]["K3"].value, "FIT pendiente")

    def test_tag_matching_excludes_untagged_workouts(self):
        book = create_template("5k", 10, date(2026, 11, 9), 1)
        workout = {"workout_key": "a" * 24, "start_time_ms": 1794214800000,
                   "fit_status": "ok", "sport": "running", "timer_seconds": 3600,
                   "distance_meters": 10000, "avg_heart_rate": 145,
                   "training_stress_score": 80, "record_count": 100, "user_tags": ["otra"]}
        update_plan(book, [workout], datetime(2026, 11, 17, tzinfo=timezone.utc), matching="tag")
        self.assertIsNone(book["Plan semanal"]["I2"].value)
        self.assertEqual(book["Plan semanal"]["M2"].value, "Sin actividades etiquetadas")
        self.assertEqual(book["Suunto"].max_row, 1)
        workout["user_tags"].append(plan_tag("5k", date(2026, 11, 9)))
        update_plan(book, [workout], datetime(2026, 11, 17, tzinfo=timezone.utc), matching="tag")
        self.assertEqual(book["Plan semanal"]["I2"].value, 10)
        self.assertEqual(book["Suunto"].max_row, 2)


if __name__ == "__main__":
    unittest.main()
