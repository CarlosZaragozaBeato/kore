# Marcas personales funcionales

## Objetivo

Conservar las mejores marcas actuales y toda su evolución sin sobrescribir referencias anteriores.

## Implementación

- Alta por distancia, duración y fecha.
- Distancias rápidas 5K, 10K, media maratón y maratón, más distancia personalizada.
- Evidencia diferenciada: carrera oficial, entrenamiento, estimación o entrada manual.
- Vínculo opcional a una actividad o pieza del Museo.
- Validación de que actividad y Museo representan la misma sesión cuando se informan juntas.
- Relación inversa `personalBestIds` en la pieza del Museo.
- Ritmo derivado, mejor marca actual por distancia y mejora respecto a la referencia anterior.
- Cronología completa por distancia y contadores reales en Inicio.
- Rechazo de duplicados exactos sin borrar ni sustituir datos históricos.

## Verificación

- Nueve pruebas del contrato, incluida derivación de la marca actual y vínculo bidireccional con Museo.
- APK debug compilado correctamente.
- Android Lint: 0 errores; se mantienen únicamente avisos informativos ya conocidos.

## Siguiente paso

Construir el adaptador Suunto hacia `activity` y después asignar actividades a zapatillas para calcular kilómetros verificables.
