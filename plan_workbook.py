"""Shared private workbook paths, schema and atomic persistence."""

from __future__ import annotations

from datetime import datetime
import os
from pathlib import Path
import secrets
import shutil
import tempfile

from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill


ROOT = Path(__file__).resolve().parent
PLANS = ROOT / "data" / "plans"
ACTIVE = PLANS / "active" / "plan-activo.xlsx"

DAILY_HEADERS = (
    "Semana", "Bloque", "Fecha", "Día", "Sesión planificada", "Tipo",
    "Km previstos", "Detalle de la sesión", "Ritmo/esfuerzo previsto",
    "Km reales", "Tiempo real (min)", "Ritmo real", "FC media", "TSS",
    "Estado", "Actividad(es) Suunto", "FIT pendientes", "Otros deportes",
    "Versión ritmos", "Descanso confirmado", "Sesión clave", "Dosis mínima",
    "Dosis prevista", "Dosis máxima",
)
WEEK_HEADERS = (
    "Semana", "Desde", "Hasta", "Bloque", "Km previstos", "Km reales",
    "Sesiones previstas", "Sesiones reales", "Estado", "FIT pendientes", "Nota",
    "Sesiones clave",
)
SUUNTO_HEADERS = (
    "Clave Suunto", "Fecha local", "Inicio local", "Deporte", "Km",
    "Tiempo (min)", "Ritmo", "FC media", "TSS", "FIT", "Etiquetas",
)


def style_table(sheet, widths: dict[str, int]) -> None:
    sheet.freeze_panes = "E2" if sheet.title == "Plan diario" else "A2"
    sheet.auto_filter.ref = sheet.dimensions
    for cell in sheet[1]:
        cell.fill = PatternFill("solid", fgColor="1F4E78")
        cell.font = Font(color="FFFFFF", bold=True)
        cell.alignment = Alignment(wrap_text=True, vertical="top")
    for column, width in widths.items():
        sheet.column_dimensions[column].width = width
    for row in sheet.iter_rows(min_row=2):
        for cell in row:
            cell.alignment = Alignment(wrap_text=True, vertical="top")


def save_with_backup(book: Workbook, path: Path = ACTIVE) -> Path | None:
    """Write privately and atomically, archiving any previous book first."""
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    fd, temporary = tempfile.mkstemp(prefix=".kore-plan-", suffix=".xlsx", dir=path.parent)
    os.close(fd)
    backup = None
    try:
        book.save(temporary)
        os.chmod(temporary, 0o600)
        if path.exists():
            archive = PLANS / "archive"
            archive.mkdir(parents=True, exist_ok=True, mode=0o700)
            backup = archive / f"{path.stem}-{datetime.now():%Y%m%dT%H%M%S}-{secrets.token_hex(4)}.xlsx"
            shutil.copy2(path, backup)
            os.chmod(backup, 0o600)
        os.replace(temporary, path)
    finally:
        Path(temporary).unlink(missing_ok=True)
    return backup
