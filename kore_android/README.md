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

- Navegación con Inicio, Museo, Rutas, Marcas y Zapatillas; Ajustes se abre desde Inicio.
- Archivo visual de sesiones especiales, PB actuales/históricos, rotación y vida útil del calzado y fuentes simuladas.
- El modo demostración está claramente marcado y no modifica datos; el archivo real se mantiene separado.
- El núcleo mantiene creación, migración, reapertura, importación y exportación de una sesión Kore v2; el mock expone importación/exportación desde Ajustes.
- Persistencia local atómica y sin base de datos.
- Onboarding, creación/reapertura de sesión y alta manual de rutas funcionales.
- Ciclo de vida funcional de zapatillas: alta, edición, kilometraje, límite, precio, fases de uso, cinco valoraciones y retirada al histórico.
- Registro de actividades canónicas y selección explícita para crear piezas de Museo con snapshot, relato, etiquetas y zapatilla vinculada.
- Marcas personales actuales e históricas con ritmo derivado, tipo de evidencia y vínculos a actividad/Museo.
- Configuración Suunto cifrada con Android Keystore, renovación segura del token y sincronización paginada de resúmenes hacia actividades canónicas.

Todavía no incluye FIT, vueltas, muestras, geometría GPS/GPX ni asignación automática de kilómetros a zapatillas. Las credenciales nunca se añaden al archivo de sesión ni a recursos compilados del APK.

La conexión directa es para una instalación personal con credenciales propias ya autorizadas. No debe distribuirse un `client_secret` compartido dentro de una aplicación pública.
