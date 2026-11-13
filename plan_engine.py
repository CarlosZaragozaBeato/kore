"""Deterministic, parameter-driven drafts selected from a versioned catalog."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import date, timedelta
from math import ceil
from typing import Literal

from pydantic import BaseModel, Field, model_validator

from session_catalog import SessionCatalog, SessionResource, default_catalog


Distance = Literal["5k", "10k", "21k", "42k"]
DISTANCE_KM = {"5k": 5.0, "10k": 10.0, "21k": 21.1, "42k": 42.2}
WEEKDAYS = ("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")


class PlanInputs(BaseModel):
    start_date: date
    focus_distance: Distance
    ultimate_distance: Distance
    objective: Literal["race", "development"]
    weeks: int = Field(ge=2, le=40)
    event_date: date | None = None
    bottleneck: Literal["speed", "sustained", "balanced"]
    current_weekly_km: float = Field(gt=0, le=250)
    target_weekly_km: float = Field(gt=0, le=250)
    recent_long_run_km: float = Field(gt=0, le=50)
    training_days: list[int] = Field(min_length=2, max_length=7)
    long_run_day: int = Field(ge=0, le=6)
    quality_sessions_per_week: int = Field(default=2, ge=0, le=2)
    speed_entry_level: int = Field(ge=0, le=3)
    sustained_entry_level: int = Field(ge=0, le=3)
    max_weekly_increase_km: float = Field(ge=0, le=30)
    max_long_run_increase_km: float = Field(ge=0, le=10)
    deload_every: int = Field(ge=2, le=8)
    deload_fraction: float = Field(ge=0.5, le=1.0)
    fatigue: Literal["normal", "elevated"]

    @model_validator(mode="after")
    def validate_calendar(self):
        if self.start_date.weekday() != 0:
            raise ValueError("start_date must be a Monday")
        if len(set(self.training_days)) != len(self.training_days) or any(day not in range(7) for day in self.training_days):
            raise ValueError("training_days must be unique weekday numbers 0–6")
        if self.long_run_day not in self.training_days:
            raise ValueError("long_run_day must be one of training_days")
        if self.objective == "race":
            if self.event_date is None:
                raise ValueError("race objective requires event_date")
            inferred_weeks = (self.event_date - self.start_date).days // 7 + 1
            if not 2 <= inferred_weeks <= 40:
                raise ValueError("event_date must be 2–40 weeks after the start")
            self.weeks = inferred_weeks
            last_week_start = self.start_date + timedelta(weeks=self.weeks - 1)
            if not last_week_start <= self.event_date <= last_week_start + timedelta(days=6):
                raise ValueError("event_date must fall in the final plan week")
            if self.event_date.weekday() not in self.training_days:
                raise ValueError("event_date must be one of training_days")
        elif self.event_date is not None:
            raise ValueError("development objective must not have event_date")
        return self


@dataclass(frozen=True)
class Session:
    day: date
    week: int
    phase: str
    role: str
    name: str
    km: float
    description: str
    dose_min: str | None = None
    dose_target: str | None = None
    dose_max: str | None = None
    template_id: str | None = None
    selection_reason: str | None = None


@dataclass(frozen=True)
class PlanDraft:
    inputs: PlanInputs
    sessions: tuple[Session, ...]
    weekly_km: tuple[float, ...]
    notes: tuple[str, ...]


def _phase(inputs: PlanInputs, week: int) -> str:
    if inputs.objective == "race" and week == inputs.weeks:
        return "competición"
    if inputs.objective == "race" and week == inputs.weeks - 1:
        return "afinación"
    if week % inputs.deload_every == 0:
        return "descarga"
    if week <= max(1, ceil(inputs.weeks * 0.25)):
        return "base"
    return "desarrollo" if inputs.objective == "development" else "específico"


def _quality_days(inputs: PlanInputs) -> tuple[int, ...]:
    days = sorted(inputs.training_days)
    if len(days) < 4 or inputs.quality_sessions_per_week == 0:
        return ()
    candidates = [day for day in days if day != inputs.long_run_day
                  and min((day - inputs.long_run_day) % 7,
                          (inputs.long_run_day - day) % 7) >= 2]
    if not candidates:
        return ()
    first = candidates[0]
    second = next((day for day in candidates[1:] if min((day - first) % 7,
                    (first - day) % 7) >= 2), None)
    return (first, second) if inputs.quality_sessions_per_week == 2 and len(days) >= 5 and second is not None else (first,)


def _recipe(catalog: SessionCatalog, inputs: PlanInputs, kind: str, level: int,
            week: int, phase: str) -> tuple[SessionResource, int]:
    progression = (week - 1) // 3 if phase not in ("descarga", "afinación", "competición") else 0
    requested_level = min(3, level + progression)
    return catalog.select_progression(kind, requested_level=requested_level,
                                      focus_distance=inputs.focus_distance,
                                      objective=inputs.objective, phase=phase), requested_level


def _allocate(total: float, days: list[int], long_day: int, long_km: float,
              quality_days: tuple[int, ...]) -> dict[int, float]:
    remaining = total - long_km
    weights = {day: (1.15 if day in quality_days else 0.8 if day == (long_day + 1) % 7 else 1.0)
               for day in days if day != long_day}
    weight_sum = sum(weights.values())
    allocation = {day: round(remaining * weight / weight_sum, 1) for day, weight in weights.items()}
    allocation[long_day] = round(long_km, 1)
    last = next(day for day in reversed(days) if day != long_day)
    allocation[last] = round(allocation[last] + round(total - sum(allocation.values()), 1), 1)
    return allocation


def generate(inputs: PlanInputs, catalog: SessionCatalog | None = None) -> PlanDraft:
    catalog = catalog or default_catalog()
    notes = []
    quality_days = _quality_days(inputs)
    if not quality_days:
        notes.append("Con estos días no se programa calidad: revisar distribución si se desea incluirla.")
    if inputs.fatigue == "elevated":
        notes.append("Fatiga elevada declarada: primera semana con menor volumen y sin sesiones de calidad.")
    if inputs.ultimate_distance != inputs.focus_distance:
        notes.append(f"Bloque de {inputs.focus_distance} al servicio del objetivo final {inputs.ultimate_distance}; conservar la tirada larga.")
    if inputs.objective == "development":
        notes.append("Sin competición fijada: la última semana no presupone una carrera.")
    notes.append("El catálogo propone dosis; ritmos y progresión requieren revisión con resultados y sensaciones reales.")

    sessions: list[Session] = []
    volumes: list[float] = []
    baseline = inputs.current_weekly_km
    long_baseline = inputs.recent_long_run_km
    for week in range(1, inputs.weeks + 1):
        phase = _phase(inputs, week)
        if week == 1:
            planned = min(baseline, inputs.target_weekly_km) * (0.8 if inputs.fatigue == "elevated" else 1.0)
        elif phase == "descarga":
            planned = volumes[-1] * inputs.deload_fraction
        elif phase == "afinación":
            planned = volumes[-1] * 0.8
        elif phase == "competición":
            planned = volumes[-1] * 0.7
        else:
            planned = min(inputs.target_weekly_km, max(baseline, volumes[-1]) + inputs.max_weekly_increase_km)
        event_week = phase == "competición"
        race_km = DISTANCE_KM[inputs.focus_distance]
        if event_week:
            planned = max(planned, race_km)
        planned = round(planned, 1)
        volumes.append(planned)
        long_day = inputs.event_date.weekday() if event_week else inputs.long_run_day
        if event_week:
            long_km = race_km
        else:
            long_share = {"5k": 0.25, "10k": 0.30, "21k": 0.34, "42k": 0.38}[inputs.focus_distance]
            if inputs.ultimate_distance == "42k":
                long_share = max(long_share, 0.35)
            long_baseline = min(long_baseline + (inputs.max_long_run_increase_km if week > 1 else 0),
                                planned * long_share)
            long_km = round(long_baseline, 1)
        if planned - long_km < len(inputs.training_days) - 1:
            raise ValueError("Weekly volume leaves less than 1 km for another training day; adjust inputs")
        week_start = inputs.start_date + timedelta(weeks=week - 1)
        allocation = _allocate(planned, sorted(inputs.training_days), long_day, long_km, quality_days)
        if any(km < 1 for km in allocation.values()):
            raise ValueError("Selected volume is too small for the chosen training days")
        for day_index in range(7):
            when = week_start + timedelta(days=day_index)
            km = allocation.get(day_index, 0.0)
            if not km:
                resource = catalog.select("rest", focus_distance=inputs.focus_distance,
                                          objective=inputs.objective, phase=phase)
                session = Session(when, week, phase, "rest", resource.name, 0,
                                  resource.description(), template_id=resource.id,
                                  selection_reason="Día no seleccionado para entrenar.")
            elif event_week and when == inputs.event_date:
                resource = catalog.select("race", focus_distance=inputs.focus_distance,
                                          objective=inputs.objective, phase=phase)
                session = Session(when, week, phase, "race", resource.name, km,
                                  resource.description(), template_id=resource.id,
                                  selection_reason="Fecha de competición confirmada en la última semana.")
            elif day_index == long_day:
                resource = catalog.select("long", focus_distance=inputs.focus_distance,
                                          objective=inputs.objective, phase=phase)
                session = Session(when, week, phase, "long", resource.name, km,
                                  resource.description(), template_id=resource.id,
                                  selection_reason=(f"Día de tirada larga elegido; {km:g} km limitados por "
                                                    f"volumen, distancia {inputs.focus_distance} y objetivo final {inputs.ultimate_distance}."))
            elif day_index in quality_days and phase not in ("descarga", "afinación", "competición") and not (week == 1 and inputs.fatigue == "elevated"):
                rank = quality_days.index(day_index)
                if inputs.bottleneck == "balanced":
                    shorter_focus = inputs.focus_distance in ("5k", "10k")
                    main = "speed" if (week % 2 == 1) == shorter_focus else "sustained"
                else:
                    main = "speed" if inputs.bottleneck == "speed" else "sustained"
                kind = main if rank == 0 else ("sustained" if main == "speed" else "speed")
                level = inputs.speed_entry_level if kind == "speed" else inputs.sustained_entry_level
                resource, requested_level = _recipe(catalog, inputs, kind, level, week, phase)
                dose = resource.dose
                session = Session(when, week, phase, kind, resource.name, km,
                                  f"{resource.description()} Dosis prevista: {dose.target}. "
                                  "El trabajo está incluido en los km diarios.",
                                  dose.minimum, dose.target, dose.maximum, resource.id,
                                  (f"Necesidad {inputs.bottleneck}; sesión estructurada {rank + 1} de "
                                   f"{len(quality_days)}; nivel solicitado {requested_level}, "
                                   f"recurso elegible {resource.level} para {inputs.focus_distance}/{phase}."))
            else:
                recovery = day_index == (long_day + 1) % 7
                role = "recovery" if recovery else "easy"
                resource = catalog.select(role, focus_distance=inputs.focus_distance,
                                          objective=inputs.objective, phase=phase)
                session = Session(when, week, phase, role, resource.name, km,
                                  resource.description(), template_id=resource.id,
                                  selection_reason=("Día posterior a la tirada larga."
                                                    if recovery else "Día de apoyo para completar el volumen elegido."))
            sessions.append(session)
    if volumes[-1] < inputs.target_weekly_km and inputs.objective == "development":
        notes.append("El volumen final queda bajo el objetivo solicitado con el límite de incremento elegido.")
    return PlanDraft(inputs, tuple(sessions), tuple(volumes), tuple(notes))
