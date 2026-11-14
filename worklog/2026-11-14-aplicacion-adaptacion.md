# Aplicación confirmada de una adaptación

## Estado y objetivo

Implementada la conversión de una recomendación semanal en una comparación concreta sobre fechas futuras. La escritura requiere una segunda acción explícita del usuario.

## Cambios

- `plan_adjustment.py` genera cambios de celda inspeccionables sin mutar el libro.
- Reducir selecciona la primera sesión estructurada de la semana objetivo y cambia su dosis prevista por la mínima existente.
- Mantener conserva el recurso y las dosis del nivel anterior en las sesiones estructuradas de la semana objetivo.
- Permitir la progresión prevista no genera cambios adicionales.
- La aplicación comprueba que la recomendación y el archivo sigan vigentes, rechaza fechas actuales o anteriores, conserva los kilómetros y archiva una copia privada.
- Cada modificación se registra en la hoja privada `Adaptaciones` con fecha, semana, acción, celda y valores anterior y nuevo.
- `plan_app.py` añade los pasos de vista previa y confirmación en `/adaptation`.

## Verificación

Las pruebas cubren reducción, mantenimiento, ausencia de escritura al progresar, protección de días anteriores y kilómetros, auditoría y rechazo de propuestas obsoletas. Las 44 pruebas del repositorio pasan. En la aplicación local se comprobó también que, antes del inicio del bloque, la evaluación no ofrece confirmación y una petición directa de vista previa no modifica el hash del Excel activo.

## Límites y siguiente paso

Esta versión adapta recursos y dosis estructuradas, no el volumen semanal. El siguiente paso es usar el historial de ejecución y adaptación como contexto al crear un bloque nuevo y diseñar por separado reglas de volumen que también requieran vista previa.
