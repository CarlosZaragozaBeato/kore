# Kore Android

Aplicación Android nativa, local-first y sin backend obligatorio. Sigue la separación validada en Muskop:

- `session-core`: Kotlin puro, contrato JSON, validación y persistencia atómica.
- `app`: Jetpack Compose, almacenamiento privado Android e importación/exportación nativa.

La sesión activa se guarda como `active-session.kore.json` bajo el almacenamiento privado de la aplicación. Importar valida el documento completo antes de sustituirla. Campos desconocidos se conservan y versiones futuras se rechazan sin guardarse.

## Compilar

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :session-core:test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:assembleDebug
```

El APK debug queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Alcance actual

- Mockup navegable con Inicio, Museo, Marcas, Zapatillas y Ajustes.
- Archivo visual de sesiones especiales, PB actuales/históricos, rotación y vida útil del calzado y fuentes simuladas.
- Datos claramente marcados como demostración; no sincroniza ni modifica entrenamiento real.
- El núcleo mantiene creación, migración, reapertura, importación y exportación de una sesión Kore v2; el mock expone importación/exportación desde Ajustes.
- Persistencia local atómica y sin base de datos.

Todavía no incluye mutaciones reales de museo/PB/zapatillas ni adaptadores de proveedores. Las credenciales nunca se añadirán al archivo de sesión ni a recursos compilados del APK.
