# 2026-11-12 — Revisión inicial

**Estado:** revisión de código completada; extracción real y persistencia aún no verificadas.

## Objetivo acordado

Extraer información de Suunto, conservarla localmente y procesarla para consultas, visualización y apoyo de un agente en decisiones de entrenamiento. El backend futuro no será Java.

## Situación observada

- Existe un servicio Quarkus/Java que consulta entrenamientos de Suunto, ejecuta una sincronización horaria y guarda resúmenes JSON en PostgreSQL local.
- Hay endpoints para listar entrenamientos, obtener un resumen y lanzar una sincronización manual.
- No hay todavía ingestión de archivos FIT, series detalladas, procesos de tratamiento ni interfaz de consulta específica para agentes.
- Se ha creado un `.env` local para las credenciales facilitadas y un enlace desde el directorio del servicio actual. Ambos están excluidos de Git y no forman parte de este historial.

## Problemas abiertos

1. `services/suunto-service/src/main/resources/application.properties` combina `drop-and-create` con Liquibase. Hay que impedir que el arranque destruya los datos locales.
2. `SuuntoAuthTokenProvider` conserva el refresh token renovado solo en memoria. Hay que persistirlo de forma segura.
3. `SuuntoSyncScheduler` carece de pruebas sobre paginación, recuperación tras errores y continuidad del cursor.
4. Solo se conserva el resumen JSON; para análisis detallado hacen falta los FIT o una fuente equivalente de muestras.
5. Falta comprobar con la cuenta real qué permisos y endpoints de Suunto están disponibles. La comprobación local del JWT y el plan de validación constan en `2026-11-12-integracion-suunto.md`.

## Dirección técnica inicial

Evaluar la sustitución del servicio Java por un backend Python, PostgreSQL local y una API FastAPI. Separar la ingestión del tratamiento de datos y conservar el dato original para permitir reprocesarlo. Validar esta dirección con una primera extracción real antes de ampliar la arquitectura.

## Verificación y siguiente paso

La revisión fue estática; no se ejecutó el servicio ni se consultó Suunto. El siguiente paso es validar el acceso a Suunto y diseñar una importación recuperable que preserve los datos locales.
