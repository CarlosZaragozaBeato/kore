"""Check parameter choices and the daily Suunto workbook contract."""

import unittest
from datetime import date, datetime, timezone
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

from pydantic import ValidationError

from plan_builder import activate_draft, build_book
from plan_daily_sync import update_book, validate
from plan_engine import PlanInputs, generate


def inputs(**changes):
    values = dict(
        start_date=date(2026, 11, 16), focus_distance="10k", ultimate_distance="42k",
        objective="development", weeks=8, bottleneck="speed",
        current_weekly_km=80, target_weekly_km=90, recent_long_run_km=18,
        training_days=[0, 1, 2, 3, 5, 6], long_run_day=6,
        speed_entry_level=2, sustained_entry_level=1,
        max_weekly_increase_km=4, max_long_run_increase_km=1,
        deload_every=4, deload_fraction=.8, fatigue="normal",
    )
    values.update(changes)
    return PlanInputs(**values)


class PlanEngineTest(unittest.TestCase):
    def test_entry_level_and_bottleneck_change_the_first_stimulus(self):
        speed = generate(inputs())
        first_speed = next(s for s in speed.sessions[:7] if s.role in ("speed", "sustained"))
        self.assertEqual((first_speed.name, first_speed.dose_target), ("Series de 1 km", "5×1 km"))
        endurance = generate(inputs(bottleneck="sustained", speed_entry_level=0))
        first_endurance = next(s for s in endurance.sessions[:7] if s.role in ("speed", "sustained"))
        self.assertEqual((first_endurance.name, first_endurance.dose_target), ("Sostenido medio", "3×10 min"))

    def test_days_volume_and_deload_follow_inputs(self):
        draft = generate(inputs(training_days=[1, 3, 5, 6], target_weekly_km=88))
        self.assertEqual(len(draft.sessions), 56)
        self.assertEqual(draft.weekly_km[:4], (80, 84, 88, 70.4))
        self.assertEqual(draft.weekly_km[4:6], (88, 88))
        for week in range(8):
            days = draft.sessions[week * 7:(week + 1) * 7]
            self.assertEqual({s.day.weekday() for s in days if s.km}, {1, 3, 5, 6})
            self.assertAlmostEqual(sum(s.km for s in days), draft.weekly_km[week])

    def test_focus_distance_and_quality_count_change_the_structure(self):
        short = generate(inputs(focus_distance="5k", ultimate_distance="5k", bottleneck="balanced"))
        long = generate(inputs(focus_distance="42k", ultimate_distance="42k", bottleneck="balanced"))
        short_first = next(s for s in short.sessions[:7] if s.role in ("speed", "sustained"))
        long_first = next(s for s in long.sessions[:7] if s.role in ("speed", "sustained"))
        self.assertEqual(short_first.role, "speed")
        self.assertEqual(long_first.role, "sustained")
        no_quality = generate(inputs(quality_sessions_per_week=0))
        self.assertFalse(any(s.role in ("speed", "sustained") for s in no_quality.sessions))

    def test_explicit_quality_days_and_focused_mix_are_respected(self):
        draft = generate(inputs(quality_days=[1, 3], quality_mix="focused",
                                bottleneck="speed"))
        quality = [session for session in draft.sessions[:7]
                   if session.role in ("speed", "sustained")]
        self.assertEqual([session.day.weekday() for session in quality], [1, 3])
        self.assertEqual([session.role for session in quality], ["speed", "speed"])
        with self.assertRaises(ValidationError):
            inputs(quality_days=[1, 2])

    def test_progression_cadence_changes_when_the_dose_advances(self):
        default = generate(inputs())
        faster = generate(inputs(dose_progression_every=2))
        default_week_three = next(session for session in default.sessions[14:21]
                                  if session.role == "speed")
        faster_week_three = next(session for session in faster.sessions[14:21]
                                 if session.role == "speed")
        self.assertEqual(default_week_three.name, "Series de 1 km")
        self.assertEqual(faster_week_three.name, "Series largas")

    def test_deload_reduces_long_run_and_target_caps_growth(self):
        draft = generate(inputs(recent_long_run_km=18, long_run_target_km=19,
                                deload_long_run_fraction=.75))
        long_runs = [session.km for session in draft.sessions if session.role == "long"]
        self.assertEqual(long_runs[:5], [18, 19, 19, 14.2, 19])
        self.assertTrue(any("referencia personal" in note.lower() for note in draft.notes))

    def test_volume_resumes_from_pre_deload_development_load(self):
        draft = generate(inputs(current_weekly_km=78, target_weekly_km=90,
                                max_weekly_increase_km=2))
        self.assertEqual(draft.weekly_km,
                         (78, 80, 82, 65.6, 84, 86, 88, 70.4))
        self.assertTrue(any("pico calculado de 88 km" in note for note in draft.notes))

    def test_supported_distances_bottlenecks_and_entry_levels_generate(self):
        for distance in ("5k", "10k", "21k", "42k"):
            for bottleneck in ("speed", "sustained", "balanced"):
                for level in range(4):
                    with self.subTest(distance=distance, bottleneck=bottleneck, level=level):
                        draft = generate(inputs(focus_distance=distance,
                                                ultimate_distance=distance,
                                                bottleneck=bottleneck,
                                                speed_entry_level=level,
                                                sustained_entry_level=level))
                        self.assertEqual(len(draft.sessions), 56)
                        self.assertTrue(all(session.template_id for session in draft.sessions))

    def test_race_date_must_be_in_final_week_and_on_training_day(self):
        with self.assertRaises(ValidationError):
            inputs(objective="race", event_date=date(2026, 11, 22))
        race = generate(inputs(objective="race", event_date=date(2027, 1, 10)))
        self.assertEqual(race.sessions[-1].role, "race")
        self.assertEqual(race.sessions[-1].km, 10)
        self.assertEqual(generate(inputs(objective="race", event_date=date(2027, 1, 24))).inputs.weeks, 10)

    def test_fatigue_reduces_first_week_and_defers_quality(self):
        draft = generate(inputs(fatigue="elevated"))
        self.assertEqual(draft.weekly_km[0], 64)
        self.assertFalse(any(s.role in ("speed", "sustained") for s in draft.sessions[:7]))

    def test_generated_active_book_accepts_suunto_daily_update(self):
        draft = generate(inputs())
        book = build_book(draft, active=True)
        self.assertEqual(validate(book)[2], "kore_10k_20261116")
        monday = draft.sessions[0].day
        workout = {
            "workout_key": "synthetic-1",
            "start_time_ms": int(datetime(2026, 11, 16, 8, tzinfo=timezone.utc).timestamp() * 1000),
            "user_tags": ["kore_10k_20261116"], "record_count": 100,
            "sport": "running", "distance_meters": 9800, "timer_seconds": 2940,
            "avg_heart_rate": 140, "training_stress_score": 45,
        }
        update_book(book, [workout], now=datetime(2026, 11, 17, tzinfo=timezone.utc))
        row = book["Plan diario"][2]
        self.assertEqual(row[2].value, monday)
        self.assertEqual(row[6].value, draft.sessions[0].km)
        self.assertEqual(row[9].value, 9.8)
        update_book(book, [workout], now=datetime(2026, 11, 17, tzinfo=timezone.utc))
        self.assertEqual(book["Suunto"].max_row, 2)
        decisions = book["Decisiones de sesión"]
        self.assertEqual(decisions.max_row, len(draft.sessions) + 1)
        self.assertEqual(decisions["C2"].value, draft.sessions[0].template_id)

    def test_activation_rejects_replacing_observed_plan(self):
        draft = generate(inputs())
        with TemporaryDirectory() as folder:
            path = Path(folder) / "active.xlsx"
            book = build_book(draft, active=True)
            book["Suunto"].append(("synthetic",))
            book.save(path)
            with patch("plan_builder.ACTIVE", path):
                with self.assertRaisesRegex(ValueError, "has Suunto results"):
                    activate_draft(draft)


if __name__ == "__main__":
    unittest.main()
