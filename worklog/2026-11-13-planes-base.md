# 2026-11-13: planes diarios y enlace Suunto

Estado: implementado; probado con datos sintéticos y consulta local de solo lectura. Se generó una copia activa provisional para verificar el flujo; no representa una elección deportiva confirmada. Los cuatro libros base se regeneraron con fechas ilustrativas.

Corrección del diseño anterior: `Plan semanal` resumía pero no permitía comparar previsto y real por fecha. Se añadió `Plan diario` como hoja principal, una fila por día, con columnas de planificación y resultados en la misma fila. El lunes inicial es recovery de 10 km. `Plan semanal` se recalcula desde las filas diarias. El antiguo 5 km activo provisional se preservó en `data/plans/archive/` antes de sustituirlo.

Integración: `plan_daily_sync.py` consulta `suunto_workouts` y `suunto_fit_sessions` con la misma condición de frescura FIT que el exportador de la rama `codex/excel-export`. Exige pertenencia exacta de la etiqueta del bloque a `userTags`, usa `Europe/Madrid` para la fecha local, agrupa varias carreras del día, deja FIT pendientes sin métricas inventadas y reconstruye la hoja de detalle por clave Suunto. `plan_excel.py update` actualiza el libro activo con copia previa y sin tocar columnas planificadas. La consulta a la base local para el bloque futuro se completó y devolvió 0 actividades, lo esperado antes del inicio; no se han demostrado aún coincidencias reales de Suunto para este bloque.

Verificación: cuatro pruebas con actividades sintéticas cubren lunes 10 km previstos/9,8 km reales, límite horario UTC→Madrid, etiqueta ausente, dos actividades el mismo día, descanso sin confirmar y confirmado, FIT pendiente, métricas incompletas, claves duplicadas y actualización repetida. `py_compile` pasó. Los libros privados se abrieron y se comprobó la distribución de hojas y filas. La integración real con actividad etiquetada se verificará cuando exista una actividad del bloque.

Se generó `data/plans/examples/demo-enlace-diario-10k.xlsx`, identificado como sintético y separado del activo. Se comprobó en el archivo que el lunes conserva 10 km previstos y muestra 9,8 km, 49 min, 5:00 min/km, FC 142 y TSS 45 procedentes de la actividad ficticia; el resumen semanal muestra 74 km previstos y 9,8 km reales.

La tabla de ritmos queda sin valor individual en el código. El valor, fecha y fuente se conservan únicamente en los libros privados y requieren calibración antes de usarse.

Próximo paso: al comenzar el bloque, comprobar etiqueta exacta en Suunto y ejecutar `plan_excel.py update`; revisar el resultado diario y la respuesta a la carga semanal antes de ajustar sesiones futuras.

Corrección posterior solicitada por el usuario: la primera distribución tenía demasiados rodajes sin estímulo. Se rediseñaron las cuatro plantillas y el activo alrededor de tres sesiones clave semanales, con dosis mínima/prevista/máxima y calentamiento/vuelta a la calma. El 10 km progresa de series de 1 min y umbral breve a la combinación de la semana 7 (`6×1 km`, `8 km progresivos`, `3×2 km`). Los miércoles pueden incluir strides. Se preservó una copia del activo previo, que se comprobó sin resultados reales, antes de regenerarlo. El ejemplo sintético también se actualizó. La literatura revisada orienta a separar cargas con días fáciles; no se interpreta «tres sesiones clave» como tres esfuerzos máximos.
