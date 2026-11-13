"""Generate editable Kore training plans and activate one dated copy.

The default books are examples. Only an activated copy is intended for Suunto
matching. This module never writes observed training columns or the Suunto sheet.
"""

from __future__ import annotations

import argparse
from datetime import date, datetime, timedelta
import json
import os
from pathlib import Path
import secrets
import shutil
import tempfile

from openpyxl import Workbook, load_workbook
from openpyxl.styles import Alignment, Font, PatternFill


ROOT = Path(__file__).resolve().parent
PLANS = ROOT / "data" / "plans"
DEFAULTS = PLANS / "defaults"
ACTIVE = PLANS / "active" / "plan-activo.xlsx"
BASE_MONDAY = date(2026, 11, 16)
VO2 = None
PACE_VERSION = "2026-11-13-v1"

# (week distance, long run distance, block). Distances are proposals, not floors.
SPECS = {
    "5k": [
        (74, 15, "Adaptación"), (80, 16, "Base"), (82, 17, "Base"),
        (68, 14, "Descarga"), (82, 17, "Específico"), (84, 18, "Específico"),
        (86, 18, "Específico"), (70, 15, "Descarga"),
        (78, 16, "Afinación"), (46, 9, "Competición"),
    ],
    "10k": [
        (74, 15, "Adaptación"), (80, 16, "Base"), (82, 17, "Base"),
        (68, 14, "Descarga"), (82, 18, "Base"), (84, 18, "Específico"),
        (86, 19, "Específico"), (70, 15, "Descarga"),
        (86, 19, "Específico"), (84, 18, "Específico"),
        (76, 16, "Afinación"), (50, 14, "Competición"),
    ],
    "21k": [
        (76, 15, "Adaptación"), (80, 16, "Base"), (84, 17, "Base"),
        (70, 14, "Descarga"), (86, 18, "Base"), (88, 19, "Base"),
        (90, 20, "Específico"), (74, 16, "Descarga"),
        (90, 21, "Específico"), (92, 22, "Específico"),
        (94, 23, "Específico"), (78, 18, "Descarga"),
        (92, 23, "Específico"), (90, 21, "Específico"),
        (76, 18, "Afinación"), (57, 23, "Competición"),
    ],
    "42k": [
        (76, 15, "Adaptación"), (80, 16, "Base"), (84, 17, "Base"),
        (70, 14, "Descarga"), (86, 18, "Base"), (88, 19, "Base"),
        (90, 20, "Base"), (76, 16, "Descarga"),
        (92, 21, "Desarrollo"), (94, 22, "Desarrollo"),
        (96, 23, "Desarrollo"), (80, 18, "Descarga"),
        (98, 24, "Específico"), (100, 25, "Específico"),
        (102, 26, "Específico"), (84, 20, "Descarga"),
        (102, 28, "Específico"), (100, 29, "Específico"),
        (96, 26, "Específico"), (80, 21, "Afinación"),
        (66, 17, "Afinación"), (70, 44, "Competición"),
    ],
}
RACE_KM = {"5k": 5, "10k": 10, "21k": 21.1, "42k": 42.2}

# Fraction of self-reported VO2max used in a level-ground ACSM oxygen-cost model.
# These are broad illustrative effort bands, not physiological thresholds.
ZONES = [
    ("Recuperación", .58, .66, "20–50 min", "Muy cómodo; hablar sin esfuerzo"),
    ("Rodaje fácil", .65, .75, "40–90 min", "Conversación completa"),
    ("Tirada larga", .65, .76, "75–180 min", "Cómodo y sostenible; limitar por tiempo y fatiga"),
    ("Ritmo sostenido", .76, .83, "20–50 min", "Controlado; no confundir con carrera"),
    ("Umbral aproximado", .82, .90, "Bloques de 6–20 min", "Firme y controlado; calibrar con prueba"),
    ("Intervalos", .92, 1.00, "Repeticiones de 2–5 min", "Alto esfuerzo con recuperación al trote"),
    ("Repeticiones cortas", 1.00, 1.06, "Repeticiones de 15–60 s", "Técnica y rapidez; no perseguir ritmo GPS"),
]


def pace_text(vo2: float | None, low: float, high: float) -> str:
    if vo2 is None:
        return "Por calibrar"
    def fmt(fraction: float) -> str:
        seconds = round(60 * 200 / (vo2 * fraction - 3.5))
        return f"{seconds // 60}:{seconds % 60:02d}"
    return f"{fmt(high)}–{fmt(low)} min/km"


def key_sessions(modality: str, week: int, block: str):
    """Three structured stimuli. Min/target/max refer to work, not total km."""
    if modality == "10k":
        # Session, easy-to-read dose range, intensity, recovery. These are
        # alternatives within a controlled progression, not three max efforts.
        plans = [
            (("Series 1 min", "4×1 min", "6×1 min", "8×1 min", "Alegre, controlado", "1 min trote"),
             ("Umbral aeróbico", "2×6 min", "3×6 min", "3×8 min", "Controlado", "3 min trote"),
             ("Tirada + strides", "4×20 s", "6×20 s", "8×20 s", "Ágil sin sprint", "recuperación completa")),
            (("Series 1 min", "6×1 min", "8×1 min", "10×1 min", "Alegre, controlado", "1 min trote"),
             ("Umbral aeróbico", "2×8 min", "2×10 min", "3×10 min", "Controlado", "3 min trote"),
             ("Tirada + sostenido", "2×4 min", "3×4 min", "4×4 min", "Sostenido cómodo", "3 min fácil")),
            (("Series 4 min", "4×4 min", "5×4 min", "6×4 min", "Fuerte controlado", "2 min trote"),
             ("Umbral", "2×10 min", "3×8 min", "3×10 min", "Umbral aproximado", "3 min trote"),
             ("Tirada + sostenido", "2×5 min", "3×5 min", "4×5 min", "Sostenido cómodo", "4 min fácil")),
            (("Series 30 s", "4×30 s", "6×30 s", "8×30 s", "Ágil sin sprint", "90 s fácil"),
             ("Umbral breve", "1×8 min", "2×6 min", "2×8 min", "Controlado", "3 min fácil"),
             ("Tirada + strides", "4×20 s", "4×20 s", "6×20 s", "Ágil sin sprint", "recuperación completa")),
            (("Series 1 km", "3×1 km", "4×1 km", "5×1 km", "Fuerte controlado", "2–3 min trote"),
             ("Progresivo", "4 km", "6 km", "7 km", "De fácil a sostenido", "continuo"),
             ("Tirada + 10k", "2×1 km", "3×1 km", "4×1 km", "Esfuerzo 10k provisional", "3 min fácil")),
            (("Series 1 km", "4×1 km", "5×1 km", "6×1 km", "Fuerte controlado", "2–3 min trote"),
             ("Progresivo", "5 km", "7 km", "8 km", "De fácil a sostenido", "continuo"),
             ("Tirada + 10k", "1×2 km", "2×2 km", "3×2 km", "Esfuerzo 10k provisional", "4 min fácil")),
            (("Series 1 km", "4×1 km", "6×1 km", "7×1 km", "Fuerte controlado", "2–3 min trote"),
             ("Progresivo", "6 km", "8 km", "9 km", "De fácil a sostenido", "continuo"),
             ("Tirada + 10k", "2×2 km", "3×2 km", "3×2 km", "Esfuerzo 10k provisional", "4 min fácil")),
            (("Series 30 s", "4×30 s", "6×30 s", "8×30 s", "Ágil sin sprint", "90 s fácil"),
             ("Umbral breve", "1×8 min", "2×6 min", "2×8 min", "Controlado", "3 min fácil"),
             ("Tirada + strides", "4×20 s", "4×20 s", "6×20 s", "Ágil sin sprint", "recuperación completa")),
            (("Series 1 km", "4×1 km", "6×1 km", "7×1 km", "Fuerte controlado", "2–3 min trote"),
             ("Progresivo", "6 km", "8 km", "9 km", "De fácil a sostenido", "continuo"),
             ("Tirada + 10k", "2×2 km", "3×2 km", "3×2 km", "Esfuerzo 10k provisional", "4 min fácil")),
            (("Series 4 min", "4×4 min", "5×4 min", "6×4 min", "Fuerte controlado", "2 min trote"),
             ("Umbral", "2×12 min", "2×15 min", "3×12 min", "Umbral aproximado", "4 min trote"),
             ("Tirada + 10k", "2×2 km", "3×2 km", "3×2 km", "Esfuerzo 10k provisional", "4 min fácil")),
            (("Series 1 km", "3×1 km", "4×1 km", "5×1 km", "Fuerte controlado", "3 min trote"),
             ("Progresivo", "4 km", "5 km", "6 km", "De fácil a sostenido", "continuo"),
             ("Tirada + 10k", "1×2 km", "2×2 km", "2×2 km", "Esfuerzo 10k provisional", "4 min fácil")),
            (("Series 30 s", "4×30 s", "6×30 s", "6×30 s", "Ágil sin sprint", "90 s fácil"),
             ("Umbral breve", "1×5 min", "2×5 min", "2×6 min", "Controlado", "3 min fácil"),
             ("Competición", "10 km controlados", "10 km competición/contrarreloj", "10 km al máximo solo si recuperado", "Por calibrar", "evaluación")),
        ]
        return plans[week - 1]
    if block == "Competición":
        return (("Series 30 s", "4×30 s", "6×30 s", "6×30 s", "Ágil sin sprint", "90 s fácil"),
                ("Umbral breve", "1×5 min", "2×5 min", "2×6 min", "Controlado", "3 min fácil"),
                ("Competición", f"{RACE_KM[modality]:g} km", f"{RACE_KM[modality]:g} km", f"{RACE_KM[modality]:g} km", "Ritmo competición por validar", "evaluación"))
    light = block in ("Adaptación", "Descarga", "Afinación")
    if modality == "5k":
        if light:
            return (("Series 30 s", "4×30 s", "6×30 s", "8×30 s", "Ágil sin sprint", "90 s fácil"),
                    ("Umbral breve", "2×6 min", "2×8 min", "3×8 min", "Controlado", "3 min trote"),
                    ("Tirada + strides", "4×20 s", "6×20 s", "8×20 s", "Ágil sin sprint", "recuperación completa"))
        return (("Series 1 min/4 min", "6×1 min", "5×4 min", "6×4 min", "Fuerte controlado", "2 min trote"),
                ("Umbral", "2×8 min", "3×8 min", "3×10 min", "Umbral aproximado", "3 min trote"),
                ("Tirada + 5k", "4×1 min", "6×1 min", "8×1 min", "Esfuerzo 5k provisional", "2 min fácil"))
    if modality == "21k":
        if light:
            return (("Series 1 min", "4×1 min", "6×1 min", "8×1 min", "Alegre, controlado", "1 min trote"),
                    ("Umbral aeróbico", "2×6 min", "2×8 min", "3×8 min", "Controlado", "3 min trote"),
                    ("Tirada + strides", "4×20 s", "6×20 s", "8×20 s", "Ágil sin sprint", "recuperación completa"))
        return (("Series 4 min", "4×4 min", "5×4 min", "6×4 min", "Fuerte controlado", "2 min trote"),
                ("Umbral", "2×10 min", "3×10 min", "3×12 min", "Umbral aproximado", "3 min trote"),
                ("Tirada + medio maratón", "2×8 min", "3×10 min", "2×4 km", "Sostenido controlado", "5 min fácil"))
    if light:
        return (("Series 1 min", "4×1 min", "6×1 min", "8×1 min", "Alegre, controlado", "1 min trote"),
                ("Umbral aeróbico", "2×6 min", "2×8 min", "3×8 min", "Controlado", "3 min trote"),
                ("Tirada + strides", "4×20 s", "6×20 s", "8×20 s", "Ágil sin sprint", "recuperación completa"))
    return (("Series 4 min", "3×4 min", "4×4 min", "5×4 min", "Fuerte controlado", "2 min trote"),
            ("Umbral aeróbico", "2×12 min", "2×15 min", "3×15 min", "Controlado", "4 min trote"),
            ("Tirada + sostenido", "2×10 min", "3×10 min", "2×5 km", "Sostenido cómodo", "5 min fácil"))


def schedule(modality: str, week: int, total: int, long_km: int, block: str):
    race = block == "Competición"
    monday = (4 if modality == "42k" else 6) if race else 10
    tuesday = (4 if modality == "42k" else 6) if race else round(total * .18)
    thursday = (4 if modality == "42k" else 6) if race else round(total * .18)
    saturday = (4 if modality == "42k" else 5) if race else max(5, round(total * .10))
    wednesday = total - monday - tuesday - thursday - saturday - long_km
    if min(wednesday, saturday) < 4:
        raise ValueError((modality, week, total, long_km))
    recipes = key_sessions(modality, week, block)
    def key(day, km, recipe, index):
        name, minimum, target, maximum, effort, recovery = recipe
        detail = (f"Calentar 2–3 km + movilidad; {target} ({effort}); recuperación: {recovery}; "
                  f"enfriar 2 km; completar {km} km con carrera fácil. "
                  f"Rango de trabajo {minimum}–{maximum}; el máximo es un techo.")
        if day == "Domingo":
            detail = (f"Tirada de {km} km: empezar fácil; {target} ({effort}) dentro de la tirada, "
                      f"recuperación: {recovery}; acabar fácil. Rango {minimum}–{maximum}. "
                      "Reducir el estímulo si la tirada ya supone una carga alta.")
        return (day, name, km, detail, effort, minimum, target, maximum, index)
    return [
        ("Lunes", "Recovery", monday, "Muy fácil tras la tirada; acortar si hay fatiga", "Recuperación", None, None, None, None),
        key("Martes", tuesday, recipes[0], 1),
        ("Miércoles", "Rodaje + strides", wednesday, "Fácil; 4–6×20 s strides con recuperación completa solo si hay frescura", "Rodaje fácil", "4×20 s", "6×20 s", "6×20 s", None),
        key("Jueves", thursday, recipes[1], 2),
        ("Viernes", "Descanso", 0, "Descanso o movilidad suave", "—", None, None, None, None),
        ("Sábado", "Rodaje fácil", saturday, "Fácil y corto antes de la tirada", "Rodaje fácil", None, None, None, None),
        key("Domingo", long_km, recipes[2], 3),
    ]


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


def style_table(ws, widths: dict[str, int]):
    ws.freeze_panes = "E2" if ws.title == "Plan diario" else "A2"
    ws.auto_filter.ref = ws.dimensions
    for cell in ws[1]:
        cell.fill = PatternFill("solid", fgColor="1F4E78")
        cell.font = Font(color="FFFFFF", bold=True)
    for col, width in widths.items():
        ws.column_dimensions[col].width = width
    for row in ws.iter_rows(min_row=2):
        for cell in row:
            cell.alignment = Alignment(vertical="top", wrap_text=True)


def read_local_minimums() -> dict[str, float | None]:
    path = ROOT / "data" / "planning" / "config.json"
    if not path.exists():
        return {modality: None for modality in SPECS}
    try:
        values = json.loads(path.read_text(encoding="utf-8"))["minimum_weekly_km"]
    except (OSError, ValueError, KeyError, TypeError) as exc:
        raise ValueError("Invalid private planning config") from exc
    if set(values) != set(SPECS) or any(value is not None and
       (isinstance(value, bool) or not isinstance(value, (int, float)) or value < 0)
       for value in values.values()):
        raise ValueError("Private weekly targets must cover 5k, 10k, 21k and 42k")
    return values


def make_book(modality: str, monday: date, *, active: bool = False,
              no_race: bool = False, minimum_weekly_km: float | None = None) -> Workbook:
    if modality not in SPECS or monday.weekday() != 0:
        raise ValueError("Invalid modality or start date; start must be Monday")
    wb = Workbook()
    daily = wb.active
    daily.title = "Plan diario"
    daily.append(DAILY_HEADERS)
    weekly = wb.create_sheet("Plan semanal")
    weekly.append(WEEK_HEADERS)
    config = wb.create_sheet("Configuración")
    end = monday + timedelta(weeks=len(SPECS[modality]), days=-1)
    tag = f"kore_{modality}_{monday:%Y%m%d}" if active else None
    for label, value in (
        ("KORE_PLAN_DAILY_V2", None), ("Modalidad", modality),
        ("Km mínimo semanal declarado", minimum_weekly_km),
        ("Fecha de inicio", monday if active else None),
        ("Fecha final", end if active else None),
        ("Semanas del bloque", len(SPECS[modality])),
        ("Etiqueta Suunto exacta", tag),
        ("Estado del bloque", "Activo" if active else "Plantilla; fechas ilustrativas"),
        ("Identificador estable", tag),
        ("Día actual", None), ("Semana actual", None), ("Días restantes", None),
        ("VO₂max", VO2),
        ("Fecha de medición VO₂max", None),
        ("Fuente VO₂max", "Sin configurar"),
        ("Objetivo", "Evaluación sin carrera" if no_race else "Competición hipotética; confirmar"),
    ):
        config.append((label, value))
    for row in (4, 5): config.cell(row, 2).number_format = "dd/mm/yyyy"
    config.column_dimensions["A"].width = 34
    config.column_dimensions["B"].width = 65
    for i, (total, long_km, initial_block) in enumerate(SPECS[modality], start=1):
        block = "Evaluación" if no_race and initial_block == "Competición" else initial_block
        start = monday + timedelta(weeks=i - 1)
        rows = schedule(modality, i, total, long_km, initial_block)
        for offset, (day, kind, dist, description, zone, dose_min, dose_target, dose_max, key_index) in enumerate(rows):
            when = start + timedelta(days=offset)
            if block == "Evaluación" and kind == "Competición":
                kind = "Prueba opcional"
                description = f"Prueba controlada de {RACE_KM[modality]:g} km si hay frescura; si no, rodaje cómodo. Incluye calentamiento/enfriamiento."
                dose_min = "rodaje fácil"
                dose_target = f"{RACE_KM[modality]:g} km controlados opcionales"
                dose_max = "sin máximo; no forzar"
            pace = next((pace_text(VO2, lo, hi) for label, lo, hi, _, _ in ZONES if label == zone), zone)
            daily.append((i, block, when, day.lower(), kind, zone, dist,
                          description, pace if dist else "Libre", None, None,
                          None, None, None, "Pendiente" if dist else "Descanso sin confirmar",
                          None, None, None, PACE_VERSION, None, key_index,
                          dose_min, dose_target, dose_max))
            daily.cell(daily.max_row, 3).number_format = "dd/mm/yyyy"
        minimum_note = (f"Mínimo local configurado {minimum_weekly_km:g} km; "
                        if minimum_weekly_km is not None else "Sin mínimo local configurado; ")
        weekly.append((i, start, start + timedelta(days=6), block,
                       sum(row[2] for row in rows), None, sum(row[2] > 0 for row in rows),
                       None, "Pendiente", None,
                       minimum_note + "adaptación, descarga y evaluación pueden quedar por debajo.",
                       sum(row[8] is not None for row in rows)))
        for column in (2, 3): weekly.cell(weekly.max_row, column).number_format = "dd/mm/yyyy"
    suunto = wb.create_sheet("Suunto")
    suunto.append(SUUNTO_HEADERS)
    add_paces(wb)
    add_notes(wb, modality, active, no_race)
    style_table(daily, {"A": 10, "B": 17, "C": 16, "D": 13, "E": 24,
                        "F": 22, "G": 14, "H": 75, "I": 28, "J": 14,
                        "K": 18, "L": 16, "M": 13, "N": 12, "O": 27,
                        "P": 38, "Q": 16, "R": 25, "S": 20, "T": 20,
                        "U": 15, "V": 18, "W": 18, "X": 18})
    style_table(weekly, {"A": 12, "B": 16, "C": 16, "D": 18,
                         "E": 17, "F": 17, "G": 18, "H": 17,
                         "I": 27, "J": 17, "K": 80, "L": 18})
    style_table(suunto, {"A": 30, "B": 16, "C": 22, "D": 18, "E": 14,
                         "F": 19, "G": 15, "H": 14, "I": 12, "J": 19, "K": 45})
    return wb


def add_paces(wb: Workbook):
    ws = wb.create_sheet("Ritmos")
    ws.append(["VO₂max (editable)", VO2])
    ws.append(["Fecha y fuente", "Sin configurar"])
    ws.append(["Método", "ACSM, terreno llano: VO₂≈0,2×velocidad(m/min)+3,5; bandas por fracción de VO₂max asumidas"])
    ws.append(["Límite", "VO₂max no equivale a VDOT ni determina por sí solo el ritmo individual; calibrar con carrera/contrarreloj."])
    ws.append([])
    ws.append(["Tipo", "Fracción baja", "Fracción alta", "Más rápido (min/km)", "Más lento (min/km)", "Duración", "Objetivo"])
    for idx, (label, low, high, duration, goal) in enumerate(ZONES, start=7):
        ws.append([label, low, high, f'=IFERROR(200/($B$1*C{idx}-3.5)/1440,"")', f'=IFERROR(200/($B$1*B{idx}-3.5)/1440,"")', duration, goal])
        ws.cell(idx, 4).number_format = "[m]:ss"
        ws.cell(idx, 5).number_format = "[m]:ss"
        ws.cell(idx, 2).number_format = "0%"
        ws.cell(idx, 3).number_format = "0%"
    ws.append([])
    ws.append(["Tabla general orientativa", "Al cambiar B1, las filas anteriores se recalculan en Excel/LibreOffice."])
    ws.append(["VO₂max", "Recuperación", "Fácil", "Larga", "Sostenido", "Umbral", "Intervalos", "Repeticiones"])
    for val in (40, 45, 50, 55, 60, 65, 70):
        ws.append([val] + [pace_text(val, lo, hi) for _, lo, hi, _, _ in ZONES])
    ws.append([])
    ws.append(["Vigencia", "Fuente", "Valor", "Método", "Prueba/observación", "Ritmos por sesión", "Motivo del cambio"])
    ws.append(["Pendiente", "Sin fuente", None, "Sin calcular", "Aportar valor fechado o prueba válida", "Sin prescripción", "Configuración pendiente"])
    ws.append(["Fuentes", "https://www.acsm.org/docs/default-source/event-documents/annual-meeting-documents/abstracts/2019/2019-new/acsm19_abstracts_f.pdf"])
    ws.append(["Límites", "https://pubmed.ncbi.nlm.nih.gov/27747844/"])
    ws.column_dimensions["A"].width = 29
    for c in "BCDEFGH": ws.column_dimensions[c].width = 30
    ws.freeze_panes = "B7"
    ws["B1"].fill = PatternFill("solid", fgColor="FFF2CC")


def add_notes(wb: Workbook, modality: str, active: bool, no_race: bool):
    ws = wb.create_sheet("Uso y ajustes")
    notes = [
        ("Estado", "Activo provisional; confirmar elección" if active else "Base no activada: fechas ilustrativas"),
        ("Semanas mínimas", f"{len(SPECS[modality])} semanas desde una base reciente cercana a 84 km/semana."),
        ("Descarga", "Las semanas de descarga y afinación quedan por debajo del mínimo declarado de forma deliberada."),
        ("Fatiga", "Si persiste el cansancio, convertir calidad en fácil o descansar; no recuperar kilómetros perdidos."),
        ("Molestia", "Si aparece dolor que altera la zancada o empeora, suspender la sesión y valorar atención profesional."),
        ("Control semanal", "Comparar completado, fatiga, molestias y tirada larga de los últimos 30 días antes de progresar."),
        ("Tirada larga", "Evitar saltos bruscos frente a la mayor sesión de los últimos 30 días; repetir semana si es preciso."),
        ("Ritmos", "La tabla es aproximada. Ajustar por terreno, calor y fatiga; usar esfuerzo conversacional en fáciles."),
        ("Suunto", "Solo la copia activa se enlaza. Kore rellena resultados en Plan diario y el detalle Suunto por etiqueta exacta y fecha local."),
        ("Última semana", "Evaluación opcional sin carrera inscrita; sustituir la prueba por rodaje si no hay frescura." if no_race else "Competición hipotética en domingo; confirmar calendario antes de seguir el bloque."),
        ("Evidencia", "https://bjsm.bmj.com/content/early/2025/07/07/bjsports-2024-109380"),
    ]
    for row in notes: ws.append(row)
    ws.column_dimensions["A"].width = 22
    ws.column_dimensions["B"].width = 110
    for row in ws:
        for cell in row: cell.alignment = Alignment(wrap_text=True, vertical="top")


def save_with_backup(book: Workbook, path: Path) -> Path | None:
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


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("defaults", help="generate four example books; dates are illustrative")
    activate = sub.add_parser("activate", help="write the active book for one modality")
    activate.add_argument("modality", choices=SPECS)
    activate.add_argument("--start", type=date.fromisoformat, required=True, help="confirmed Monday YYYY-MM-DD")
    activate.add_argument("--no-race", action="store_true", help="last week is an optional evaluation")
    sub.add_parser("update", help="refresh the active book from the local Suunto database")
    args = parser.parse_args()
    minimums = read_local_minimums()
    if args.command == "defaults":
        DEFAULTS.mkdir(parents=True, exist_ok=True)
        for modality in SPECS:
            path = DEFAULTS / f"plan-base-{modality}.xlsx"
            save_with_backup(make_book(modality, BASE_MONDAY,
                                       minimum_weekly_km=minimums[modality]), path)
            print(path)
    elif args.command == "activate":
        if args.start.weekday() != 0:
            parser.error("--start must be a Monday")
        save_with_backup(make_book(args.modality, args.start, active=True,
                                   no_race=args.no_race,
                                   minimum_weekly_km=minimums[args.modality]), ACTIVE)
        print(ACTIVE)
    else:
        from plan_daily_sync import query_workouts, update_book, validate
        if not ACTIVE.exists():
            parser.error("No active block; confirm modality and start date first")
        book = load_workbook(ACTIVE)
        start, end, _ = validate(book)
        workouts = query_workouts(start, end)
        update_book(book, workouts)
        backup = save_with_backup(book, ACTIVE)
        print(f"Updated {ACTIVE}; {len(workouts)} workouts queried; previous copy {backup}")


if __name__ == "__main__":
    main()
