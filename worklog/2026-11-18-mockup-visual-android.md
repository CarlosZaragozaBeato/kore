# Mockup visual navegable de Kore Android

## Objetivo

Construir una primera versión visual para validar estructura, jerarquía y features antes de conectar lógica real.

## Implementación

- Navegación inferior con Inicio, Plan, Progreso, Historial y Ajustes.
- Inicio con objetivo activo, sesión del día, volumen semanal, estado, último entrenamiento y recomendación.
- Plan semanal por días, detalle de sesión, resultados simulados y formulario visual para un nuevo bloque.
- Progreso con media relativa al objetivo, tendencia, gráfica, capacidades, marcas y próxima evaluación.
- Histórico con bloques anteriores, entrenamientos destacados y kilometraje de zapatillas.
- Ajustes con Suunto simulado, sesión, privacidad y preferencias de planificación.
- Todos los valores deportivos se identifican como datos de demostración; no se escribe información real.
- Material 3, tema claro/oscuro y componentes inspirados en la jerarquía visual de Muskop.

## Verificación

- `:session-core:test` correcto.
- `:app:assembleDebug` correcto tras añadir el mockup.
- `:app:lintDebug` correcto: 0 errores; quedan cinco avisos informativos por mantener SDK y dependencias alineados con Muskop.
- No había dispositivo o emulador conectado para una prueba visual real.

## Siguiente validación

Instalar el APK y revisar navegación, densidad de información, nombres de secciones, modo oscuro y legibilidad en el dispositivo. No implementar integración real hasta recibir feedback visual sobre este corte.
