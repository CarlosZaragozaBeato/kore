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

Las credenciales personales de Suunto se cifran con una clave AES-GCM no exportable de Android Keystore y viven en preferencias privadas separadas. El refresh token rotado se persiste antes de solicitar entrenamientos. La sesión solo recibe actividades canónicas e identificadores externos.

El cliente usa OAuth refresh y Workout API v3 directamente para esta instalación personal. No es el diseño de distribución pública: mientras Suunto exija `client_secret`, una aplicación publicada no debe incorporar secretos compartidos en el APK. La app activa core-library desugaring para usar `java.time` de forma segura desde Android 24.

## Estado actual

El runtime ofrece contrato v2, migración preservadora desde v1, rutas, ciclo de zapatillas, actividades canónicas, Museo, marcas y un adaptador Suunto de resúmenes. El kilometraje del calzado sigue manual hasta definir la asignación verificable de actividades; FIT y GPS permanecen pendientes.
