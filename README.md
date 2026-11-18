# Kore Android

Kore es una aplicación Android local-first para planificar, seguir y analizar entrenamiento de carrera con datos de Suunto. Esta rama contiene exclusivamente la aplicación móvil y su documentación; no incluye backend, PostgreSQL ni servicios Python.

## Estado

La versión actual es un mockup navegable con datos de demostración:

- Inicio con objetivo, sesión del día, carga y recomendación.
- Plan semanal por días y constructor visual de bloques.
- Progreso relativo al objetivo, capacidades y marcas.
- Histórico de bloques, entrenamientos y zapatillas.
- Ajustes, sesión local y conexión Suunto simulada.

## Compilar

```bash
cd kore_android
ANDROID_HOME=/home/carlos/Android/Sdk \
JAVA_HOME=/usr/lib/jvm/java-21-openjdk \
./gradlew :session-core:test :app:assembleDebug
```

El APK se genera en `kore_android/app/build/outputs/apk/debug/app-debug.apk`.

Consulta [kore_android/README.md](kore_android/README.md), [PRODUCT_VISION.md](PRODUCT_VISION.md), [KORE_SESSION.md](KORE_SESSION.md) y [ARCHITECTURE.md](ARCHITECTURE.md).
