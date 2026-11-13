"""Conservative level inference from synthetic Suunto sessions and laps."""

import unittest
from datetime import date, datetime, timedelta, timezone

from training_level import (
    ObservedLap, ObservedSession, assess_history, detect_structured_level,
    suggest_level,
)


BASE = datetime(2026, 11, 10, 8, tzinfo=timezone.utc)


def session(key, days=0, km=10, minutes=50):
    return ObservedSession(key, BASE + timedelta(days=days), minutes * 60, km * 1000)


def speed_1k_laps(key):
    laps = []
    index = 0
    for _ in range(3):
        laps.append(ObservedLap(key, index, 240, 1000)); index += 1
        laps.append(ObservedLap(key, index, 150, 400)); index += 1
    return laps


def sustained_10min_laps(key):
    laps = []
    index = 0
    for _ in range(2):
        laps.append(ObservedLap(key, index, 600, 2000)); index += 1
        laps.append(ObservedLap(key, index, 180, 400)); index += 1
    return laps


class TrainingLevelTest(unittest.TestCase):
    def test_auto_laps_do_not_look_like_structured_training(self):
        run = session("easy")
        laps = [ObservedLap("easy", index, 300, 1000) for index in range(10)]
        self.assertIsNone(detect_structured_level(run, laps, "speed"))
        self.assertIsNone(detect_structured_level(run, laps, "sustained"))

    def test_repeated_work_recovery_detects_exposure_but_one_session_regresses(self):
        sessions = [session("structured"), session("easy-1", 1), session("easy-2", 2)]
        suggestion = suggest_level(sessions, speed_1k_laps("structured"), "speed")
        self.assertEqual(suggestion.detected_structured_level, 2)
        self.assertEqual(suggestion.volume_supported_ceiling, 2)
        self.assertEqual(suggestion.suggested_level, 1)
        self.assertEqual(suggestion.confidence, "low")

    def test_two_sustained_sessions_support_medium_confidence(self):
        sessions = [session("s1", minutes=60), session("s2", 1, minutes=60),
                    session("easy", 2, minutes=60)]
        laps = sustained_10min_laps("s1") + sustained_10min_laps("s2")
        suggestion = suggest_level(sessions, laps, "sustained")
        self.assertEqual((suggestion.suggested_level, suggestion.detected_sessions,
                          suggestion.confidence), (1, 2, "medium"))

    def test_assessment_prefills_external_load_without_claiming_readiness(self):
        sessions = [session(f"run-{index}", index, km=10 + index) for index in range(4)]
        assessment = assess_history(
            sessions, [], generated_at=BASE, from_date=date(2026, 8, 19),
            through_date=date(2026, 11, 10), window_days=84, stored_workouts=5,
        )
        self.assertEqual(assessment.current_weekly_km, 11.5)
        self.assertEqual(assessment.recent_long_run_km, 13)
        self.assertEqual(assessment.stored_workouts, 5)
        self.assertEqual(assessment.speed.confidence, "low")
        self.assertIn("no una medida de capacidad fisiológica", assessment.speed.limitations[-1])


if __name__ == "__main__":
    unittest.main()
