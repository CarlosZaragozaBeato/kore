# Sesión personal de Kore

La sesión de Kore es el almacenamiento canónico, portátil y versionado del corredor. La aplicación móvil funcionará sin base de datos ni backend: reconstruirá la experiencia personal desde este documento, los recursos incluidos en la aplicación y Suunto. La primera versión se identifica con `"koreSession": 1`.

## Qué contiene

| Sección | Contenido |
|---|---|
| `profile` | Identificador local, etiqueta y fechas. |
| `activeGoal` / `goalHistory` | Objetivo vigente y objetivos anteriores. |
| `activePlan` | Bloque activo completo: semanas, días, recursos parametrizados y resultados enlazados. |
| `planArchive` | Planes anteriores compactos con planificación y ejecución suficientes para revisarlos sin conexión. |
| `workouts` | Entrenamientos Suunto normalizados, métricas útiles, vueltas/intervalos necesarios y referencias a la fuente. |
| `personalRecords` | Marca, distancia, fecha y referencia a la evidencia. |
| `shoes` | Identidad, estado y asignaciones de actividades; el kilometraje se deriva de ellas. |
| `diagnostics` | Pruebas de velocidad, capacidad aeróbica, umbral, resistencia específica y simulaciones. |
| `ratings` | Foto actual e historial de puntuaciones relativas al objetivo, con versión y evidencia. |
| `pinnedWorkouts` | Entrenamientos que el usuario decidió destacar. |
| `milestones` | Hitos personales. |
| `sync` | Identidad Suunto, cursores, fechas y estado de sincronización; nunca secretos. |
| `settings` | Preferencias portátiles. |

El lector conserva campos desconocidos al importar y exportar. Una versión futura se rechaza sin reescribirla. La escritura es atómica dentro del almacenamiento privado de Android.

## Qué queda fuera

- El catálogo de sesiones, perfiles y reglas del generador. Son recursos versionados de la app y se referencian mediante ID y versión.
- Binarios FIT, respuestas originales, GPS punto a punto y muestras de alta frecuencia. Pueden mantenerse en caché privada y recuperarse desde Suunto.
- Credenciales, refresh tokens, client secret o subscription key. Se almacenan por separado mediante mecanismos seguros de Android y nunca se exportan.

No habrá PostgreSQL, API FastAPI ni servicio Python necesarios para usar Kore.

## Política de tamaño

La sesión conserva los datos semánticos necesarios para operar, analizar y visualizar sin conexión. Evita únicamente binarios y muestras masivas. Cada entrenamiento almacena resumen, laps relevantes y métricas derivadas junto a su ID Suunto. Cada día de plan guarda `resourceId`, `resourceVersion`, parámetros y resultado, sin copiar repetidamente la descripción estática del catálogo.

El histórico se puede compactar sin perder significado: los planes archivados mantienen sus días y resultados, pero no copias de recursos ni muestras crudas. Una exportación completa puede incluir opcionalmente adjuntos en un paquete, sin convertirlos en campos del JSON principal.

## Puntuaciones relativas al objetivo

Una puntuación no representa una condición física universal. Cada foto debe indicar `goalId`, dimensiones, versión del algoritmo, fecha, cobertura/confianza y referencias a evidencia. Las dimensiones iniciales previstas son base aeróbica, umbral, velocidad, resistencia específica y tolerancia/continuidad de carga.

La media general solo se mostrará cuando haya evidencia mínima suficiente. La tendencia compara fotos compatibles del mismo objetivo y versión. Una simulación lograda valida el objetivo; proponer el siguiente no debe cambiarlo sin confirmación.

## Implementación del contrato

El núcleo Kotlin puro de `kore_android/session-core` es la implementación canónica. Las herramientas externas futuras consumirán exportaciones del mismo contrato, sin convertirse en una dependencia de la aplicación.
