# 2026-11-13 — Nivel inicial sugerido desde Suunto

**Estado:** implementado y verificado con datos sintéticos y lectura local.

## Cambio

Se añadió una evaluación de solo lectura de los últimos 84 días. Calcula volumen reciente, tirada larga, tamaño de sesiones y exposición estructurada a partir de parejas de trabajo y recuperación en las vueltas FIT. Devuelve niveles iniciales editables para velocidad y sostenido, evidencia, limitaciones y confianza baja o media. La aplicación utiliza estos valores como preselección y conserva sugerencia y elección en el Excel.

## Verificación y límites

Las pruebas cubren autolaps uniformes, detección de patrones estructurados, regresión ante una sola observación, confianza con varias observaciones y conservación de la evidencia en el libro. La evaluación se ejecutó contra la base local sin modificarla y la interfaz siguió funcionando. Los resultados personales no se registran aquí.

La detección no conoce RPE, propósito de la sesión, sueño ni respuesta posterior. Los umbrales de tamaño son heurísticas del sistema y no umbrales fisiológicos. La decisión siguiente es añadir percepción de esfuerzo y respuesta a la carga para revisar semanas futuras.
