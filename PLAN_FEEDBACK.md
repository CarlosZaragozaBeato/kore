# Feedback y adaptación semanal

Kore combina los resultados objetivos enlazados desde Suunto con observaciones subjetivas guardadas en el Excel privado activo. La pantalla `http://127.0.0.1:8092/feedback` permite registrar o corregir una observación por fecha:

| Campo | Escala | Interpretación |
|---|---:|---|
| RPE de la sesión | 1–10 | Esfuerzo percibido global de la sesión. |
| Disponibilidad antes | 1–5 | Estado percibido antes de entrenar; 1 es muy bajo y 5 muy bueno. |
| Recuperación posterior | 1–5 | Respuesta posterior; 1 es muy mala y 5 muy buena. |
| Molestia o dolor | 0–10 | 0 es ausencia y 10 es máxima intensidad percibida. |

Los datos se almacenan en la hoja `Feedback` del libro activo, dentro de `data/`, y no se versionan. Guardar de nuevo la misma fecha actualiza su fila y el guardado archiva una copia privada del libro anterior.

## Propuesta semanal

`http://127.0.0.1:8092/adaptation` es una evaluación de solo lectura. Considera únicamente días planificados ya transcurridos de la semana actual:

- Con menos del 60 % de esos días cubiertos por resultados Suunto válidos, devuelve **datos insuficientes**. Una actividad sin etiqueta o pendiente de decodificar no se interpreta como descanso ni como cero kilómetros.
- Con menos de dos días de feedback, devuelve **datos insuficientes**.
- Propone **reducir** si aparece molestia de 7/10 o más, recuperación o disponibilidad de 2/5 o menos, o al menos dos sesiones con RPE 9–10.
- Permite la **progresión ya prevista** solo con tres días de feedback, kilómetros comparables entre el 90 % y el 110 % del plan, RPE medio de 7 o menos, recuperación mínima de 4/5 y molestias máximas de 3/10.
- En los demás casos propone **mantener**.

Estos umbrales son reglas conservadoras iniciales del producto, no límites clínicos ni una prescripción universal. La propuesta muestra métricas, motivos y confianza; no altera sesiones ni kilómetros. Una molestia importante o persistente requiere valoración profesional y no debe resolverse solo con esta regla.

El uso conjunto de carga externa e información subjetiva sigue el principio de individualizar el entrenamiento según recuperación y estado observado. Referencias de contexto: [entrenamiento individualizado según recuperación y estado](https://pubmed.ncbi.nlm.nih.gov/35975912/) y [RPE de sesión combinado con volumen](https://pubmed.ncbi.nlm.nih.gov/33064812/).

## Siguiente evolución

La aplicación podrá preparar una copia de las semanas futuras con la dosis mínima, el nivel actual o la progresión prevista. Antes de sustituir el activo deberá mostrar la comparación, exigir confirmación y conservar intactos los días pasados.
