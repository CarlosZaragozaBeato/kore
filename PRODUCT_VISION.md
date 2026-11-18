# Visión de producto de Kore

## Propósito

Kore será una aplicación Android local-first para planificar, ejecutar y comprender el entrenamiento de carrera. El centro es un objetivo vigente, su bloque activo y la comparación diaria entre lo planificado y lo realizado en Suunto.

Toma de Muskop cuatro decisiones: interfaz móvil simple, sesión personal portátil, núcleo de dominio independiente de la interfaz y separación entre estado personal y catálogos de contenido. Kore será autónoma: no dependerá de PostgreSQL, FastAPI, Python ni de un backend propio para funcionar.

## Experiencia móvil

- **Inicio:** objetivo, día del bloque, sesión prevista, resultado Suunto y acción siguiente.
- **Plan:** semana actual por días, detalle bajo demanda y adaptaciones explicadas y confirmadas.
- **Progreso:** media relativa al objetivo, tendencia, dimensiones, cobertura y recomendaciones basadas en evidencia.
- **Histórico:** bloques, marcas, zapatillas, diagnósticos y entrenamientos destacados.
- **Ajustes:** sesión, importación/exportación, credenciales locales, sincronización y privacidad.

La sesión permite análisis transversales dentro o fuera de la aplicación. Las muestras masivas se leerán desde la caché privada o se recuperarán desde Suunto cuando sean necesarias.

## Fuentes de verdad

| Información | Fuente canónica |
|---|---|
| Perfil, objetivos, planes, ejecución, marcas, calzado, puntuaciones e índice de entrenamientos | Sesión personal de Kore |
| Catálogo y reglas de planificación | Recursos versionados incluidos en la aplicación |
| Detalle remoto recuperable de entrenamientos | Suunto Cloud API |
| FIT y muestras masivas descargadas | Caché privada prescindible de la aplicación |
| Tokens y claves aportados por el usuario | Almacenamiento seguro Android, nunca sesión ni APK |

## Planificación flexible

El generador compondrá planes mediante reglas y recursos limitados, no plantillas rígidas ni sesiones inventadas por un agente. Las entradas incluirán objetivo inmediato y final, tiempo disponible, nivel de entrada por capacidad, volumen, días, distribución de intensidad, debilidad prioritaria, sesiones obligatorias y restricciones.

Esto permite tanto una preparación tipo como un bloque corto concreto —por ejemplo, cuatro semanas con cuatro sesiones específicas— manteniendo carga, recuperación, progresión y explicación.

## Progreso y evaluación

- Objetivos medibles: distancia, tiempo o capacidad, con fecha opcional.
- Dimensiones de 0 a 100 únicamente respecto al objetivo activo.
- Foto actual, mejor foto comparable y tendencia, siempre con cobertura y versión.
- Pruebas diagnósticas y simulaciones como evidencia explícita.
- Marcas verificadas, volumen, continuidad, ritmos y tolerancia de carga.
- Recomendaciones que describan evidencia, incertidumbre y acción sugerida.

Las zapatillas y sus asignaciones viven en la sesión. Los entrenamientos normalizados también; el ID Suunto permite recuperar FIT y muestras que no resulte razonable duplicar.

## Suunto sin backend

Kore se plantea inicialmente como aplicación personal instalada directamente. Las credenciales no se compilan en el APK: el usuario las importa y Android las protege en almacenamiento ligado al dispositivo. La sesión nunca las exporta.

Suunto exige actualmente `client_id` y `client_secret` mediante Basic al intercambiar y renovar tokens, además de una subscription key para consultar entrenamientos. Su documentación no ofrece PKCE ni SDK móvil. El modo directo cumple el objetivo de no tener backend, pero no ofrece la protección de secreto necesaria para distribuir públicamente la aplicación. Antes de una publicación habrá que acordar con Suunto un flujo de cliente móvil o introducir un intermediario mínimo. Para uso personal, Kore mostrará el riesgo y facilitará rotar las claves.

Referencias oficiales: [inicio de integración](https://apizone.suunto.com/how-to-start) y [FAQ de autorización](https://apizone.suunto.com/faq).

## Secuencia de implementación

1. Aplicación Android y núcleo Kotlin de sesión v1 con importación/exportación atómica.
2. Plan activo completo y recursos de catálogo incluidos en la aplicación.
3. Cliente Suunto directo, almacenamiento seguro, sincronización incremental e índice normalizado en sesión.
4. Histórico de planes, zapatillas, marcas e hitos.
5. Modelo de objetivo y motor de puntuación versionado.
6. Generador por restricciones, sesiones diagnósticas y simulaciones.
7. Herramientas opcionales de análisis externo sobre una exportación de sesión.

La interfaz móvil y el contrato evolucionarán juntos desde el principio. No se mostrará una media general hasta definir evidencia, umbrales y comportamiento ante datos incompletos.
