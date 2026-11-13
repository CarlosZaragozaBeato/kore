"""Catalog validation and deterministic eligibility rules."""

import unittest

from pydantic import ValidationError

from session_catalog import SessionCatalog, default_catalog


class SessionCatalogTest(unittest.TestCase):
    def test_default_catalog_has_contiguous_ladders_and_draft_status(self):
        catalog = default_catalog()
        self.assertEqual(len(catalog.sessions), 13)
        self.assertEqual([session.level for session in catalog.sessions
                          if session.progression_group == "speed"], [0, 1, 2, 3])
        self.assertTrue(all(session.review_status == "draft" for session in catalog.sessions))

    def test_selection_regresses_to_eligible_level_and_keeps_identity(self):
        resource = default_catalog().select_progression(
            "sustained", requested_level=3, focus_distance="5k",
            objective="development", phase="desarrollo"
        )
        self.assertEqual((resource.id, resource.level), ("sustained_10min", 1))
        self.assertEqual(resource.dose.target, "3×10 min")

    def test_duplicate_ids_and_review_without_sources_are_rejected(self):
        source = default_catalog().model_dump(mode="json")
        source["sessions"].append(source["sessions"][0].copy())
        with self.assertRaisesRegex(ValidationError, "unique"):
            SessionCatalog.model_validate(source)
        source = default_catalog().model_dump(mode="json")
        source["sessions"][0]["review_status"] = "reviewed"
        with self.assertRaisesRegex(ValidationError, "source_refs"):
            SessionCatalog.model_validate(source)


if __name__ == "__main__":
    unittest.main()
