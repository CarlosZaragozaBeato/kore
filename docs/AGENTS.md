# CCollector para agentes

Guía para que un agente de IA lea y escriba datos de CCollector. Todo es JSON
sobre HTTP; los datos son tuyos y portables.

## Empezar aquí

1. **Manifest** — `GET /api/v1/agent/manifest` (sin sesión): mapa de todos los
   recursos y cómo leerlos/escribirlos. Empieza siempre por aquí.
2. **OpenAPI** — `GET /q/openapi` (spec) y `/q/swagger-ui` (UI interactiva):
   contrato completo y tipado de cada endpoint.
3. **Contexto** — `GET /api/v1/agent/context` (con sesión): toda la sesión del
   usuario (mismo formato que el export) + el resumen analítico, en una llamada.

## Sesión

No hay autenticación real (la app es de uso propio y no se expone). Cada
petición que lee/escribe datos del usuario lleva el header:

```
X-CCollector-Username: <username>
```

Para crear/recuperar una sesión: `POST /api/v1/auth/login` con
`{"username": "..."}`.

## Convención de respuestas

La mayoría de endpoints envuelven la respuesta:

```json
{ "success": true, "data": { ... }, "error": null }
```

Excepciones (documento crudo, sin envoltorio): `GET /session/export`,
`GET /analytics/summary`, `GET /analytics/correlations`, `GET /recipes/recommend`,
`GET /agent/manifest`, `GET /agent/context`.

## Recursos

Todos siguen el mismo patrón CRUD bajo `/api/v1` (ver el manifest para las rutas
exactas). El **POST de cada recurso sirve también para "importar"** un recurso
generado por un agente.

| Recurso | Ruta | Notas |
|---|---|---|
| Entrenamientos | `/workouts` | origen `MANUAL` o `SUUNTO`; ritmo, cadencia (`avgCadenceSpm`) y zancada (`strideLengthMeters`) derivados de `stepCount` |
| Planes | `/plans` | sesiones planificadas (con `steps` estructurados: calentamiento/serie/recuperación/rodaje/vuelta a la calma, ritmo min/max, FC, repeticiones, descanso) + adherencia calculada |
| Sesiones de calendario | `/planned` | sesiones planificadas sueltas (sin plan) por día, con `steps`, `status` (PROPOSED/ACCEPTED/REJECTED) y variantes (`variantGroup`/`variantLabel`); `POST /planned/{id}/accept` acepta una variante y descarta el resto de su grupo, `POST /planned/{id}/reject` la descarta; `GET /planned/{id}/comparison` contrasta lo planificado con el entreno realizado del día (distancia/duración/ritmo/FC vs objetivo, con banda de adherencia BELOW/WITHIN/ABOVE) |
| Periodización | `/blocks` | bloques macro/meso/micro anidados por `parentId` (`level`, `focus`, rango de fechas, `loadStance` opcional); el `POST` admite hijos anidados (`children`) para crear una temporada de una vez; `GET /blocks/{id}/summary` resume planificado vs realizado en el rango del bloque (relación con las sesiones derivada por fechas) |
| Ejercicios | `/exercises` | catálogo con `category` (WARMUP/STRENGTH/RECOVERY), material, `imageUrl` (imagen/animación de ejecución), `instructions` (cómo) y `metValue` (MET → gasto energético); `POST /exercises/seed` carga ejemplos |
| Ingredientes | `/ingredients` | catálogo con nutrición por 100 g/ml e `imageUrl`; `POST /ingredients/seed` carga ejemplos |
| Rutinas | `/routines` | ejercicios con series/reps/descanso |
| Plantillas de sesión | `/session-templates` | sesiones específicas por disciplina/objetivo (maratón, media, 10k, 5k)/nivel; `POST /session-templates/seed` carga ejemplos |
| Sesiones de fuerza | `/strength-sessions` | `status` PLANNED (planificada para un día del calendario) o DONE (realizada; por defecto) |
| Recetas | `/recipes` | ingredientes (con `ingredientId` opcional al catálogo), pasos, macros. `GET /recipes/recommend` sugiere recetas según el contexto (entreno/actividad/objetivo); `GET /recipes/by-ingredient/{id}` lista las que usan un ingrediente |
| Dietas | `/diet-plans` | objetivos + comidas por día |
| Peso | `/weight` | mediciones (upsert por fecha) + objetivo `/weight/goal` (rango + kcal/día) |
| Actividad diaria | `/activity/daily` | pasos y quema total del día (upsert por fecha); la quema diaria sustituye la estimación por entrenos en el balance |
| Energía | `/energy/summary` | solo lectura: balance kcal consumidas vs quemadas, diario y semanal |
| Analítica | `/analytics/summary` · `/analytics/compare` · `/analytics/correlations` | solo lectura: resumen (totales + series + carga), comparativa de carga entre rangos (por día/acumulada, desglose por disciplina/origen, señales ACWR/monotony/ramp) y correlaciones transversales semana a semana (entreno × nutrición × actividad × peso; Pearson por par + interpretación) |
| Ajustes Suunto | `/suunto/settings` | los secretos nunca se devuelven |
| Sync Suunto | `/suunto/sync` · `/suunto/purge` | `POST sync` importa nuevos y **rellena** los ya importados (`imported`/`updated`/`skipped`); `POST purge` aplica la retención mínima |

## Import / export de sesión completa

- **Export** — `GET /api/v1/session/export`: vuelca usuario + workouts + planes +
  gimnasio + nutrición a un único documento portable. No incluye ids de base de
  datos ni las credenciales de Suunto.
- **Import** — `POST /api/v1/session/import`: reconstruye una sesión desde ese
  documento en un dispositivo nuevo. Rechaza con 409 si el username ya existe.

### Versionado del esquema

El documento lleva `schemaVersion`. Versión actual: **15**.

| Versión | Añade |
|---|---|
| 1 | usuario + workouts |
| 2 | planes de entrenamiento |
| 3 | gimnasio (ejercicios, rutinas, sesiones de fuerza) |
| 4 | nutrición (recetas, dietas) |
| 5 | workouts: `maxHeartRate` (ppm) y `energyKcal` (kcal); nuevos tipos `CYCLING`/`SWIMMING` |
| 6 | catálogo de `ingredients` (nutrición por 100 g/ml); ejercicios con `category` (WARMUP/STRENGTH/RECOVERY) y `requiresEquipment`; línea de receta con `ingredientId` opcional al catálogo |
| 7 | registro de peso (`weightEntries`) y objetivo de mantenimiento (`weightGoal`: rango + kcal/día) |
| 8 | plantillas de sesión (`sessionTemplates`): disciplina + objetivo (maratón/media/10k/5k) + nivel |
| 9 | workouts: `stepCount` (pasos); de él se derivan cadencia (pasos/min) y zancada (m/paso) al leer |
| 10 | actividad diaria (`dailyActivities`): pasos y quema total del día (la quema diaria manda en el balance) |
| 11 | ejercicios con `imageUrl`/`instructions`/`metValue`; sesiones de fuerza con `status` (PLANNED/DONE) para planificar gimnasio por día |
| 12 | catálogo de ingredientes con `imageUrl` |
| 13 | sesiones planificadas con `steps` estructurados (`kind` WARMUP/INTERVAL/RECOVERY/STEADY/COOLDOWN, `repeat`, distancia/tiempo objetivo, ritmo `targetPaceMin/MaxSecPerKm`, FC `targetHrMin/Max`, `recoverySeconds`, `note`) |
| 14 | sesiones planificadas sueltas del calendario (`calendarSessions`): sin plan, con `steps`, `status` (PROPOSED/ACCEPTED/REJECTED) y variantes (`variantGroup`/`variantLabel`) |
| 15 | periodización (`trainingBlocks`): árbol macro/meso/micro con `focus`, rango de fechas, `loadStance` y `note`; los hijos viajan anidados en `children` |

Es **retrocompatible**: un documento de versión menor se importa sin las
secciones que no incluya. El import rechaza versiones mayores que la soportada.

## Kore Data Language (KDL) — recursos sueltos

Para insertar/extraer **recursos individuales o en lote** (no la sesión entera),
usa KDL: un JSON con envoltorio común por recurso.

```json
{
  "kore": "kdl",
  "kdlVersion": 1,
  "items": [
    { "koreType": "ingredient", "schemaVersion": 1, "payload": { "name": "Avena", "baseUnit": "GRAM", "calories": 389 } },
    { "koreType": "workout", "schemaVersion": 1, "payload": { "date": "2026-07-15", "type": "RUNNING", "distanceMeters": 10000, "durationSeconds": 3600 } }
  ]
}
```

- **Export** — `GET /api/v1/kdl/export` (todo) o `?types=workout,ingredient`
  (filtrado). Documento crudo, sin ids de BD.
- **Import** — `POST /api/v1/kdl/import`: inserta el lote en la sesión actual.
  Cada item se procesa **aislado**: uno inválido no aborta el resto; la
  respuesta trae `imported`, `skipped`, `byType` y `errors`.
- **Import por sección** — `POST /api/v1/kdl/import/{type}`: el cuerpo es uno o
  varios recursos de ese `type` — un **payload suelto**, un **array** de
  payloads, o un **documento KDL** completo. Pensado para importar algo generado
  por una IA directamente en su sección (p. ej. `POST /kdl/import/plan` con el
  JSON de un plan de 8 semanas). Misma respuesta que `import`.
- **Recursos compuestos**: un `plan` ya lleva todas sus sesiones por día; un
  `dietPlan` sus comidas; un `recipe` sus ingredientes; un `routine` sus
  ejercicios. Es decir, un "plan completo" se importa como **un solo item**.
- **Tipos (`koreType`)**: `workout`, `plan`, `planned`, `block`, `exercise`,
  `ingredient`, `recipe`, `routine`, `dietPlan`, `weightEntry`,
  `sessionTemplate`, `dailyActivity`. El `payload` es la forma de creación de ese
  recurso (el mismo cuerpo que su `POST`). `planned` son sesiones sueltas de
  calendario: un **array** importa una semana/mes de golpe, con **varios entrenos
  por día** y **variantes** (mismo `variantGroup`). `block` es un bloque de
  periodización: sus hijos van anidados en `children`, así que una **temporada
  entera** (macro con sus mesos y micros) se importa como **un solo item**.
- **Convivencia entre versiones**: cada tipo lleva su `schemaVersion` (hoy todos
  en 1); los campos desconocidos se ignoran (compatibilidad hacia adelante para
  cambios aditivos) y se rechaza un `schemaVersion` mayor que el soportado. Los
  migradores por tipo se enganchan cuando un tipo evolucione.
- **Nota**: al importarse por el `POST` del recurso, el origen no viaja — un
  `workout` de Suunto se reinserta como `MANUAL`. Para copia fiel de una sesión
  completa usa el export/import de sesión.

## Flujos típicos de agente

- **Generar un plan**: `GET /agent/context` → razona sobre workouts y carga →
  `POST /api/v1/plans` con el plan generado (nombre, fechas, sesiones).
- **Analizar carga y sugerir descarga**: lee `analytics` del contexto (series
  semanal de `load`) → propone ajustes.
- **Proponer dieta**: según volumen/objetivo → `POST /api/v1/diet-plans` con
  objetivos y comidas; y `POST /api/v1/recipes` para las recetas nuevas.
- **Backup/restore o migración completa**: `GET /session/export` →
  `POST /session/import`.

## Extracción de Suunto (referencia)

La Cloud API de Suunto (`/v2/workouts`) devuelve `{"payload": [ ... ]}`. Campos
que consumimos (validados contra la documentación oficial y una carga real):

| Campo Suunto | Interno | Unidad / nota |
|---|---|---|
| `workoutKey` | `sourceId` | id de origen, para deduplicar |
| `activityId` | `type` | entero; se mapea a disciplina (ver abajo) |
| `startTime` | `date` | epoch-millis |
| `totalDistance` | `distanceMeters` | metros |
| `totalTime` | `durationSeconds` | segundos |
| `energyConsumption` | `energyKcal` | kcal |
| `stepCount` | `stepCount` | pasos; deriva cadencia (`stepCount`/min) y zancada (`distance`/`stepCount`) |
| `hrdata.workoutAvgHR` | `avgHeartRate` | ppm — **anidado** |
| `hrdata.workoutMaxHR` | `maxHeartRate` | ppm — **anidado** |

**FC anidada:** la frecuencia cardiaca va dentro de `hrdata`, no como campo
plano. Un intento previo con `hravg` dejaba la FC siempre a null.

**Mapeo de `activityId` → disciplina** (tabla oficial de Suunto, ~120 ids). Solo
distinguimos lo que la app pinta con color propio; el resto cae en `OTHER`:
`RUNNING` = 1, 22, 53, 59, 60, 103, 115 · `CYCLING` = 2, 10, 52, 99, 105, 106,
109, 114 · `SWIMMING` = 21, 85, 90 · `STRENGTH` = 20, 23, 54, 63, 104.

Punto único de mapeo: `SuuntoWorkoutMapper`; contrato del payload:
`SuuntoWorkout`. Un re-sync **rellena** (backfill) los entrenos ya importados por
`sourceId` — FC, tipo, kcal y pasos que faltaran — además de añadir los nuevos;
el resumen distingue `imported` de `updated`.

**Retención mínima (ROADMAP v3, principio 3):** los datos de Suunto son
recuperables desde su nube, así que no se guardan indefinidamente. `POST
/api/v1/suunto/purge` borra los entrenos de Suunto anteriores a la ventana
(`kore.suunto.retention-months`, por defecto 1 → mes actual + anterior) y nunca
toca los entrenos `MANUAL`. Splits/laps y series por muestra requieren descargar
el FIT del entreno (no vienen en el resumen `/v2/workouts`): pendiente.
