# Actividades canónicas y Museo funcional

## Objetivo

Crear el puente entre una actividad independiente del proveedor y el Museo personal, sin esperar a integrar Suunto.

## Implementación

- Modelo `activity` con deporte, fecha, duración, distancia, desnivel, frecuencia cardiaca media y `sourceRef`.
- Alta manual de actividades desde el Museo.
- Bandeja de actividades que todavía no se han convertido en piezas.
- Creación explícita de una pieza con título, motivo, relato, etiquetas y zapatilla opcional.
- Snapshot de métricas para que el recuerdo sobreviva a la desconexión de su fuente.
- Prevención de duplicados: una actividad solo puede originar una pieza de Museo.
- Contadores reales de actividades y piezas en Inicio.
- Persistencia atómica mediante mutaciones de `session-core`, sin JSON en Compose.

## Decisiones

- El alta manual pide fecha, no hora. Guarda mediodía UTC como instante técnico para no inventar una hora de entrenamiento; un adaptador aportará el valor exacto.
- Registrar no equivale a archivar: el Museo solo crece por decisión explícita del usuario.
- `sourceRef.type = manual` permite que Suunto, GPX y otros adaptadores produzcan el mismo modelo más adelante.

## Verificación

- Ocho pruebas del contrato, incluida actividad, snapshot del Museo y rechazo de duplicados.
- APK debug compilado correctamente.
- Android Lint: 0 errores; permanecen avisos informativos de versiones y autoboxing de estado Compose.

## Siguiente paso

Implementar marcas personales con evidencia y después el adaptador Suunto hacia `activity`.
