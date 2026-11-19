# Cambio de producto: archivo personal del runner

## Decisión

Kore deja de ser un planificador de entrenamiento. Su función pasa a ser conservar la historia personal del corredor con una presentación cuidada: zapatillas, sesiones especiales y evolución de marcas.

La aplicación continúa siendo Android local-first, con sesión portable, sin backend y sin base de datos. Suunto es el primer proveedor, no el idioma del dominio.

## Contrato

- Sesión Kore v2 con `activities`, `museum`, `personalBests`, `shoes`, `sources` y `settings`.
- Vocabulario canónico independiente del proveedor mediante `sourceRef`.
- Migración v1→v2 que conserva los campos anteriores y transforma entrenamientos/marcas compatibles.
- El museo guarda snapshot y relato para no depender permanentemente del proveedor.
- Las marcas forman una cronología; no se sobrescriben al conseguir una nueva.
- El kilometraje de zapatillas deriva de actividades asignadas más correcciones explícitas.

## Mockup

La navegación pasa a Inicio, Museo, Marcas, Zapatillas y Ajustes. Incluye visualmente selección por fecha, relaciones museo/PB/zapatilla, PB actuales e históricos, fichas de calzado, valoraciones y fases de uso.

## Siguiente paso

Validar el nuevo mockup en dispositivo. Tras aprobarlo, implementar primero las mutaciones de zapatillas y después el flujo actividad→museo→marca personal.

## Verificación

- Cinco pruebas de `session-core` correctas, incluida migración v1→v2 y preservación de campos desconocidos.
- APK debug compilado correctamente.
- Android Lint sin errores; los avisos restantes corresponden a versiones de SDK/dependencias mantenidas deliberadamente alineadas con el proyecto de referencia.
