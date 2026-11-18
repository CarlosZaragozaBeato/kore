# Inicio de Kore Android

## Objetivo

Iniciar la aplicación móvil tomando como referencia la arquitectura activa de Muskop: núcleo Kotlin puro, interfaz Compose, sesión como fuente de verdad y almacenamiento privado sin backend ni base de datos.

## Implementación

- Proyecto reproducible `kore_android/` con wrapper Gradle 8.14.3, AGP 8.13.0, Kotlin 2.2.21, SDK 36 y bytecode 17.
- Módulo `session-core` con sesión Kore v1, fábrica, importador, inventario y guardado atómico.
- El importador conserva campos desconocidos, valida el sobre completo y rechaza versiones futuras antes de persistir.
- Módulo `app` con Compose, ViewModel, repositorio Android y selectores nativos de importación/exportación.
- Pantalla inicial para crear perfil y visualizar objetivo, plan activo, inventario personal y estado Suunto.
- La sesión activa vive en almacenamiento privado. Android Backup está desactivado; la portabilidad se realiza mediante exportación explícita.

## Verificación

Comando ejecutado con JDK 21 y el SDK Android local:

```bash
./gradlew :session-core:test :app:assembleDebug
```

Resultado: `BUILD SUCCESSFUL`; cuatro pruebas del contrato Kotlin correctas, APK debug generado y `git diff --check` correcto.

## Límites y siguiente paso

- No se ha instalado ni probado todavía el APK en un dispositivo o emulador.
- Aún no existen editor/generador de planes ni cliente Suunto en Android.
- El siguiente corte debe definir las mutaciones Kotlin del objetivo y plan activo, incluir el catálogo de sesiones como recurso Android y permitir crear el primer bloque desde la app.
- Las credenciales Suunto se abordarán después mediante entrada explícita y almacenamiento seguro separado de la sesión; nunca se compilarán en el APK.
