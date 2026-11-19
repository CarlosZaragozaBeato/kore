# Primer flujo funcional: sesión y rutas

## Objetivo

Pasar del mockup a una aplicación que cree un archivo personal real y permita guardar el primer tipo de contenido: rutas.

## Implementación

- Onboarding para crear una sesión Kore, importar otra o explorar la demostración.
- Navegación principal con Rutas; Ajustes queda accesible desde Inicio para mantener cinco destinos inferiores.
- Modelo neutral `RunnerRoute` y entrada `RouteInput` en `session-core`.
- Alta manual con nombre, ubicación, distancia, superficie, desnivel y notas.
- Persistencia atómica mediante el repositorio Android y actualización inmediata de la UI.
- Listado real separado de los datos de demostración.
- `routes` es una adición compatible a sesión v2: archivos v2 anteriores se normalizan con lista vacía y v1 sigue migrando de forma preservadora.
- Las áreas aún no funcionales muestran estados vacíos en un archivo real y nunca mezclan ejemplos con datos personales.

## Verificación

- Seis pruebas de `session-core`, incluida alta de ruta y preservación de campos desconocidos.
- APK debug compilado correctamente.
- Android Lint: 0 errores; permanecen cinco avisos informativos por mantener versiones alineadas con el stack Android de referencia.

## Siguiente paso

Probar creación, cierre/reapertura y alta de varias rutas en dispositivo. Después implementar el alta real de zapatillas antes de conectar actividades externas.
