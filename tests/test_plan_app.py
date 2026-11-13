"""Local form generates a downloadable V2 draft without activating it."""

import unittest
from datetime import date, datetime, timezone
from io import BytesIO
from unittest.mock import patch

from fastapi.testclient import TestClient
from openpyxl import load_workbook

from plan_app import app
from training_level import LevelSuggestion, TrainingAssessment


def assessment():
    limitations = ("editable",)
    return TrainingAssessment(
        datetime(2026, 11, 13, 12, tzinfo=timezone.utc), date(2026, 8, 22),
        date(2026, 11, 13), 84, 100, 98, 80.5, 18.0, 22.0,
        date(2026, 11, 12),
        LevelSuggestion("speed", 2, 3, 2, 4, "medium", ("evidencia velocidad",), limitations),
        LevelSuggestion("sustained", 1, 3, 1, 2, "medium", ("evidencia sostenido",), limitations),
    )


class PlanAppTest(unittest.TestCase):
    def test_home_prefills_editable_suunto_suggestions(self):
        client = TestClient(app)
        with patch("plan_app.current_assessment", return_value=assessment()):
            response = client.get("/")
        self.assertEqual(response.status_code, 200)
        self.assertIn("value='80.5'", response.text)
        self.assertIn("name='speed_entry_level' type='number' value='2'", response.text)
        self.assertIn("evidencia velocidad", response.text)

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
            "suggested_speed_level": "2", "suggested_sustained_level": "1",
            "speed_suggestion_confidence": "medium",
            "sustained_suggestion_confidence": "medium",
            "speed_evidence_summary": "evidencia velocidad",
            "sustained_evidence_summary": "evidencia sostenido",
            "assessment_as_of": "2026-11-13", "assessment_window_days": "84",
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
        self.assertEqual(book["Configuración"]["B20"].value, 2)
        self.assertEqual(book["Configuración"]["B22"].value, 1)
        self.assertEqual(book["Reglas y decisiones"]["B21"].value, "evidencia velocidad")
        self.assertEqual(book["Plan diario"].max_row, 57)
        book.close()


if __name__ == "__main__":
    unittest.main()
