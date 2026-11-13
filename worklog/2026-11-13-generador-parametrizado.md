# 2026-11-13 — Generador parametrizado de planes

**Estado:** primera versión local implementada y verificada con datos sintéticos.

## Revisión del diseño anterior

El generador diario anterior fija en código la duración de cada modalidad, los días disponibles, la distribución semanal, los volúmenes, tres sesiones estructuradas y el escalón inicial de series. El libro diario y el enlace con Suunto funcionan, pero esas decisiones no se pueden seleccionar al construir un bloque.

## Cambio

Se añadió un motor determinista con catálogo de sesiones y entradas explícitas para calendario, objetivo inmediato y final, necesidad principal, volumen, días, tirada larga, nivel inicial de series y sostenido, ritmo de progresión y fatiga declarada. Se añadieron un generador de libros compatible con el esquema diario y una interfaz web local con vista previa, descarga y activación explícita. Los libros previos se archivan mediante el mecanismo existente; la activación rechaza reemplazar uno con resultados reales.

## Verificación y límites

Las pruebas con datos sintéticos comprueban que diferentes niveles y objetivos cambian las sesiones, que días y volúmenes siguen las entradas y que el libro generado acepta el enlace diario de Suunto. El formulario se probó localmente. No se ha activado ningún borrador generado por este motor ni se han usado datos reales de Suunto en las pruebas. El motor no infiere nivel ni ritmos del historial y no modifica semanas futuras automáticamente tras cada sincronización; esas decisiones requieren una siguiente iteración.

## Catálogo versionado

Las sesiones dejaron de estar incrustadas en el motor. Se añadió un catálogo JSON validado con 13 recursos y progresiones explícitas. Los recursos actuales están pendientes de revisión deportiva y se identifican como borradores. El Excel generado incluye una hoja con el recurso y el motivo de cada selección. Las ampliaciones de un agente deben cumplir el contrato del catálogo y aportar referencias antes de marcarse como revisadas.
