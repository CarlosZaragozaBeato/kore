import unittest
from datetime import date, datetime, timedelta, timezone

from fastapi.testclient import TestClient

from kore_api.app import app, calendar_bounds


class KoreApiContractTest(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)

    def test_query_bounds_are_validated_before_database_access(self):
        self.assertEqual(self.client.get("/api/v1/workouts?limit=0").status_code, 422)
        self.assertEqual(self.client.get("/api/v1/workouts?offset=-1").status_code, 422)
        self.assertEqual(self.client.get("/api/v1/agent/context?days=366").status_code, 422)

    def test_openapi_exposes_only_read_endpoints_and_no_raw_payload(self):
        spec = self.client.get("/openapi.json").json()
        self.assertEqual(len(spec["paths"]), 4)
        self.assertTrue(all(set(methods) == {"get"} for methods in spec["paths"].values()))
        self.assertNotIn("raw_payload", str(spec))
        self.assertNotIn("position_lat", str(spec))

    def test_madrid_calendar_bounds_handle_spring_clock_change(self):
        first, today, start, end = calendar_bounds(1, datetime(2026, 3, 29, 12, tzinfo=timezone.utc))
        self.assertEqual((first, today), (date(2026, 3, 29), date(2026, 3, 29)))
        self.assertEqual(end.astimezone(timezone.utc) - start.astimezone(timezone.utc), timedelta(hours=23))


if __name__ == "__main__":
    unittest.main()
