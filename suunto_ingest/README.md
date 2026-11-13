# Primera extracción de Suunto

Desde la raíz del repositorio:

```bash
python -m suunto_ingest.probe --days 30 --limit 1
```

Requiere el `.env` local con `SUUNTO_CLIENT_ID`, `SUUNTO_CLIENT_SECRET`, `SUUNTO_SUBSCRIPTION_KEY` y `SUUNTO_REFRESH_TOKEN`. Usa solo la biblioteca estándar de Python. Consulta una página de resúmenes de entrenamientos de los últimos 30 días; `--days` y `--limit` permiten ajustar la prueba (`--limit` entre 1 y 50).

La primera renovación parte del token del `.env`. A partir de entonces se usa el token vigente de `data/secrets/suunto-token.json`, que se actualiza antes de consultar entrenamientos. Para conservar el acceso, incluye ese archivo en una copia de seguridad privada. No vuelvas a sustituirlo por el token inicial del `.env`.

Las respuestas originales se guardan en `data/raw/suunto/workouts/`, junto a un archivo `.meta.json` con la ventana y los parámetros de la consulta. `data/` está excluido de Git. Estos archivos pueden contener información de salud y ubicación.

Esta herramienta valida la conexión y conserva una sola página. Para la sincronización completa, PostgreSQL y los FIT, consulta [SYNC.md](SYNC.md).

Pruebas locales:

```bash
python -m unittest discover -s tests -v
```
