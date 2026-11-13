# 2026-11-12 — Sincronización histórica con PostgreSQL

**Estado:** implementada y verificada con datos reales. La descarga FIT posterior consta en `2026-11-12-fit-historico.md`.

## Cambios

- Se añadió `suunto_ingest/sync.py`: consulta `/v3/workouts` por páginas de hasta 50 elementos, conserva cada respuesta original y hace upsert en `suunto_workouts` usando `workoutKey` como clave.
- El cursor de `suunto_sync_state` avanza solo tras completar todas las páginas. Las ejecuciones siguientes solapan 24 horas para captar modificaciones recientes. Un bloqueo de PostgreSQL evita dos ejecuciones simultáneas.
- Se añadieron reintentos limitados para errores transitorios, detección de páginas repetidas y pruebas de fallo durante la paginación.
- La base definida en Compose ahora expone el puerto solo en localhost. Como Docker no estaba accesible para este usuario, se preparó PostgreSQL 18 en `data/postgres` y el runtime en `data/postgres-runtime`; ambos quedan fuera de Git. El script `infra/postgres-local.sh` controla esa instancia en el puerto 55432.
- Se añadieron `requirements.txt` e instrucciones de uso en `suunto_ingest/SYNC.md`.

## Verificación

- Primera ejecución real: 12 páginas, 580 entrenamientos procesados. PostgreSQL contenía 580 claves únicas y 580 payloads con `workoutKey`; el cursor avanzó.
- Segunda ejecución: 3 entrenamientos de la ventana reciente procesados. PostgreSQL siguió teniendo 580 filas y 580 claves únicas.
- Cuatro pruebas locales correctas, incluidas las que comprueban que el cursor no avanza si falla una página posterior o Suunto repite una página.
- `data/`, los JSON y el token renovado permanecen excluidos de Git. El directorio `data/` tiene permisos `700`; los archivos privados, `600`.
- Se creó una copia de la base en `data/backups/kore-2026-11-12.dump`, con permisos `600`. `pg_restore -l` confirmó que contiene las tablas y datos de sincronización; no se ha probado una restauración completa.
- Se detuvo y arrancó PostgreSQL desde el runtime persistente de `data/postgres-runtime`; tras el reinicio seguían presentes las 580 filas.

## Pendientes

- Resuelto después: los FIT se descargaron y conservaron; ver `2026-11-12-fit-historico.md`.
- Añadir una tarea programada y observabilidad de fallos; por ahora la sincronización se ejecuta manualmente.
- Automatizar y probar la restauración de copias de PostgreSQL, archivos originales y token renovado. La copia actual cubre solo la base.
- Migrar o sustituir las consultas del servicio Java por una API Python; el servicio Java no debe arrancarse contra esta base por su configuración `drop-and-create`.
