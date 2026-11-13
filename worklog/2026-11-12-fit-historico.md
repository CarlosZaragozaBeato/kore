# 2026-11-12 — Descarga histórica de FIT

**Estado:** completada para los 580 entrenamientos existentes.

## Cambios

- Se añadió `suunto_ingest/fit.py` para descargar FIT por lotes desde `/v3/workouts/{workoutKey}/fit`.
- Cada archivo se valida como FIT, se guarda fuera de Git con permisos `600` y se registra en PostgreSQL con estado, tamaño y hash SHA-256. Si el archivo existe tras una interrupción, se reutiliza sin descargarlo otra vez.
- El descargador aplica una pausa entre peticiones y una recuperación limitada ante HTTP 401. Comparte el bloqueo de sincronización con la carga de resúmenes para evitar renovaciones simultáneas del token.

## Verificación

- Se validó con la cuenta real que el endpoint FIT responde HTTP 200 y devuelve un archivo con firma FIT.
- Tras varias ejecuciones reanudables, PostgreSQL contiene 580 entrenamientos con `fit_status='ok'`. Existen 580 archivos FIT locales; todos coinciden con sus hashes, tienen firma FIT y permisos `600`. No quedaron FIT pendientes ni marcados como no disponibles.
- Se detuvieron las descargas cuando Suunto devolvió HTTP 401. Una comprobación posterior volvió a recibir HTTP 200; con un ritmo menor y una pausa ante 401 se completó la descarga. No está confirmada la causa del 401.
- Siete pruebas locales correctas, incluidas la reutilización de un archivo tras interrupción, rechazo de FIT truncado y renovación ante 401.
- Se crearon nuevas copias locales en `data/backups/kore-2026-11-12-with-fit.dump` y `data/backups/suunto-raw-and-token-2026-11-12.tar.gz`, ambas con permisos `600`. Se comprobó que los archivos pueden listarse; no se probó una restauración completa.

## Pendientes

- Decodificar FIT y derivar muestras, vueltas y métricas para consultas y agentes.
- Programar ejecuciones periódicas de sincronización y FIT; revisar alertas y límites de Suunto.
- Probar una restauración completa de las copias y automatizar su actualización.
