# Kore

Kore extrae entrenamientos de Suunto, conserva originales y datos estructurados en local, genera planes diarios parametrizados y cruza cada actividad con el día previsto. Todo el backend vigente es Python con PostgreSQL y FastAPI.

## Flujo principal

1. `suunto_ingest` sincroniza resúmenes, descarga FIT y los decodifica.
2. La misma ejecución actualiza el Excel activo mediante etiqueta exacta y fecha local.
3. `plan_app.py` genera borradores, activa un bloque y recoge feedback.
4. Kore compara plan, ejecución y sensaciones antes de proponer una adaptación futura.

```text
Suunto → PostgreSQL + FIT originales → plan activo → feedback → adaptación revisada
```

## Puesta en marcha

```bash
./infra/postgres-local.sh ensure
.venv/bin/uvicorn plan_app:app --host 127.0.0.1 --port 8092
```

Abrir `http://127.0.0.1:8092/`. La aplicación es exclusivamente local: permite escribir el Excel privado activo y no dispone de autenticación de red.

Para ejecutar manualmente toda la cadena Suunto y actualizar después el plan:

```bash
.venv/bin/python -m suunto_ingest.pipeline
```

Para refrescar únicamente el plan desde la base ya sincronizada:

```bash
.venv/bin/python -m plan_daily_sync
```

## Estructura esencial

| Ruta | Función |
|---|---|
| `suunto_ingest/` | Extracción, FIT, decodificación y cadena recuperable. |
| `kore_api/` | API local de consulta de entrenamientos. |
| `plan_engine.py` | Reglas deterministas de generación. |
| `resources/` | Catálogo de sesiones y referencias de planificación versionadas. |
| `plan_app.py` | Interfaz local de generación, feedback y adaptación. |
| `plan_workbook.py` | Esquema y guardado atómico del Excel. |
| `plan_daily_sync.py` | Enlace Suunto–plan por etiqueta y fecha. |
| `plan_lifecycle.py` | Finalización automática y retirada recuperable de bloques. |
| `infra/` | PostgreSQL local, copias verificadas y unidades `systemd --user`. |
| `tests/` | Pruebas sin datos personales. |
| `worklog/` | Historial técnico versionado. |
| `data/` | Datos privados ignorados por Git. |

Consulta [ARCHITECTURE.md](ARCHITECTURE.md), [PLAN_GENERATOR.md](PLAN_GENERATOR.md), [PLAN_FEEDBACK.md](PLAN_FEEDBACK.md) y [suunto_ingest/SYNC.md](suunto_ingest/SYNC.md).

## Privacidad y verificación

`.env`, PostgreSQL, FIT, Excel, análisis y copias viven en `data/` o archivos ignorados. No deben publicarse. Las pruebas se ejecutan con:

```bash
.venv/bin/python -m unittest discover -s tests -v
```
