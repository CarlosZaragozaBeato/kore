# Arquitectura Android

## Módulos

```text
kore_android/
  session-core/   contrato JSON, validación y persistencia atómica
  app/            Compose, navegación y adaptadores Android
```

`session-core` no depende de Android. La interfaz consume sus proyecciones y operaciones; no mantiene un modelo paralelo.

## Persistencia

La sesión `*.kore.json` es la fuente canónica de perfil, actividades normalizadas, museo, marcas y zapatillas. FIT y muestras masivas serán caché privada recuperable desde el proveedor.

No existe PostgreSQL ni backend en esta rama. La aplicación usa almacenamiento privado y exportación/importación explícita. Android Backup está desactivado.

## Estado actual

El runtime ofrece el contrato de sesión v2, migración preservadora desde v1 y un mockup visual. Los adaptadores de proveedores y las mutaciones del archivo runner todavía no están conectados a las pantallas demostrativas.
