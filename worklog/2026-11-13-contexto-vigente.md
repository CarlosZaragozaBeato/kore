# 2026-11-13 — Vigencia de FIT y contexto local

**Estado:** correcciones implementadas y verificadas localmente.

## Cambios

- La sincronización marca un FIT como pendiente si cambia `lastModified`. El descargador consulta otra vez a Suunto aunque haya un FIT anterior en disco y lo reemplaza solo tras validarlo.
- La API cuenta un entrenamiento como decodificado solo si el FIT está vigente, coincide el hash y se usó la versión actual del parser. Las métricas pendientes de actualización quedan excluidas del contexto y del detalle.
- El contexto usa días de calendario en `Europe/Madrid`, incluye días sin entrenamientos, semanas agrupadas por lunes y cobertura de TSS. El día actual es parcial.
- Se actualizó el contrato OpenAPI y la documentación local.

## Verificación

- Las 11 pruebas locales pasan, incluida una prueba del cambio horario de primavera.
- Una transacción de prueba revertida confirmó que cambiar `lastModified` deja el FIT pendiente.
- La API real respondió 200 para estado y contexto; el contexto devolvió 28 días, cinco semanas y totales semanales coherentes.
- No se hizo una nueva llamada a Suunto en esta tarea.

## Siguiente paso

- Programar la secuencia de sincronizar resúmenes, descargar FIT pendientes y decodificarlos; hacer visibles los fallos y la hora de última actualización al consumidor.
