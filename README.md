# Kore Android

Kore es el archivo personal de la vida de un runner: una aplicación Android local-first para conservar zapatillas, sesiones memorables y la evolución completa de marcas personales. Esta rama contiene exclusivamente la aplicación móvil y su documentación; no incluye backend, PostgreSQL ni servicios Python.

## Estado

La versión actual es un mockup navegable con datos de demostración:

- Inicio con resumen vital y recuerdos destacados.
- Museo de sesiones elegidas por el usuario.
- Rutas guardadas manualmente dentro de la sesión.
- Marcas personales actuales e históricas.
- Zapatillas, kilometraje, valoraciones y fases de uso.
- Ajustes, sesión local y fuentes externas simuladas.

La creación del archivo, el alta manual de rutas y el ciclo completo de zapatillas ya son funcionales. Museo y Marcas mantienen una demostración visual o estado vacío hasta sus siguientes cortes.

## Compilar

```bash
cd kore_android
ANDROID_HOME=/home/carlos/Android/Sdk \
JAVA_HOME=/usr/lib/jvm/java-21-openjdk \
./gradlew :session-core:test :app:assembleDebug
```

El APK se genera en `kore_android/app/build/outputs/apk/debug/app-debug.apk`.

Consulta [kore_android/README.md](kore_android/README.md), [PRODUCT_VISION.md](PRODUCT_VISION.md), [KORE_SESSION.md](KORE_SESSION.md) y [ARCHITECTURE.md](ARCHITECTURE.md).
