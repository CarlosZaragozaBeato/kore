# Generador flexible y limpieza del repositorio

## Estado y objetivo

Revisado el primer bloque activado con el generador parametrizado. Se amplió la capacidad de planificación y se retiraron caminos heredados para dejar un único flujo vigente y documentado.

## Hallazgos del borrador

- La duración y el incremento máximo seleccionados pueden no permitir alcanzar el volumen objetivo; el borrador ya lo indicaba en una nota, pero el formulario necesitaba hacer más visibles los controles que producen esa trayectoria.
- La tirada larga aumentaba incluso en una semana de descarga. Se corrigió para aplicar una reducción específica sin avanzar el estado de progresión durante esa semana.
- La colocación y combinación de sesiones de calidad eran demasiado automáticas.

El libro activo se inspeccionó en modo de solo lectura y no se regeneró ni sustituyó.

## Cambios de planificación

- Días de calidad automáticos o elegidos explícitamente con validación de separación.
- Mezcla mixta o enfocada en la limitación principal.
- Cadencia configurable para avanzar dosis.
- Objetivo máximo de tirada larga y factor específico de reducción en descarga.
- Referencias personales versionadas por distancia en `resources/planning_profiles.json`; son editables y no se fuerzan en descargas.
- Las decisiones nuevas se conservan en la hoja `Reglas y decisiones` del borrador.

## Limpieza y documentación

- Eliminado el prototipo Java/Quarkus del árbol vigente; sigue recuperable en Git.
- Eliminado el generador rígido `plan_excel.py`. `plan_workbook.py` contiene únicamente contrato y persistencia; `plan_daily_sync.py` actualiza el activo.
- La cadena programada de Suunto termina ahora refrescando el plan si existe uno activo.
- Retiradas cachés Python regenerables del proyecto.
- Las plantillas y demostraciones privadas antiguas se movieron a `data/plans/legacy/2026-11-13/`; no se borraron datos personales, FIT, base, copias ni exportaciones.
- Añadidos `README.md` y `ARCHITECTURE.md`; se actualizaron las guías de planes, catálogo e ingesta.

## Verificación y siguiente paso

Las pruebas cubren los parámetros nuevos, la reducción de tirada en descarga y el contrato de enlace diario con el libro generado por el motor vigente. Las 47 pruebas disponibles pasaron tras la refactorización; después se amplió también la prueba del formulario para comprobar que los parámetros nuevos quedan guardados en el Excel.

El bloque activo actual conserva las reglas con las que fue creado. Si se desean las nuevas reglas de descarga y flexibilidad, hay que generar un nuevo borrador, compararlo y activarlo explícitamente antes de empezar o cuando se decida cambiar de bloque.
