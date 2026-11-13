# 2026-11-13 — Evidencia para el agente

**Estado:** implementado y comprobado localmente.

## Decisión

El contexto del agente expone datos observados y señales de cobertura. No calcula preparación, fatiga ni recomendaciones clínicas o deportivas a partir de TSS sin un criterio validado.

## Cambios

- Se añadió `sports` al contexto: sesiones, duración, distancia, TSS y número de sesiones con TSS, frecuencia cardíaca media y potencia media por deporte.
- Se añadió `availability`: último éxito y estado de ingesta, fechas de los últimos resúmenes y FIT vigentes, pendientes de descarga y decodificación.
- El día actual se marca `partial`. Una semana se marca `complete` solo cuando sus siete días terminaron y están dentro de la ventana solicitada.
- Se actualizó el contrato OpenAPI y la documentación.

## Verificación

- En la base actual hay 580 sesiones; 578 tienen TSS, 580 frecuencia cardíaca media y 522 potencia media. Son recuentos de disponibilidad, no una valoración del entrenamiento.
- La API respondió 200 para ventanas de 28 y 365 días. En ambas, la suma de sesiones y de sesiones con TSS por deporte coincidió con los totales del contexto; hubo exactamente un día parcial.
- Las 12 pruebas locales pasan, incluida la ausencia de historial de ingesta en una base recién creada.

## Siguiente paso

- Definir con el usuario qué decisiones de entrenamiento debe apoyar el agente y qué datos adicionales necesita para ellas. Mantener visible la cobertura y la vigencia al consumir este contexto.
