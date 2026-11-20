# Sincronización Suunto en Android

## Objetivo

Conectar una instalación personal de Kore con Suunto sin backend, PostgreSQL ni credenciales compiladas en el APK.

## Evidencia revisada

- La integración previa de la rama técnica ya había validado OAuth refresh y `/v3/workouts` con esta cuenta.
- La guía oficial exige access token y `Ocp-Apim-Subscription-Key`; el canje y la renovación usan `client_id` y `client_secret`: <https://apizone.suunto.com/how-to-start>.
- El FAQ oficial documenta `startTime`, `stopTime` y `timeOffsetInMinutes`: <https://apizone.suunto.com/faq>.
- La tabla oficial de actividades confirma los IDs de running, trail, cinta, pista y carrera vertical: <https://aspartnercontent.blob.core.windows.net/apizone/docs/Activities.pdf>.
- Se comprobaron únicamente las claves del último JSON privado local, sin imprimir valores personales; coinciden con los campos manejados por el adaptador.

## Implementación

- Formulario local para Client ID, Client Secret, Subscription Key y Refresh Token.
- Cifrado AES-GCM mediante una clave no exportable de Android Keystore.
- Credenciales fuera de la sesión Kore, de las exportaciones y del APK.
- Renovación del token y guardado del refresh rotado antes de consultar entrenamientos.
- Consulta paginada de Workout API v3 para 30 días o hasta 10 años.
- Detección de páginas repetidas para evitar bucles.
- Adaptador puro Suunto → `ImportedActivity` con fecha local, instante UTC, modalidad, duración, distancia, ascenso y FC media.
- Upsert idempotente por proveedor e identificador externo, preservando campos desconocidos.
- Core-library desugaring para soportar `java.time` desde Android 24.

## Límite de seguridad

Este flujo es válido para una instalación personal donde el propio usuario introduce sus credenciales. Una aplicación pública no debe compartir un `client_secret` dentro del APK; necesitaría un modelo OAuth móvil admitido por el proveedor o un servicio mínimo que custodie el secreto.

## Pendiente

- Probar la sincronización desde un dispositivo con las credenciales personales vigentes.
- Incorporar FIT y asignación verificable de actividades a zapatillas.

## Verificación

- Diez pruebas de `session-core`, incluida conversión Suunto e importación idempotente.
- APK debug compilado correctamente con compatibilidad Android 24.
- Android Lint: 0 errores; seis avisos de versiones mantenidas deliberadamente y cinco sugerencias de autoboxing Compose.
- No había un dispositivo Android conectado, por lo que la llamada real y Android Keystore quedan como prueba manual necesaria.
