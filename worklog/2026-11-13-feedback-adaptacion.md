# Feedback subjetivo y adaptación semanal

## Estado y objetivo

Implementado un primer circuito local para registrar la respuesta subjetiva a las sesiones y proponer una acción semanal explicable sin modificar el plan activo.

## Cambios

- `plan_feedback.py` valida y guarda una observación por fecha en una hoja privada `Feedback`; una segunda escritura de la misma fecha la actualiza.
- `plan_adaptation.py` cruza cobertura Suunto, kilómetros comparables y feedback para devolver datos insuficientes, reducir, mantener o permitir la progresión ya prevista.
- `plan_app.py` expone `/feedback` y `/adaptation` solo dentro de la aplicación local existente.
- La ausencia de una actividad enlazada se conserva como dato desconocido, no como entrenamiento omitido ni cero kilómetros.
- El guardado de feedback usa la copia de seguridad atómica existente. La evaluación semanal es de solo lectura y no cambia ninguna fila del plan.

## Verificación

Se añadieron pruebas de actualización idempotente, texto seguro para Excel, falta de cobertura, progresión limitada al plan, señal de reducción, inmutabilidad del plan y bloque finalizado. Las 40 pruebas del repositorio pasan. También se comprobó que `/`, `/feedback` y `/adaptation` responden correctamente en el servidor local; al no haber empezado el bloque, la evaluación devuelve datos insuficientes y no escribe en el libro.

## Decisiones y siguiente paso

Los umbrales iniciales son heurísticas conservadoras visibles, no una valoración clínica. El siguiente paso es generar una comparación de las semanas futuras para una propuesta aceptada y exigir confirmación explícita antes de aplicarla.
