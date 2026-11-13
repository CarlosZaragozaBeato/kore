import unittest
from datetime import date, datetime, timezone

from analysis_training import weekly_rows


class AnalysisTrainingTest(unittest.TestCase):
    def test_weekly_analysis_keeps_partial_weeks_out_of_completed_average(self):
        workouts = [
            {"start_time_ms": 1794214800000, "record_count": 100,
             "sport": "running", "distance_meters": 10000,
             "timer_seconds": 3600, "training_stress_score": 50},
            {"start_time_ms": 1794819600000, "record_count": 100,
             "sport": "running", "distance_meters": 20000,
             "timer_seconds": 7200, "training_stress_score": None},
        ]
        rows = weekly_rows(date(2026, 11, 9), date(2026, 11, 17), workouts)
        self.assertEqual([(row["complete"], row["km"]) for row in rows],
                         [(True, 10), (False, 20)])
        self.assertEqual(rows[0]["tss_runs"], 1)
        self.assertEqual(rows[1]["tss_runs"], 0)
        self.assertIsNone(rows[1]["average_4w_km"])


if __name__ == "__main__":
    unittest.main()
