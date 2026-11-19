# Arquitectura Android

## Módulos

```text
kore_android/
  session-core/   contrato JSON, validación y persistencia atómica
  app/            Compose, navegación y adaptadores Android
```

`session-core` no depende de Android. La interfaz consume sus proyecciones y operaciones; no mantiene un modelo paralelo.

## Persistencia

La sesión `*.kore.json` es la fuente canónica de perfil, actividades normalizadas, museo, marcas, zapatillas y rutas. FIT, GPX completos y muestras masivas podrán ser adjuntos privados recuperables.

No existe PostgreSQL ni backend en esta rama. La aplicación usa almacenamiento privado y exportación/importación explícita. Android Backup está desactivado.

## Estado actual

El runtime ofrece contrato v2, migración preservadora desde v1, creación/reapertura del archivo, rutas persistentes y ciclo local completo de zapatillas. Museo, PB y proveedores todavía no tienen mutaciones reales; el kilometraje del calzado es manual hasta disponer de actividades asignables.
