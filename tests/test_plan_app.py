"""Local form generates a downloadable V2 draft without activating it."""

import unittest
from io import BytesIO

from fastapi.testclient import TestClient
from openpyxl import load_workbook

from plan_app import app


class PlanAppTest(unittest.TestCase):
    def test_preview_and_download_keep_plan_in_draft_state(self):
        client = TestClient(app)
        data = {
            "start_date": "2026-11-16", "focus_distance": "10k", "ultimate_distance": "42k",
            "objective": "development", "weeks": "8", "bottleneck": "speed",
            "current_weekly_km": "80", "target_weekly_km": "90",
            "recent_long_run_km": "18", "training_days": ["0", "1", "2", "3", "5", "6"],
            "long_run_day": "6", "speed_entry_level": "2", "sustained_entry_level": "1",
            "max_weekly_increase_km": "4", "max_long_run_increase_km": "1",
            "deload_every": "4", "deload_fraction": "0.8", "fatigue": "normal",
        }
        preview = client.post("/preview", data=data)
        self.assertEqual(preview.status_code, 200)
        self.assertIn("Series de 1 km", preview.text)
        path = preview.text.split("href='/download/", 1)[1].split("'", 1)[0]
        response = client.get("/download/" + path)
        self.assertEqual(response.status_code, 200)
        book = load_workbook(BytesIO(response.content), read_only=True)
        self.assertEqual(book["Configuración"]["B8"].value, "Borrador")
        self.assertIsNone(book["Configuración"]["B7"].value)
        self.assertEqual(book["Plan diario"].max_row, 57)
        book.close()


if __name__ == "__main__":
    unittest.main()
