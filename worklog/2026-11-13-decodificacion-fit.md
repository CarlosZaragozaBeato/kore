# 2026-11-13 — Decodificación de FIT

**Estado:** completada para los 580 FIT existentes.

## Implementación

- Se añadió `suunto_ingest/decode.py` con el SDK oficial de Garmin FIT. Extrae sesiones, vueltas y muestras en tres tablas PostgreSQL, una transacción por entrenamiento.
- Los FIT originales siguen siendo la fuente completa; las tablas contienen campos habituales para consulta y visualización. El hash y la versión del decodificador permiten reanudar y reprocesar cuando cambie el archivo o el mapeo.
- Las posiciones del SDK están expresadas en semicírculos FIT. Se convierten a grados antes de guardarlas. Se corrigieron también los registros importados antes de detectar esa unidad.

## Verificación

- Un FIT real se decodificó sin errores y produjo 1.593 muestras y 10 vueltas.
- Los 580 FIT quedaron procesados: 580 sesiones, 7.056 vueltas y 1.813.756 muestras. No quedaron FIT pendientes de decodificación.
- La comprobación de coordenadas dio latitudes dentro de ±90° y longitudes dentro de ±180°.
- Una nueva ejecución procesó cero archivos, sin duplicar datos. Ocho pruebas locales correctas.
- Se creó `data/backups/kore-2026-11-13-decoded.dump` con permisos `600`. `pg_restore -l` confirma que incluye las tablas nuevas; la restauración completa sigue pendiente de prueba.

## Pendientes

- Crear consultas y métricas derivadas que un agente pueda consumir sin leer millones de muestras directamente.
- Programar la cadena de sincronización, descarga FIT y decodificación.
- Probar una restauración de la nueva copia de PostgreSQL.
