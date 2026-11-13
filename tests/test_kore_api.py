import unittest
from datetime import date, datetime, timedelta, timezone

from fastapi.testclient import TestClient

from kore_api.app import DataAvailability, app, calendar_bounds, completed_window_bounds


class KoreApiContractTest(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)

    def test_query_bounds_are_validated_before_database_access(self):
        self.assertEqual(self.client.get("/api/v1/workouts?limit=0").status_code, 422)
        self.assertEqual(self.client.get("/api/v1/workouts?offset=-1").status_code, 422)
        self.assertEqual(self.client.get("/api/v1/agent/context?days=366").status_code, 422)
        self.assertEqual(self.client.get("/api/v1/agent/next-session/evidence?window_days=0").status_code, 422)
        self.assertEqual(self.client.get("/api/v1/agent/next-session/evidence?target_sport=").status_code, 422)

    def test_openapi_exposes_only_read_endpoints_and_no_raw_payload(self):
        spec = self.client.get("/openapi.json").json()
        self.assertEqual(len(spec["paths"]), 5)
        self.assertTrue(all(set(methods) == {"get"} for methods in spec["paths"].values()))
        self.assertNotIn("raw_payload", str(spec))
        self.assertNotIn("position_lat", str(spec))

    def test_madrid_calendar_bounds_handle_spring_clock_change(self):
        first, today, start, end = calendar_bounds(1, datetime(2026, 3, 29, 12, tzinfo=timezone.utc))
        self.assertEqual((first, today), (date(2026, 3, 29), date(2026, 3, 29)))
        self.assertEqual(end.astimezone(timezone.utc) - start.astimezone(timezone.utc), timedelta(hours=23))

    def test_completed_comparison_windows_exclude_today_across_clock_change(self):
        today, previous_first, recent_first, _, recent_start, end = completed_window_bounds(
            7, datetime(2026, 3, 31, 12, tzinfo=timezone.utc)
        )
        self.assertEqual((previous_first, recent_first, today),
                         (date(2026, 3, 17), date(2026, 3, 24), date(2026, 3, 31)))
        self.assertEqual(end.astimezone(timezone.utc) - recent_start.astimezone(timezone.utc),
                         timedelta(hours=167))

    def test_availability_allows_database_without_pipeline_history(self):
        availability = DataAvailability.model_validate({
            "as_of": datetime(2026, 11, 13, tzinfo=timezone.utc),
            "latest_summary_at": None, "latest_decoded_at": None,
            "pending_fit": 0, "unavailable_fit": 0, "pending_decode": 0,
        })
        self.assertIsNone(availability.latest_ingestion_status)
        self.assertIsNone(availability.last_successful_ingestion_at)


if __name__ == "__main__":
    unittest.main()
