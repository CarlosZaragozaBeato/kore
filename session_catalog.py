"""Validated versioned session resources for the deterministic plan engine."""

from __future__ import annotations

from functools import lru_cache
import argparse
import json
from pathlib import Path
from typing import Literal

from pydantic import BaseModel, Field, model_validator


ROOT = Path(__file__).resolve().parent
DEFAULT_CATALOG = ROOT / "resources" / "session_catalog.json"
Distance = Literal["5k", "10k", "21k", "42k"]
Objective = Literal["race", "development"]
Phase = Literal["base", "desarrollo", "específico", "descarga", "afinación", "competición"]
Role = Literal["rest", "recovery", "easy", "long", "speed", "sustained", "race"]


class Dose(BaseModel):
    minimum: str = Field(min_length=1)
    target: str = Field(min_length=1)
    maximum: str = Field(min_length=1)


class SessionResource(BaseModel):
    id: str = Field(pattern=r"^[a-z][a-z0-9_]*$")
    name: str = Field(min_length=1)
    role: Role
    compatible_focus_distances: list[Distance] = Field(min_length=1)
    objectives: list[Objective] = Field(min_length=1)
    phases: list[Phase] = Field(min_length=1)
    level: int | None = Field(default=None, ge=0, le=3)
    progression_group: Literal["speed", "sustained"] | None
    dose: Dose | None
    warmup: str | None
    main_set: str = Field(min_length=1)
    recovery: str | None
    cooldown: str | None
    intensity: str = Field(min_length=1)
    placement: str = Field(min_length=1)
    progression_id: str | None
    regression_id: str | None
    review_status: Literal["draft", "reviewed"] = "draft"
    source_refs: list[str] = Field(default_factory=list)

    @model_validator(mode="after")
    def validate_progression_fields(self):
        structured = self.role in ("speed", "sustained")
        if structured != (self.progression_group is not None and self.level is not None and self.dose is not None):
            raise ValueError("structured sessions require progression_group, level and dose")
        if structured and self.role != self.progression_group:
            raise ValueError("structured role and progression_group must match")
        if self.review_status == "reviewed" and not self.source_refs:
            raise ValueError("reviewed resources require source_refs")
        return self

    def description(self) -> str:
        parts = [part for part in (
            f"Calentar: {self.warmup}" if self.warmup else None,
            self.main_set,
            f"Recuperación: {self.recovery}" if self.recovery else None,
            f"Vuelta a la calma: {self.cooldown}" if self.cooldown else None,
        ) if part]
        return "; ".join(parts) + "."


class SessionCatalog(BaseModel):
    schema_version: Literal[1]
    catalog_version: str = Field(min_length=1)
    sessions: list[SessionResource] = Field(min_length=1)

    @model_validator(mode="after")
    def validate_graph(self):
        indexed = {session.id: session for session in self.sessions}
        if len(indexed) != len(self.sessions):
            raise ValueError("session ids must be unique")
        for session in self.sessions:
            for field, step in (("progression_id", 1), ("regression_id", -1)):
                linked_id = getattr(session, field)
                if linked_id is None:
                    continue
                linked = indexed.get(linked_id)
                if linked is None:
                    raise ValueError(f"unknown {field}: {linked_id}")
                if linked.progression_group != session.progression_group or linked.level != session.level + step:
                    raise ValueError(f"invalid {field} link for {session.id}")
        for group in ("speed", "sustained"):
            levels = sorted(session.level for session in self.sessions if session.progression_group == group)
            if levels != list(range(len(levels))):
                raise ValueError(f"{group} progression levels must be contiguous from zero")
        return self

    def get(self, resource_id: str) -> SessionResource:
        try:
            return next(session for session in self.sessions if session.id == resource_id)
        except StopIteration as exc:
            raise ValueError(f"unknown session resource: {resource_id}") from exc

    def select(self, role: Role, *, focus_distance: Distance, objective: Objective,
               phase: Phase, level: int | None = None) -> SessionResource:
        candidates = [session for session in self.sessions
                      if session.role == role
                      and focus_distance in session.compatible_focus_distances
                      and objective in session.objectives
                      and phase in session.phases
                      and (level is None or session.level == level)]
        if not candidates:
            raise ValueError(f"no catalog session for role={role}, distance={focus_distance}, phase={phase}, level={level}")
        if len(candidates) > 1:
            raise ValueError(f"ambiguous catalog selection for role={role}, distance={focus_distance}, phase={phase}, level={level}")
        return candidates[0]

    def select_progression(self, role: Literal["speed", "sustained"], *, requested_level: int,
                           focus_distance: Distance, objective: Objective,
                           phase: Phase) -> SessionResource:
        eligible = sorted((session for session in self.sessions
                           if session.role == role
                           and focus_distance in session.compatible_focus_distances
                           and objective in session.objectives
                           and phase in session.phases), key=lambda session: session.level)
        if not eligible:
            raise ValueError(f"no eligible {role} sessions for {focus_distance} in {phase}")
        return next((session for session in reversed(eligible)
                     if session.level <= requested_level), eligible[0])


def load_catalog(path: Path = DEFAULT_CATALOG) -> SessionCatalog:
    try:
        return SessionCatalog.model_validate(json.loads(path.read_text(encoding="utf-8")))
    except (OSError, ValueError, TypeError) as exc:
        raise ValueError(f"invalid session catalog: {path}") from exc


@lru_cache(maxsize=1)
def default_catalog() -> SessionCatalog:
    return load_catalog()


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate a Kore session catalog")
    parser.add_argument("path", nargs="?", type=Path, default=DEFAULT_CATALOG)
    args = parser.parse_args()
    try:
        catalog = load_catalog(args.path)
    except ValueError as exc:
        parser.error(str(exc))
    drafts = sum(session.review_status == "draft" for session in catalog.sessions)
    print(f"Catalog {catalog.catalog_version}: {len(catalog.sessions)} resources, {drafts} pending review")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
