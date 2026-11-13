"""Local plan builder UI. Launch only on 127.0.0.1 (see PLAN_GENERATOR.md)."""

from __future__ import annotations

from datetime import date, timedelta
from html import escape
from io import BytesIO
from secrets import token_urlsafe
from urllib.parse import parse_qs

from fastapi import FastAPI, Request
from fastapi.responses import HTMLResponse, StreamingResponse
from pydantic import ValidationError

from plan_builder import activate_draft, build_book
from plan_engine import PlanDraft, PlanInputs, WEEKDAYS, generate


app = FastAPI(title="Kore: generador de planes", version="0.1.0")
_drafts: dict[str, PlanDraft] = {}
CSS = """<style>body{font:16px system-ui;max-width:1050px;margin:2rem auto;padding:0 1rem;color:#20242a}
label{display:block;margin:.65rem 0}input,select{font:inherit;padding:.35rem;width:17rem;max-width:100%}
fieldset{border:1px solid #cbd2d9;border-radius:8px;margin:1rem 0;padding:1rem}
legend{font-weight:700}button,.button{font:inherit;padding:.5rem .9rem;background:#145d76;color:white;
border:0;border-radius:5px;cursor:pointer;text-decoration:none;display:inline-block}
table{border-collapse:collapse;width:100%;margin:1rem 0}td,th{border:1px solid #cbd2d9;padding:.4rem;text-align:left}
.error{background:#ffe7e3;padding:1rem}.notice{background:#e6f4f1;padding:1rem}
small{color:#555}</style>"""


def page(body: str) -> HTMLResponse:
    return HTMLResponse(f"<!doctype html><html lang='es'><meta charset='utf-8'><title>Kore planes</title>"
                        f"{CSS}<body>{body}</body></html>", headers={"Cache-Control": "no-store"})


def field(label: str, name: str, value: str, *, type_: str = "text", required: bool = True) -> str:
    return (f"<label>{escape(label)}<br><input name='{name}' type='{type_}' "
            f"value='{escape(value, quote=True)}' {'step=any' if type_ == 'number' else ''} "
            f"{'required' if required else ''}></label>")


def select(label: str, name: str, choices: tuple[tuple[str, str], ...], default: str) -> str:
    options = "".join(f"<option value='{escape(value)}' {'selected' if value == default else ''}>"
                      f"{escape(title)}</option>" for value, title in choices)
    return f"<label>{escape(label)}<br><select name='{name}'>{options}</select></label>"


def form(error: str = "") -> str:
    today = date.today()
    monday = today + timedelta(days=(7 - today.weekday()) % 7)
    distances = tuple((v, v) for v in ("5k", "10k", "21k", "42k"))
    return ("<h1>Generador de planes Kore</h1><p>Selecciona los parámetros y revisa el borrador "
            "antes de activar un bloque. Los resultados de Suunto solo se insertan en el activo.</p>"
            + (f"<div class='error'>{escape(error)}</div>" if error else "")
            + "<form method='post' action='/preview'><fieldset><legend>Objetivo y calendario</legend>"
            + select("Distancia de este bloque", "focus_distance", distances, "10k")
            + select("Objetivo final", "ultimate_distance", distances, "42k")
            + select("Tipo de bloque", "objective", (("development", "Desarrollo sin carrera"), ("race", "Competición")), "development")
            + field("Inicio del bloque (lunes)", "start_date", monday.isoformat(), type_="date")
            + field("Semanas (se calculan desde la fecha si hay competición)", "weeks", "8", type_="number")
            + field("Fecha de competición (si aplica, en la última semana)", "event_date", "", type_="date", required=False)
            + "</fieldset><fieldset><legend>Estado y carga</legend>"
            + select("Necesidad principal", "bottleneck", (("speed", "Velocidad"), ("sustained", "Sostener ritmo alto"), ("balanced", "Equilibrado")), "balanced")
            + field("Km semanales actuales observados", "current_weekly_km", "", type_="number")
            + field("Km semanales objetivo", "target_weekly_km", "", type_="number")
            + field("Tirada larga reciente (km)", "recent_long_run_km", "", type_="number")
            + select("Fatiga actual", "fatigue", (("normal", "Normal"), ("elevated", "Elevada")), "normal")
            + "</fieldset><fieldset><legend>Disponibilidad y progresión</legend>"
            + "<p>Días de entrenamiento</p>"
            + " ".join(f"<label style='display:inline-block;margin-right:1rem'><input style='width:auto' "
                       f"type='checkbox' name='training_days' value='{day}' "
                       f"{'checked' if day != 4 else ''}> {escape(name)}</label>"
                       for day, name in enumerate(WEEKDAYS))
            + select("Día de tirada larga", "long_run_day",
                     tuple((str(day), name) for day, name in enumerate(WEEKDAYS)), "6")
            + select("Sesiones estructuradas por semana, además de la tirada",
                     "quality_sessions_per_week", (("0", "Ninguna"), ("1", "Una"), ("2", "Dos")), "2")
            + field("Nivel inicial de series (0=1 min, 1=3 min, 2=1 km, 3=1,6 km)", "speed_entry_level", "1", type_="number")
            + field("Nivel inicial sostenido (0=6 min, 1=10 min, 2=15 min, 3=continuo)", "sustained_entry_level", "1", type_="number")
            + field("Aumento máximo elegido de km/semana", "max_weekly_increase_km", "4", type_="number")
            + field("Aumento máximo elegido de tirada larga (km/semana)", "max_long_run_increase_km", "1", type_="number")
            + field("Cada cuántas semanas reducir carga", "deload_every", "4", type_="number")
            + field("Factor de semana reducida (0,5–1)", "deload_fraction", "0.8", type_="number")
            + "</fieldset><p><small>Los límites y progresiones son elecciones para el borrador, no valores óptimos universales. "
            "Los ritmos de calidad quedan por calibrar.</small></p><button>Generar borrador</button></form>")


@app.get("/", response_class=HTMLResponse)
def home():
    return page(form())


@app.post("/preview", response_class=HTMLResponse)
async def preview(request: Request):
    raw = parse_qs((await request.body()).decode("utf-8"), keep_blank_values=True)
    data = {key: value[-1] for key, value in raw.items()}
    try:
        data["training_days"] = [int(part) for part in raw.get("training_days", [])]
        if not data.get("event_date"):
            data["event_date"] = None
        inputs = PlanInputs.model_validate(data)
        draft = generate(inputs)
    except (ValidationError, ValueError) as exc:
        return page(form(str(exc)))
    token = token_urlsafe(24)
    _drafts[token] = draft
    if len(_drafts) > 20:
        _drafts.pop(next(iter(_drafts)))
    week_rows = "".join(f"<tr><td>{index}</td><td>{escape(draft.sessions[(index-1)*7].phase)}</td>"
                        f"<td>{km:g}</td></tr>" for index, km in enumerate(draft.weekly_km, 1))
    day_rows = "".join(f"<tr><td>{session.week}</td><td>{session.day}</td>"
                       f"<td>{escape(WEEKDAYS[session.day.weekday()])}</td><td>{escape(session.name)}</td>"
                       f"<td>{session.km:g}</td><td>{escape(session.dose_target or '')}</td></tr>"
                       for session in draft.sessions[:14])
    notes = "".join(f"<li>{escape(note)}</li>" for note in draft.notes)
    body = (f"<h1>Borrador: {escape(inputs.focus_distance)} hacia {escape(inputs.ultimate_distance)}</h1>"
            f"<p>Inicio {inputs.start_date}; {inputs.weeks} semanas; {len(inputs.training_days)} días de entrenamiento.</p>"
            f"<ul>{notes}</ul><h2>Volumen semanal</h2><table><tr><th>Semana</th><th>Fase</th><th>Km</th></tr>{week_rows}</table>"
            f"<h2>Primeras dos semanas</h2><table><tr><th>Semana</th><th>Fecha</th><th>Día</th>"
            f"<th>Sesión</th><th>Km</th><th>Dosis</th></tr>{day_rows}</table>"
            f"<p><a class='button' href='/download/{token}'>Descargar Excel borrador</a></p>"
            f"<form method='post' action='/activate/{token}'><button>Activar este bloque</button></form>"
            "<p>Activar archivará el libro activo anterior. Si contiene resultados reales, la sustitución se rechazará.</p>"
            "<p><a href='/'>Cambiar parámetros</a></p>")
    return page(body)


@app.get("/download/{token}")
def download(token: str):
    draft = _drafts.get(token)
    if draft is None:
        return page("<p>Borrador caducado. <a href='/'>Generar otro</a>.</p>")
    output = BytesIO()
    build_book(draft).save(output)
    output.seek(0)
    filename = f"kore-borrador-{draft.inputs.focus_distance}-{draft.inputs.start_date}.xlsx"
    return StreamingResponse(output, media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                             headers={"Content-Disposition": f'attachment; filename="{filename}"',
                                      "Cache-Control": "no-store"})


@app.post("/activate/{token}", response_class=HTMLResponse)
def activate(token: str):
    draft = _drafts.get(token)
    if draft is None:
        return page("<p>Borrador caducado. <a href='/'>Generar otro</a>.</p>")
    try:
        path = activate_draft(draft)
    except (ValueError, OSError, KeyError) as exc:
        return page(f"<div class='error'>{escape(str(exc))}</div><p><a href='/'>Volver</a></p>")
    del _drafts[token]
    return page(f"<h1>Bloque activado</h1><p>{escape(str(path))}</p>"
                f"<p>Etiqueta Suunto: <code>kore_{escape(draft.inputs.focus_distance)}_"
                f"{draft.inputs.start_date:%Y%m%d}</code></p><p><a href='/'>Crear otro borrador</a></p>")
