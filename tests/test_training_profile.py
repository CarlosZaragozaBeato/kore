import unittest
from datetime import date, datetime, timezone

from analysis_training import weekly_rows
from training_profile import compute_profile, period_summary


def workout(day, km=10, *, sport="running", fit=True, tss=None):
    return {
        "start_time_ms": int(datetime(day.year, day.month, day.day, 9,
                                      tzinfo=timezone.utc).timestamp() * 1000),
        "sport": sport, "record_count": 100 if fit else None,
        "distance_meters": km * 1000, "timer_seconds": km * 360,
        "training_stress_score": tss, "ascent_meters": 100,
        "avg_heart_rate": 140,
    }


class TrainingProfileTest(unittest.TestCase):
    def test_period_reports_missing_fit_without_counting_it_as_zero_effort(self):
        rows = [workout(date(2026, 11, 9), tss=45),
                workout(date(2026, 11, 10), fit=False),
                workout(date(2026, 11, 11), sport="cycling")]
        summary = period_summary(rows, date(2026, 11, 9), date(2026, 11, 11))
        self.assertEqual(summary["running_sessions"], 1)
        self.assertEqual(summary["running_km"], 10)
        self.assertEqual(summary["workouts_without_current_fit"], 1)
        self.assertEqual(summary["other_sport_sessions"], 1)
        self.assertEqual(summary["tss_coverage"], 1)

    def test_profile_excludes_current_week_and_keeps_subjective_status(self):
        rows = [workout(date(2026, 11, 9), tss=45),
                workout(date(2026, 11, 16), km=50)]
        weeks = weekly_rows(date(2026, 11, 9), date(2026, 11, 17), rows)
        subjective = {"reported_on": "2026-11-17", "fatigue": "más cansado",
                      "limitations": "ninguna comunicada"}
        profile = compute_profile(date(2026, 11, 9), date(2026, 11, 17), rows,
                                  weeks, {"5k": 80}, None, subjective)
        self.assertEqual(profile["through_date"], "2026-11-16")
        self.assertEqual(profile["completed_calendar_weeks"], 1)
        self.assertEqual(profile["recent_12_week_peak_km"], 10)
        self.assertEqual(profile["subjective_status"], subjective)


if __name__ == "__main__":
    unittest.main()
