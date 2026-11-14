"""Versioned user planning references, separate from private training data."""

from __future__ import annotations

import json
from pathlib import Path


PATH = Path(__file__).resolve().parent / "resources" / "planning_profiles.json"


def load_profiles(path: Path = PATH) -> dict[str, float]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if payload.get("schema_version") != 1:
        raise ValueError("Unsupported planning profile schema")
    distances = payload.get("distances")
    if not isinstance(distances, dict) or set(distances) != {"5k", "10k", "21k", "42k"}:
        raise ValueError("Planning profiles must define 5k, 10k, 21k and 42k")
    profiles = {}
    for distance, values in distances.items():
        reference = values.get("reference_weekly_km") if isinstance(values, dict) else None
        if not isinstance(reference, (int, float)) or not 1 <= reference <= 250:
            raise ValueError(f"Invalid weekly reference for {distance}")
        profiles[distance] = float(reference)
    return profiles
