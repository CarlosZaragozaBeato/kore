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

- Mockup navegable con Inicio, Plan, Progreso, Historial y Ajustes.
- Constructor visual de plan, semana diaria, estado relativo al objetivo, marcas, zapatillas y conexión Suunto simulada.
- Datos claramente marcados como demostración; no sincroniza ni modifica entrenamiento real.
- El núcleo mantiene creación, reapertura, importación y exportación de una sesión Kore v1; el mock expone importación/exportación desde Ajustes.
- Persistencia local atómica y sin base de datos.

Todavía no incluye editor/generador de planes ni conexión Suunto. Las credenciales nunca se añadirán al archivo de sesión ni a recursos compilados del APK.
