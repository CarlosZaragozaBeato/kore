"""Recoverable lifecycle operations for the single active training block."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime
import os
from pathlib import Path

from openpyxl import load_workbook

from plan_daily_sync import validate
from plan_workbook import ACTIVE, COMPLETED, DISCARDED, save_with_backup


@dataclass(frozen=True)
class ActiveBlock:
    tag: str
    start: date
    end: date
    state: str


@dataclass(frozen=True)
class LifecycleResult:
    action: str
    tag: str
    destination: Path


def inspect_active() -> ActiveBlock | None:
    if not ACTIVE.exists():
        return None
    book = load_workbook(ACTIVE, read_only=True, data_only=True)
    try:
        start, end, tag = validate(book)
        return ActiveBlock(tag, start, end, book["Configuración"]["B8"].value)
    finally:
        book.close()


def _destination(folder: Path, tag: str, now: datetime) -> Path:
    folder.mkdir(parents=True, exist_ok=True, mode=0o700)
    base = folder / f"{tag}-{now:%Y%m%dT%H%M%S}.xlsx"
    if not base.exists():
        return base
    for suffix in range(1, 1000):
        candidate = folder / f"{tag}-{now:%Y%m%dT%H%M%S}-{suffix}.xlsx"
        if not candidate.exists():
            return candidate
    raise FileExistsError("No se pudo crear un nombre único para el bloque")


def complete_active(now: datetime, *, reason: str,
                    allow_early: bool = False) -> LifecycleResult:
    """Copy a finalized workbook to completed and remove the active pointer."""
    if not ACTIVE.exists():
        raise FileNotFoundError("No hay un bloque activo")
    book = load_workbook(ACTIVE)
    try:
        _, end, tag = validate(book)
        if now.date() <= end and not allow_early:
            raise ValueError("El bloque todavía no ha superado su fecha final")
        config = book["Configuración"]
        config["B8"] = "Finalizado"
        config.append(("Finalizado el", now.replace(tzinfo=None)))
        config.cell(config.max_row, 2).number_format = "dd/mm/yyyy hh:mm"
        config.append(("Motivo de cierre", reason))
        destination = _destination(COMPLETED, tag, now)
        save_with_backup(book, destination)
    finally:
        book.close()
    ACTIVE.unlink()
    return LifecycleResult("completed", tag, destination)


def discard_active(now: datetime) -> LifecycleResult:
    """Atomically move, never delete, the current workbook to discarded."""
    active = inspect_active()
    if active is None:
        raise FileNotFoundError("No hay un bloque activo")
    destination = _destination(DISCARDED, active.tag, now)
    os.replace(ACTIVE, destination)
    os.chmod(destination, 0o600)
    return LifecycleResult("discarded", active.tag, destination)


def stored_books(folder: Path) -> tuple[Path, ...]:
    if not folder.exists():
        return ()
    return tuple(sorted(folder.glob("*.xlsx"), reverse=True))
