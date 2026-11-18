# Kore Android: instrucciones para agentes

## Objetivo

Construir una aplicación Android nativa, local-first y sin backend obligatorio. El archivo de sesión Kore es la fuente de verdad personal; los recursos estáticos viajan con la app y Suunto aporta entrenamiento remoto recuperable.

## Reglas

- La implementación activa vive en `kore_android/`.
- Usar Kotlin, Jetpack Compose y Material 3. No introducir Java, WebView, React Native ni base de datos.
- `session-core` es Kotlin puro y contiene contrato JSON, validaciones y mutaciones.
- `app` contiene UI, almacenamiento Android y adaptadores. No mutar JSON manualmente desde Compose.
- Preservar campos desconocidos, validar antes de sustituir una sesión y rechazar versiones futuras sin guardar.
- No incluir credenciales Suunto en el APK o la sesión. Los secretos futuros deberán almacenarse separados y protegidos en el dispositivo.
- No incluir `.env`, datos personales, FIT, exportaciones ni contenido de `data/`.
- Registrar implementaciones y decisiones en `worklog/`.

## Build

- Gradle Wrapper 8.14.3.
- JDK 21 y bytecode Java/Kotlin 17.
- compile/target SDK 36 y min SDK 24.

Validación mínima:

```bash
cd kore_android
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :session-core:test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:assembleDebug
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:lintDebug
```

Comprobar también pantalla estrecha, tema claro/oscuro y un dispositivo real cuando esté disponible.
