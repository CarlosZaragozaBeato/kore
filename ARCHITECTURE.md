# Arquitectura vigente

## Componentes

- **Ingesta:** `suunto_ingest.pipeline` ejecuta resúmenes, FIT, decodificación y actualización del plan activo. Cada intento queda registrado en PostgreSQL.
- **Persistencia:** PostgreSQL local conserva datos estructurados; `data/raw/` conserva originales. Las copias privadas incluyen restauración de prueba.
- **Consulta:** `kore_api` expone información de solo lectura en localhost.
- **Planificación:** `plan_engine` combina parámetros explícitos con los recursos de `resources/session_catalog.json`. No usa un agente para construir el bloque.
- **Libro activo:** `plan_builder` crea el Excel, `plan_workbook` define su contrato y `plan_daily_sync` incorpora resultados Suunto.
- **Ciclo de vida:** `plan_lifecycle` mueve bloques vencidos a `completed/` y bloques retirados a `discarded/`; retirar nunca borra el archivo.
- **Respuesta al entrenamiento:** `plan_feedback`, `plan_adaptation` y `plan_adjustment` separan observación, recomendación, vista previa y confirmación.

## Límites de escritura

| Operación | Escritura permitida |
|---|---|
| Ingesta Suunto | PostgreSQL, FIT originales y estado de pipeline. |
| Activar bloque | Sustituye el Excel activo tras archivar el anterior. |
| Sincronizar plan | Solo columnas de resultados, resumen semanal y progreso temporal. |
| Guardar feedback | Hoja privada `Feedback`, con copia previa. |
| Confirmar adaptación | Solo fechas futuras y dosis/recursos mostrados en la comparación; nunca kilómetros. |
| Finalizar bloque | Marca el libro como finalizado, añade motivo y fecha, lo guarda en `completed/` y elimina únicamente el puntero activo. |
| Retirar bloque | Movimiento atómico del archivo intacto a `discarded/`, tras confirmación con la etiqueta exacta. |

## Datos privados

- `data/postgres/` y `data/postgres-runtime/`: base y binarios locales necesarios.
- `data/raw/`: respuestas y FIT originales.
- `data/plans/active/`: único bloque operativo.
- `data/plans/archive/`: copias anteriores creadas antes de cada escritura.
- `data/plans/completed/`: bloques finalizados, incluido el cierre automático posterior a la fecha final.
- `data/plans/discarded/`: bloques retirados de la actividad pero conservados para recuperación.
- `data/plans/legacy/`: demostraciones y plantillas anteriores, fuera del flujo vigente.
- `data/analysis/` y `data/exports/`: resultados personales derivados.
- `data/backups/`: copias verificadas; no sustituye una copia en otro disco.

Nada bajo `data/` se versiona. El tamaño de `data/postgres/pg_wal` forma parte del funcionamiento de PostgreSQL y no debe limpiarse borrando archivos manualmente.

## Procesos programados

`infra/systemd/kore-suunto-sync.timer` ejecuta diariamente la cadena completa. `kore-backup.timer` crea semanalmente una copia y prueba su restauración. `Persistent=true` recupera ejecuciones omitidas al volver a iniciar la sesión de usuario.
