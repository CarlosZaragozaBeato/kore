# ROADMAP v4 — Kore

> **Kore** como plataforma de **planificación tipo TrainingPeaks**. Tras cerrar
> v3 (calendario, Suunto profundo, análisis transversal, identidad de marca),
> v4 se **enfoca en cuatro secciones**: **Inicio · Calendario · Entrenos ·
> Planes**. El resto de secciones (Gimnasio, Nutrición, Peso, Análisis) quedan
> como están, sin trabajo nuevo en este ciclo.
>
> La tesis de v4: pasar de "registrar y ver" a "**planificar y contrastar**".
> Quiero planificar semanas / meses / años importando JSON, **planificar
> entrenos estructurados por día** (p. ej. `4 km cal + 200 m ×5 /1'`), tener
> **variantes** entre las que elegir según la situación, y **comparar lo
> planificado con lo realizado** usando los datos de Suunto (ritmo max/min/med,
> FC, repeticiones, distancia). Todo sigue siendo de uso propio, sin nube,
> portable en JSON y preparado para agentes.
>
> Mantiene los principios de siempre (uso personal, sin exposición, datos tuyos,
> credenciales Suunto cifradas y nunca exportadas, retención mínima).

## Prioridad de las fases v4

| #   | Fase | Prioridad | Idea |
| --- | ---- | --------- | ---- |
| A   | Navegación compacta + logo | **P0** ✅ | Cabecera en dos filas, logo propio alineado, pestañas limpias. |
| B   | Modelo de entreno estructurado (pasos/steps) | **P0** ✅ | Base de datos: una sesión planificada = pasos (cal/serie/rec/vuelta) con objetivo de distancia/tiempo, ritmo min/max, zona FC, repeticiones. Export/import versionado. |
| C   | Planificar por día + variantes | **P1** ✅ | En el calendario, proponer una o varias sesiones para un día (variantes/recomendaciones), con **aceptar/descartar**. Estado propuesta→aceptada. |
| D   | Comparación planificado vs realizado | **P1** ✅ | Emparejar el entreno de Suunto con la sesión planificada del día y calcular adherencia: ritmo real vs objetivo (min/max/med), FC, distancia, repeticiones completadas. Vista de comparación. |
| E   | Import mejorado + inteligencia de planes | **P2** ✅ | Import JSON con varios entrenos por día y por secciones; variantes de plan (descarga / subida de carga); sugerir variante según adherencia/tendencia de la semana. |
| F   | Periodización larga (semanas/meses/años) | **P2** ✅ | Bloques macro/meso/micro sobre el calendario para planificar a largo plazo. |
| ∞   | Mejora de frontend | **Continua** | Apartado fijo: seguir puliendo lo visual dentro de las 4 secciones foco. |

## Fases v4

### Fase A — Navegación compacta + logo 🎨 · P0 ✅

Objetivo: barra de navegación más compacta, simple y limpia; y arreglar la
alineación del logo (no estaba centrado, colgaba dentro de la tira de pestañas).

- [x] Cabecera en **dos filas**: fila 1 = logo (a la izquierda, alineado) +
      acciones (tema/exportar/salir como iconos); fila 2 = tira de pestañas
      dedicada, separada por una línea sutil.
- [x] Logo fuera de `<nav>`, como elemento de marca propio y centrado
      verticalmente en su fila.
- [x] Pestañas más compactas (menos padding, tipografía `--step--1`), acciones
      secundarias reducidas a iconos (`download`, `logout`) para aligerar.

**Entregable:** ✅ cabecera limpia con el logo bien alineado y pestañas
compactas. Verificado: lint + build frontend en verde.

### Fase B — Modelo de entreno estructurado (pasos) 🧱 · P0 ✅ (2026-07-24)

Fundamento de C y D. Una sesión planificada deja de ser texto libre y pasa a ser
una lista de **pasos** con objetivos medibles. Sin esto no hay comparación real.

- [x] Dominio: `PlannedStep` (tipo `StepKind`: calentamiento/serie/recuperación/
      rodaje/vuelta a la calma; distancia o tiempo objetivo; ritmo min/max en
      s/km; FC min/max; repeticiones `repeat` y descanso `recoverySeconds`;
      `note`). Migración Liquibase `015-planned-session-steps.yaml` (tabla hija
      `planned_session_steps`), verificada en `local.db`.
- [x] DTOs (`PlannedStepDTO`, `PlanRequest.StepRequest`) + validación
      (no negativos, `repeat`≥1) + export/import versionado (`schemaVersion`
      12→13; `ExportPlannedStep`).
- [x] Frontend: editor de pasos plegable por sesión en `PlanForm` (ritmo en
      mm:ss/km, distancia en m, descanso en s); resumen compacto de pasos
      (`src/steps.ts` → `formatStep`) en la vista de plan y en el detalle de día
      del calendario.

**Entregable:** ✅ planificas una sesión estructurada (p. ej. `4 km cal + 200 m
×5 /1'`) con objetivos medibles, portable en JSON. Verificado: 82 tests backend
en verde (schema v13, incl. roundtrip de pasos), migración aplicada, lint +
build frontend limpios.

### Fase C — Planificar por día + variantes 📆 · P1 ✅ (2026-07-24)

Planificación suelta en el calendario, reutilizando el modelo de pasos de la
Fase B. Una sesión puede vivir sin plan, con estado y variantes por día.

- [x] `PlannedSession` ahora puede no tener plan (`planId` nullable) y lleva
      `userId`, `status` (`SessionStatus` PROPOSED/ACCEPTED/REJECTED) y
      `variantGroup`/`variantLabel`. Migración `016` por **reconstrucción de
      tabla** (SQLite no soporta `dropNotNullConstraint`): crea/copia/renombra
      preservando ids y backfill de `user_id` desde el plan. Verificada en
      `local.db`.
- [x] Endpoints `/api/v1/planned`: CRUD de sesiones sueltas + `POST /{id}/accept`
      (acepta una variante y **descarta las hermanas** del mismo grupo) y
      `POST /{id}/reject`. Servicio `PlannedSessionService` + `PlannedStepWriter`
      (reutilizado por `PlanService`).
- [x] Export/import portable (`calendarSessions`, `schemaVersion` 13→14).
- [x] Frontend: en el Calendario, botón **"Planificar sesión"** por día →
      `DayPlanner` (con `StepsEditor` reutilizable); las sesiones se muestran en
      el detalle del día con **badge de estado**, agrupadas por variante, con
      botones Aceptar/Descartar/Variante/Editar/Eliminar. Marcador de pendientes
      en la celda del día.

**Entregable:** ✅ planificas un día en el calendario (p. ej. el martes las
series), propones **variantes** ("Carga normal" vs "Descarga") y eliges una con
un botón; el resto se descarta. Verificado: 86 tests backend (incl.
`PlannedSessionResourceTest`: variantes y roundtrip), migración aplicada, lint +
build frontend limpios.

### Fase D — Comparación planificado vs realizado 🔬 · P1 ✅ (2026-07-25)

Empareja la sesión planificada de un día con el entreno realizado (Suunto o
manual) de esa fecha y contrasta las métricas contra los objetivos de sus pasos.
Cálculo **derivado** (no persiste nada): sin migración ni bump de esquema.

- [x] `ComparisonService`: agrega objetivos de la sesión/pasos (distancia,
      duración, banda de ritmo, banda de FC, nº de repeticiones), empareja el
      entreno del día por tipo y distancia más cercana, y clasifica cada métrica
      en una banda `AdherenceBand` (BELOW/WITHIN/ABOVE/NO_DATA) con tolerancia
      (±5 % distancia, ±10 % duración; ritmo/FC contra su rango objetivo).
- [x] Endpoint `GET /api/v1/planned/{id}/comparison` → `ComparisonDTO`
      (target + actual + métricas). Descrito en el manifiesto del agente.
- [x] Nivel de comparación **a sesión**: el entreno guardado es un resumen (sin
      parciales por vuelta), así que se contrastan totales y medias, no cada
      repetición. Se informa el nº de series planificadas como contexto.
- [x] Frontend: en el detalle del día, botón **"Comparar"** en cada sesión de
      calendario cuando hay entreno ese día → `SessionComparison` (tabla
      objetivo/realizado/Δ con píldoras de banda coloreadas por métrica).

**Entregable:** ✅ planificas el martes (4 km cal + series a ritmo objetivo),
sincronizas Suunto, y ves de un vistazo si cumpliste: distancia, duración, ritmo
medio dentro de banda y FC, con desviación en % y color. Verificado: 88 tests
backend (incl. `PlannedSessionResourceTest`: emparejado con banda y caso sin
entreno), lint + build frontend limpios.

### Fase E — Import mejorado + inteligencia de planes 📥 · P2 ✅ (2026-07-25)

Importar planificaciones enteras por JSON (varios entrenos por día + variantes) y
que la app **sugiera** cómo ajustar la carga según cómo va la semana.

- [x] **Import por lote de calendario**: KDL gana el tipo `planned` (export +
      import). `POST /api/v1/kdl/import/planned` acepta un array → importa una
      semana/mes de días de calendario, **varios entrenos por día** y **variantes**
      (mismo `variantGroup`). Reutiliza `PlannedSessionService.create`.
- [x] **Recomendación de carga**: `GET /api/v1/planned/recommendation` →
      `{stance, label, reason, signals}` con postura DELOAD/MAINTAIN/BUILD según
      ACWR/monotonía/rampa (reutiliza `LoadComparisonService.signals`, ahora
      público). RISK → descarga; ACWR<0.8 sin rampa alta → subir; resto mantener.
- [x] **Generación de variantes**: `POST /api/v1/planned/{id}/variants` crea
      hermanas **Descarga** (−30 %) y **Subida de carga** (+10 %) de la sesión
      (base queda "Carga normal"), escalando volumen y **manteniendo intensidad**
      (en series cambia el nº de repes; en lo continuo, distancia/tiempo). Devuelve
      el grupo + la recomendación. `PlanIntelligenceService`.
- [x] Frontend: en el Calendario, **banner de recomendación** de carga de la
      semana (color por postura) y botón **"Sugerir variantes"** por sesión que
      genera Descarga/Subida y avisa de la postura recomendada.

**Entregable:** ✅ importas un mesociclo en un JSON (con variantes por día) y, según
tu ACWR/monotonía/rampa, la app te dice si esta semana toca descargar, mantener o
subir — y te genera las variantes de un clic para elegir. Verificado: 91 tests
backend (variantes, recomendación, import de `planned` por lote), lint + build
frontend limpios.

### Fase F — Periodización larga 🗓️ · P2 ✅ (2026-07-26)

Estructura de temporada **macro/meso/micro** sobre el calendario, apoyada en el
import por lote de la Fase E.

- **Modelo** `TrainingBlock` (migración **017**): árbol auto-referenciado por
  `parentId` (macro→meso→micro), con `level` (enum `BlockLevel`), `focus` (enum
  `BlockFocus`: BASE/BUILD/PEAK/TAPER/RECOVERY/RACE/GENERAL), rango de fechas,
  `loadStance` opcional (reusa `LoadStance`) y nota. La relación con las
  sesiones es **derivada por solape de fechas** (sin FK).
- **API** `/api/v1/blocks`: GET (árbol anidado), POST (un bloque con hijos
  anidados → una temporada de una vez, o colgar de un `parentId`), PUT, DELETE
  (cascada a descendientes). `GET /api/v1/blocks/{id}/summary` resume
  **planificado vs realizado** en el rango (sesiones no descartadas vs entrenos:
  nº, distancia, adherencia %). Validación: nivel del hijo más fino que el padre.
- **Portabilidad**: export/import añade `trainingBlocks` (árbol con hijos
  anidados, sin ids) → schemaVersion **14→15**. KDL gana el tipo `block`
  (`POST /api/v1/kdl/import/block`, reusa `BlockRequest` con `children`).
  Manifiesto de agente actualizado (recurso `blocks` + tipo KDL `block`).
- **Frontend**: panel **Periodización** en el Calendario — miga de pan del
  bloque activo del día (macro › meso › micro con chip de foco), árbol de
  bloques con resumen bajo demanda y borrado en cascada, y formulario de alta
  (nivel/foco/carga/contenedor/fechas). `Periodization.tsx` + estilos.
- **Cuidado**: los entrenos no admiten fecha futura (validación de workout), a
  diferencia de las sesiones planificadas; el resumen de un bloque futuro tendrá
  0 realizados por diseño.

98 tests backend (6 nuevos en `TrainingBlockResourceTest` + 1 en
`KdlResourceTest`; 7 asserts de `schemaVersion` subidos a 15). **Esquema export
ahora en v15.**

---

# Historial — ROADMAP v3 — Kore ✅ completado

> **Kore** (antes *CCollector*). App personal de entrenamiento y **control de
> vida** para atletas de running, triatlón e ironman. Mantiene la filosofía de
> siempre — uso propio, sin nube, datos tuyos y portables en JSON, preparada
> para agentes.
>
> v3 da el salto de "registrar" a "**vivir dentro de la app**": un **calendario**
> como columna vertebral (histórico + planificación), una pantalla de **inicio
> que enlaza cada métrica con su sección**, **extracción Suunto en profundidad**
> (splits, cadencia, zancada, FC, pasos y quema diaria), **análisis por tipo de
> entreno**, importación de planes generados por IA **por secciones**, y un
> gimnasio/nutrición más ricos y conectados con peso, actividad y entrenamiento.
>
> Los **ROADMAP v2 y v1**, ya completados, se conservan íntegros más abajo como
> historial.

## Principios de v3

Decisiones transversales que condicionan todas las fases:

1. **El calendario es el eje.** Casi todo (inicio, planes, gimnasio, sesiones)
   se consulta y se planifica desde el calendario, por día y por semana.
2. **Todo enlaza con su sección.** Ningún dato del inicio es un callejón sin
   salida: cada métrica lleva a la vista que la desglosa (km → calendario
   filtrado por semana; kcal → energía/peso; carga → análisis).
3. **Retención mínima de datos.** No queremos un almacén grande. Lo que la app
   **genera** (planes, recetas, dietas, peso, plantillas) sí se guarda. Los
   datos de **Suunto** son recuperables desde su nube, así que **no** se
   persisten a largo plazo: se mantiene solo lo **reciente** (mes actual +
   anterior) y se permite **cargar históricos puntuales** para un análisis
   concreto sin dejarlos almacenados.
4. **Import/export por sección.** Importar un plan de running se administra en
   Planes; una dieta en Nutrición; etc. Además, un panel en **Ajustes** lista y
   gestiona todo lo importado.

## Identidad de marca (files.zip)

Fuente: `kore-paleta.md`, `kore-icono*.svg`, `kore-logotipo*.svg` (raíz del repo).

- **Nombre:** Kore. **Concepto visual:** anillos concéntricos = disciplinas del
  triatlón; núcleo naranja = "hoy / meta / latido".
- **Paleta:** núcleo azul noche `#0E1B2A` (estructura), núcleo elevado `#132537`
  (tarjetas), natación `#17C3B2`, ciclismo `#3A86FF`, carrera/acción `#FF7A1A`,
  hueso `#EAF2F8` (texto claro), gris azulado `#7C93A8` (texto atenuado).
  Estado: éxito `#2FBF71`, aviso `#F5B300`, alerta `#E5484D`.
- **Código de disciplina** (transversal en toda la UI): natación turquesa,
  ciclismo azul, carrera naranja.

---

## Prioridad de las fases v3

El orden lo marco yo por dependencia y valor. El calendario y la extracción
Suunto son cimientos: casi todo lo demás se apoya en ellos.

| Fase | Tema | Prioridad | Por qué ahí |
|------|------|-----------|-------------|
| 17 | Calendario (nueva sección) | **P0** | Eje de la app; lo consumen inicio, planes, gimnasio y sesiones. |
| 18 | Extracción Suunto en profundidad (splits, cadencia, zancada, FC, pasos, quema diaria) | **P0** | Cimiento de datos para Entrenos, Peso y Análisis. Define la política de retención. |
| 19 | Inicio conectado (fila de la semana + enlaces a sección) | **P1** | Primera pantalla; depende de 17 (fila semanal) y de tener métricas que enlazar. |
| 20 | Entrenos: detalle, splits y análisis por tipo | **P1** | Desglose de un entreno y comparativa entre entrenos afines. Depende de 18. |
| 21 | Importación por secciones (KDL v2: planes, sesiones, gimnasio, nutrición) | **P1** | "Pide a una IA un plan de 8 semanas" → importarlo y administrarlo en su sección. |
| 22 | Control de peso: pasos y quema diaria | **P2** | Cierra el balance energético con actividad diaria real. Depende de 18. |
| 23 | Gimnasio visual (imágenes, ejecución, gasto) + planificar en calendario | **P2** | Catálogo rico y plan de gym por día. Depende de 17. |
| 24 | Nutrición conectada (catálogo con imágenes + recetas según entreno/peso/objetivo) | **P2** | Recetas en función de entrenamiento, peso y actividad. Depende de 22. |
| 25 | Análisis transversal + carga de históricos puntuales | **P3** | Correlaciones entre secciones. El usuario lo marca como bajo a corto plazo. |
| ∞  | Mejora de frontend + identidad propia | **Continua** | Apartado fijo: animaciones, más visual, entidad propia de marca. |

---

## Fases v3

### Fase 17 — Calendario (nueva sección) 🗓️ · P0 ✅ (completada)

Objetivo: un calendario que sea el eje de la app, con histórico y futuro.

- [x] Nueva sección **Calendario** con vistas **mes** y **semana**; navegación a
      pasado y futuro (‹ › + "Hoy").
- [x] Cada día agrega lo que ya existe: entrenos (punto de color por disciplina),
      sesiones planificadas de los planes (aro = pendiente) y sesiones de
      gimnasio.
- [x] Selección de un día → panel de detalle del día (entrenos con km/tiempo/
      ritmo/kcal/origen, planificado y gimnasio).
- [x] Métricas de la semana en foco (entrenos, km, kcal, carga) que cambian al
      seleccionar un día — reutilizables desde el inicio (Fase 19).
- [ ] _Futuro:_ integración con Suunto para **auto-generar y planificar**
      sesiones por día. Pendiente de dar a la sesión planificada un `source`
      cuando se implemente (hoy no se persiste ese campo).

**Entregable:** ✅ sección Calendario (`frontend/src/pages/Calendar.tsx` +
helpers en `calendar.ts`) con vistas mes/semana, agregación por día de
entrenos/planes/gimnasio, detalle del día y métricas semanales. Solo frontend
(reutiliza `workouts`, `plans`, `strength-sessions`, `analytics`); sin cambios de
backend ni de esquema. Verificado: lint y build limpios; dev server sirve la app.

---

### Fase 18 — Extracción Suunto en profundidad 📡 · P0 ✅ (núcleo completado)

Objetivo: sacar de Suunto todo lo que da valor a Entrenos, Peso y Análisis,
respetando la **retención mínima** (Principio 3).

- [x] **Cadencia y longitud de zancada**: el resumen de `/v2/workouts` trae
      `stepCount`; de él (con distancia y tiempo) se **derivan** cadencia
      (pasos/min) y zancada (m/paso) al leer — sin columnas extra. Exactamente
      las métricas del análisis de entrenos de recuperación/calidad.
- [x] **Política de retención**: `SuuntoRetentionService` + `POST
      /api/v1/suunto/purge` borra los entrenos de Suunto anteriores a la ventana
      (`kore.suunto.retention-months`, por defecto 1 → mes actual + anterior); no
      toca los `MANUAL`. Botón "Purgar antiguos" en Ajustes.
- [x] **Re-sync backfill (cierra el pendiente de los 50)**: un re-sync ahora
      **rellena** los entrenos ya importados (FC/tipo/kcal/pasos) en vez de
      ignorarlos por dedup; el resumen distingue `imported`/`updated`/`skipped`.
- [x] Contrato revisado (`SuuntoWorkout` +`stepCount`, `SuuntoWorkoutMapper` con
      `applyMetrics` reutilizable) y portabilidad al día (`schemaVersion` 9).
- [ ] _Pendiente (requiere descargar el FIT del entreno; no viene en el resumen):_
      **splits/laps** y **series por muestra** (FC/cadencia/altitud por tramo).
- [ ] _Pendiente (endpoint 24/7 aparte):_ **actividad diaria** (pasos y quema
      diaria) — aterriza junto a la Fase 22 (Peso).
- [ ] _Pendiente (el cliente no admite rango):_ **carga de históricos puntuales**
      bajo demanda sin persistir.

**Entregable:** ✅ los entrenos de Suunto traen pasos y derivan cadencia/zancada
(visibles en el detalle del día del Calendario); el re-sync corrige los ya
importados; la retención mínima evita crecer sin límite. Verificado: 69 tests
backend en verde (+2), migración `011` aplicada en `local.db` real, lint/build
frontend limpios. _Splits/series, actividad diaria 24/7 y carga histórica
puntual quedan pendientes por depender de la descarga FIT o de endpoints Suunto
que el cliente actual no cubre._

---

### Fase 19 — Inicio conectado 🏠 · P1 ✅ (completada)

Objetivo: que el inicio resuma **y** sea la puerta a cada sección.

- [x] **Fila de la semana actual** en el inicio (calendario compacto de 7 días)
      con marcadores por día (disciplina, gimnasio, planificado pendiente); cada
      día abre el Calendario en esa semana con el día seleccionado.
- [x] **Cada widget enlaza a su sección**: "Km esta semana" → Calendario
      (vista semana, día = hoy); "Carga" → Análisis; "Balance hoy" y "Peso" →
      Peso/Energía; tarjeta "Hoy" → Calendario; plan activo → Planes; últimos
      entrenos → Entrenos.
- [x] Widgets actuales conservados como resumen, ahora todos navegables.

**Entregable:** ✅ inicio navegable (`pages/Dashboard.tsx`) con fila de semana y
todos los widgets enlazados. Navegación sin router: `src/nav.ts`
(`View`/`CalendarFocus`/`Navigate`) + `App.tsx` guarda el foco del Calendario y
lo pasa a `CalendarView` (que ya aceptaba `initialDate`/`initialView` desde la
Fase 17). Solo frontend; sin backend. Verificado: lint y build limpios; dev
server sirve la app.

---

### Fase 20 — Entrenos: detalle, splits y análisis por tipo 🏃 · P1 ✅ (núcleo completado)

Objetivo: pasar del listado a un análisis real de cada entreno y entre entrenos
afines.

- [x] **Vista de detalle** de un entreno seleccionado (fila clicable → detalle):
      general primero + casillas de métricas (distancia, duración, ritmo, FC
      media/máx, **cadencia**, **zancada**, kcal, pasos, RPE, **carga de la
      semana**) y notas. `components/WorkoutDetail.tsx`.
- [x] **Análisis por tipo**: subtab Registro/Análisis; se elige disciplina y se
      acota por distancia y ritmo para aislar sesiones afines (p. ej.
      recuperación 12 km a ~5:30); tabla cronológica con ritmo/FC/cadencia/
      zancada + **carga de esa semana**, medias y rango, y micrográfico de la
      métrica elegida por sesión. `components/WorkoutAnalysis.tsx`.
- [x] Desglose progresivo: primero lo esencial, se profundiza al abrir el detalle
      o el análisis.
- [x] **UI más visual y con animaciones**: entrada `fade-up`, barras que crecen
      (`kore-grow`), fila con hover; respeta `prefers-reduced-motion`.
- [ ] _Pendiente (requiere el FIT del entreno, ver Fase 18):_ **splits por
      tramo** reales (ritmo/FC/cadencia/zancada por km/lap). El detalle ya deja
      el hueco documentado; hoy muestra las medias del entreno.

**Entregable:** ✅ seleccionar un entreno abre su detalle con todas las métricas
(incluidas cadencia/zancada) y la carga semanal; el análisis por tipo compara
entrenos afines entre fechas con micrográfico. Solo frontend. Verificado: lint y
build limpios; dev server sirve la app. _Los splits por tramo quedan pendientes
de la descarga FIT (Fase 18)._

---

### Fase 21 — Importación por secciones (KDL v2) 📥 · P1 ✅ (núcleo completado)

Objetivo: pedirle a una IA un plan (p. ej. 8 semanas para una maratón) en el
lenguaje de Kore e importarlo **en su sección**.

- [x] **Planes completos**: un `plan` KDL ya lleva todas sus sesiones por día
      (igual `dietPlan`→comidas, `recipe`→ingredientes, `routine`→ejercicios) —
      un plan completo se importa como **un solo recurso**. Documentado.
- [x] **Import por sección** (backend + frontend): endpoint tipado `POST
      /api/v1/kdl/import/{type}` que acepta un **payload suelto**, un **array** o
      un **documento KDL** completo; y componente reutilizable `SectionImport`
      (pegar JSON o subir archivo) en **Planes** (plan), **Nutrición**
      (recipe/dietPlan) y **Gimnasio** (routine/exercise).
- [x] Documentado en `docs/AGENTS.md` (import por sección + recursos compuestos)
      y en el manifest del agente.
- [ ] _Pendiente:_ panel en **Ajustes** que liste **todo lo importado** con su
      **origen** y permita administrarlo/borrarlo. Requiere **trazar la
      procedencia** por entidad (columna `origin`/`imported` en cada recurso +
      migración) — cambio transversal que se aborda aparte. Hoy Ajustes ya tiene
      el import/export global (KDL) y cada sección gestiona lo suyo.

**Entregable:** ✅ un JSON de plan generado por IA se importa desde la propia
sección (Planes) o vía `POST /kdl/import/plan`, y queda gestionable ahí. 71 tests
backend en verde (+2), lint/build frontend limpios. _El panel de "importados con
origen" en Ajustes queda pendiente por necesitar trazado de procedencia._

---

### Fase 22 — Control de peso: pasos y quema diaria ⚖️ · P2 ✅ (núcleo completado)

Objetivo: cerrar el balance energético con la actividad diaria real.

- [x] Modelo **`DailyActivity`** (pasos + quema total del día, upsert por fecha):
      domain/repo/servicio/DTOs + migración `012`; CRUD `/api/v1/activity/daily`.
- [x] **Integrado en el balance**: cuando hay quema diaria para un día,
      **sustituye** la estimación por entrenos (es el total del día basal +
      actividad + entrenos → evita contar dos veces). `EnergyService`.
- [x] **Frontend**: sección "Actividad diaria" en Peso (alta de pasos/quema por
      fecha + tabla + borrado); se refleja en el balance automáticamente.
- [x] **Portabilidad**: export `schemaVersion` 9→10 (`dailyActivities`) + KDL
      `koreType: dailyActivity`; manifest y `docs/AGENTS.md` al día.
- [ ] _Pendiente (mismo bloqueo que Fase 18):_ **fetch automático** de la
      actividad 24/7 de Suunto (pasos/quema) — endpoint aparte del resumen de
      workouts. Hoy se rellena a mano o por import/KDL.

**Entregable:** ✅ el balance cuenta con la quema diaria y los pasos, no solo con
los entrenos; se registran a mano o por import y viajan en el export/KDL. 75
tests backend en verde (+4), migración `012` aplicada en `local.db` real, lint/
build frontend limpios. _El fetch automático desde Suunto queda pendiente._

---

### Fase 23 — Gimnasio visual + planificar en calendario 🏋️ · P2 ✅

Objetivo: un catálogo de ejercicios rico y planes de gimnasio por día.

- [x] Cada ejercicio con **imagen/animación** de cómo se ejecuta (`imageUrl`),
      **descripción** (para qué y por qué), **gasto energético** (`metValue` MET →
      estimación kcal/min con tu peso) e **información de realización**
      (`instructions`). Catálogo en tarjetas visuales con miniatura, badge de
      categoría y detalle desplegable. Seed enriquecido.
- [x] Planificar **gimnasio para un día** concreto del calendario: la sesión de
      fuerza gana `status` PLANNED/DONE. Se planifica desde Gimnasio → «Gimnasio
      por día», aparece en el Calendario (Fase 17) diferenciando planificada de
      hecha, y se marca como hecha desde ahí.

**Entregable:** ver cómo se hace cada ejercicio y planificar gimnasio por día.

Portabilidad: export de sesión `schemaVersion` 11 y KDL `exercise` con los
campos nuevos. Migración `013`. Nota: se reutiliza `StrengthSession` (no una
entidad "sesión planificada" aparte) por simplicidad; el snapshot de rutina por
nombre ya viaja en el export.

---

### Fase 24 — Nutrición conectada 🥗 · P2 ✅

Objetivo: nutrición que parte de ingredientes y se adapta al entrenamiento.

- [x] **Catálogo de ingredientes con imágenes** (`imageUrl`) y valores
      nutricionales, en tarjetas visuales. **Recomendación por ingrediente**:
      `GET /recipes/by-ingredient/{id}` y botón «Ver recetas» en cada ingrediente.
- [x] Seleccionar ingredientes y **crear recetas** cómodamente: el formulario de
      receta ofrece el catálogo como `datalist` y enlaza por nombre
      (`ingredientId`).
- [x] **Recetas en función del contexto** (`GET /recipes/recommend`): mira los
      entrenos de los últimos 7 días, la actividad diaria y el objetivo de peso
      para fijar un enfoque (CARB / BALANCED / PROTEIN) y calorías objetivo, y
      ordena tus recetas por encaje. Subtab «Recomendadas». Relaciona Nutrición
      con Peso, actividad y entrenamiento.

**Entregable:** partir de un ingrediente, ver recetas sugeridas y generar recetas
adaptadas al entrenamiento y al objetivo.

Portabilidad: export `schemaVersion` 12 (ingrediente con `imageUrl`), migración
`014`. Nota: el recomendador es determinista (sin modelo); ordena por reparto de
macros de la receta, así que conviene rellenar las macros al crearlas.

---

### Fase 25 — Análisis transversal + históricos puntuales 📊 · P3 ✅ (núcleo completado)

Objetivo (bajo a corto plazo, por decisión del usuario): correlaciones entre
todas las secciones.

- [x] Análisis que cruza **nutrición** (consumidas/balance) × **vida diaria**
      (pasos) × **entrenamiento** (carga/km/ritmo) × **resultados** (ritmo/peso)
      semana a semana y muestra **correlaciones** (Pearson) con interpretación.
      `GET /analytics/correlations` (crudo) + subtab «Correlaciones» en Análisis.
- [ ] Apoyarse en la **carga de históricos puntuales** (Fase 18) para mirar más
      atrás: pendiente del mismo bloqueo (la Suunto Cloud API no da rango; los
      históricos no se persisten por retención mínima). El informe se calcula
      sobre las semanas ya almacenadas.

**Entregable:** informes de correlación entre secciones, alimentados por datos
recientes (la carga de históricos puntuales queda pendiente).

---

### Mejora de frontend + identidad propia (apartado continuo) 🎨

> Apartado fijo en **cada** versión. En v3 el foco es darle a la app una
> **entidad propia** y hacerla más **visual**.

- [x] **Modo oscuro real** (2026-07-23): la paleta Kore ahora nace en **oscuro
      por defecto**, con tokens semánticos por tema (`--bg/--surface/--surface-2/
      --text/--muted/--border/--input-*/--shadow/--action/--link…`) y override
      `:root[data-theme='light']`. Toggle ☀/☾ persistente (`kore.theme`) con
      init sin parpadeo en `index.html`; logotipo cambia a la variante `-oscuro`.
- [x] **Identidad propia** (2026-07-23): superficies/acentos por tema, logo
      adaptativo, **sistema tipográfico Kore** (tokens `--font-sans/--font-mono`,
      escala modular, titulares con tracking, **numerales mono/tabulares** en
      métricas y tablas, eyebrows en versalitas; todo con stacks del sistema) e
      **iconografía propia**: set de iconos SVG de trazo en línea
      (`components/icons.tsx`, `currentColor`) en la navegación (icono + etiqueta,
      acento naranja en la pestaña activa), el toggle de tema (sol/luna) y los
      marcadores de catálogo. Opcional futuro: un `@font-face` local con una
      tipografía de marca (basta soltar el woff2 y cambiar `--font-sans`).
- [~] **Más visual y con animaciones**: ya hay `fade-up`, barras animadas y
      transiciones de tema; ampliar micro-interacciones en Entrenos/Calendario.
- [x] **Toasts + estados de carga/vacío uniformes** (2026-07-23): store de
      toasts propio sin dependencias (`src/toast.ts`) + `<Toaster>` en la raíz,
      feedback de éxito/error en las mutaciones (los `run()` de Gimnasio/
      Nutrición/Peso/Ingredientes/Plantillas, export de sesión y sync/purga de
      Suunto). Componentes `<Loading>` (spinner Kore) y `<Empty>` (icono + texto)
      en `components/state.tsx`, aplicados en Inicio/Calendario/Análisis/
      correlaciones/recomendaciones/comparativa y en los listados de cada sección.
- [x] **Accesibilidad** (2026-07-23): anillo de foco `:focus-visible` de acento
      en toda la app, skip-link «Saltar al contenido», landmark `<nav>` con
      `aria-current="page"` en la pestaña activa, tarjetas de ejercicio y filas
      de entreno operables por teclado (Enter/Espacio, con guardas), y etiquetas
      accesibles en el calendario (fecha por celda, `aria-current="date"`). El
      tema oscuro cumple contraste AA para texto (muted ~6:1). Opcional futuro:
      una **tipografía de marca** por `@font-face` local.
- [x] **Enrutado ligero** (2026-07-23): router propio por hash sin dependencias
      (`src/router.ts`: `useRoute`/`navigateTo`/`parseHash`/`toHash`) —
      `#/gym`, `#/calendar/2026-07-23/week`, etc. URLs compartibles + atrás/
      adelante del navegador, sin reescrituras de servidor (sirve estático y
      offline). App.tsx deriva la vista de la ruta en vez de estado local.
- [ ] Preparar el terreno para la **app móvil** una vez la funcionalidad web esté
      completa.

---

<br>

# Historial — ROADMAP v2 — Kore ✅ completado

## Prioridad de las fases v2

El orden lo marca la dependencia y el valor. Etiqueta de prioridad orientativa:

| Fase | Tema | Prioridad | Por qué ahí |
|------|------|-----------|-------------|
| 9  | Rebrand a Kore + tokens de color | **P0** | Identidad y base visual; desbloquea dashboard y mejoras de frontend. |
| 10 | Revisión de extracción Suunto | **P0** | Ya hay datos reales; todo el análisis y la carga dependen de extraer bien. |
| 11 | Catálogos base (ingredientes + ejercicios) | **P1** | Datos maestros que alimentan control de peso y sesiones. |
| 12 | Control de peso y balance energético | **P1** | Objetivo central de v2; depende de 11 y 10. |
| 13 | Dashboard de inicio | **P1** | Primera página; depende de tener datos de todos los módulos. |
| 14 | Análisis de carga comparativo | **P2** | km/carga semana vs semana y por rangos; depende de 10. |
| 15 | Lenguaje propio de import/export (KDL) | **P2** | Generaliza el export de sesión a recursos sueltos, multi-versión/dispositivo. |
| 16 | Tipos de sesiones específicas | **Futuro** | El propio usuario lo marca como futuro. |
| ∞  | Mejora de frontend | **Continua** | Apartado fijo en cada versión del ROADMAP (solo web hasta v1 de funcionalidad). |

---

## Fases v2

### Fase 9 — Rebrand a Kore + tokens de color ✅ (completada)

Objetivo: que la app sea Kore de arriba abajo y tenga un sistema de color propio.

- [x] Renombrado de cara al usuario: `<title>` y `apple-mobile-web-app-title`
      → Kore; manifest PWA (`name`/`short_name`/`description`/`theme_color` +
      `background_color` → `#0E1B2A`); `icon.svg` = `kore-icono.svg`; logotipo
      (`kore-logotipo.svg`) en la cabecera y en el login; nombres de fichero de
      export (`kore-…json`); `CACHE` del SW → `kore-shell-v1`.
- [x] Assets de marca versionados en el repo: `frontend/public/brand/*` (iconos
      y logotipos) y `docs/brand/kore-paleta.md` (extraídos de `files.zip`).
- [x] Tokens CSS de la paleta en `:root` (`--color-core`, `--color-run`,
      `--color-swim`, `--color-bike`, estados…) y refactor de `index.css`: el
      azul off-brand `#2f6fed` pasa a `--color-action` (naranja, con texto oscuro
      por contraste); enlaces en azul ciclismo; barras en naranja carrera.
- [x] Color por disciplina transversal: mapeo único tipo→color en
      `frontend/src/discipline.ts` (carrera/natación/ciclismo/fuerza/otro),
      aplicado como etiqueta con punto de color en la tabla de entrenos. _Listo
      para natación/ciclismo cuando la Fase 10 mapee bien los tipos de Suunto._
- [x] **Decisión de renombrado interno:** se mantiene lo interno por
      compatibilidad de datos/imports — paquete `com.zensyra.ccollector`, header
      `X-CCollector-Username` y clave localStorage `ccollector.username` NO
      cambian; solo se renombró la capa visible. Migración interna con alias, si
      se quiere, queda como tarea aparte no urgente.

**Entregable:** ✅ la app se presenta como Kore (logo, título, PWA, paleta y
color por disciplina) sin romper sesiones ni imports existentes. Verificado:
build y lint frontend limpios; assets de marca presentes en `dist/` y `<title>`
+ `theme-color` correctos en el HTML compilado.

---

### Fase 10 — Revisión de extracción Suunto ✅ (completada)

Objetivo: validar, con la primera carga real ya hecha, que extraemos bien.

Diagnóstico sobre los 50 entrenos ya cargados: distancia/tiempo/fecha/sourceId
correctos, pero **FC media null en los 50** (bug) y **todo mapeado a RUNNING**
(valor por defecto) y las **kcal descartadas** (sin columna).

- [x] Contrastados los nombres de campo contra la documentación oficial: la FC
      va **anidada en `hrdata`** (`workoutAvgHR`/`workoutMaxHR`), no como `hravg`
      plano — de ahí los null. `SuuntoWorkout` reestructurado con `HrData` y
      accesores; se añaden `totalAscent`/`totalDescent`.
- [x] Mapeo real de `activityId` → disciplina en `SuuntoWorkoutMapper` con la
      tabla oficial de Suunto (~120 ids): running/ciclismo/natación/fuerza, resto
      `OTHER`. Enum `WorkoutType` ampliado con `CYCLING` y `SWIMMING` → el color
      de disciplina (Fase 9) ya es fiel.
- [x] Unidades verificadas (m, s, ppm, kcal) y persistencia de FC máx y kcal
      (`energyConsumption` → `energyKcal`): migración `007-workout-hr-energy`.
      Dedup por `sourceId` confirmada con datos reales.
- [x] Fixture de payload JSON real (esquema oficial) + test de mapeo end-to-end
      (`SuuntoWorkoutMapperTest`): FC anidada, kcal y mapeo de activityId.
- [x] Documentado en `docs/AGENTS.md` (sección "Extracción de Suunto") y bump del
      export a `schemaVersion` 5 (`maxHeartRate`, `energyKcal`).

**Entregable:** ✅ cada entreno sincronizado refleja el dato real (tipo,
distancia, tiempo, FC media/máx, kcal). Verificado: 41 tests backend en verde
(+3 de mapeo) y build/lint frontend limpios. **Nota:** por la dedup, los 50
entrenos YA importados no se rellenan solos en un re-sync; para corregirlos hay
que borrar los de origen Suunto y volver a sincronizar (pendiente de tu OK).

---

### Fase 11 — Catálogos base: ingredientes y ejercicios ✅ (completada)

Objetivo: datos maestros seleccionables para construir dietas, recetas y sesiones.

**11.1 · Catálogo de ingredientes**
- [x] Modelo `Ingredient` con valores nutricionales por 100 g/ml (kcal,
      proteína, carbos, grasa, fibra, azúcares, sodio) + unidad base
      (`GRAM`/`MILLILITER`). Migración `008-create-ingredients`.
- [x] CRUD `/api/v1/ingredients` (POST sirve de import de agente); las recetas
      pueden **referenciar un ingrediente del catálogo** vía `ingredientId`
      opcional en cada línea (se conserva el nombre como snapshot).
- [x] Semilla de 20 ingredientes comunes **importable** (JSON empaquetado
      `seeds/ingredients.json` + `POST /ingredients/seed`, idempotente por nombre).

**11.2 · Catálogo de ejercicios ampliado**
- [x] Ejercicios con **categoría/fase** `ExerciseCategory` (WARMUP / STRENGTH /
      RECOVERY) y **material** (`requiresEquipment` + `equipment`), cubriendo
      fuerza sin equipo (plancha, abdominales, core, flexiones…), con equipo
      (mancuernas 7 kg…), recuperación y calentamiento. Migración `008`.
- [x] Filtro por categoría en el frontend y semilla de 21 ejercicios por
      categoría (`seeds/exercises.json` + `POST /exercises/seed`).

**Entregable:** ✅ catálogos de ingredientes (con nutrición por 100 g/ml) y de
ejercicios (con/sin material, recuperación, calentamiento) seleccionables, con
sus valores/metadatos y semillas de ejemplo. Export a `schemaVersion` 6
(retrocompatible). Verificado: 46 tests backend en verde (+5: CRUD/seed de
ingredientes y seed de ejercicios) y build/lint frontend limpios.

---

### Fase 12 — Control de peso y balance energético ✅ (completada)

Objetivo central de v2: mantener el peso del atleta (ni subir ni bajar) con
control de dieta y de kcal consumidas vs quemadas.

- [x] Registro de peso (`WeightEntry`, upsert por fecha) con histórico y
      tendencia (▲/▼ vs medición previa); objetivo de **mantenimiento**
      (`WeightGoal`: **rango** min/max, no un único número) + kcal/día de
      referencia. CRUD `/api/v1/weight` y `/api/v1/weight/goal`. Migración `009`.
- [x] kcal **consumidas**: derivadas de las comidas de las dietas → receta
      (`recipe.calories` o derivadas del **catálogo de ingredientes** de la
      Fase 11 vía `ingredientId` × cantidad / 100).
- [x] kcal **quemadas**: `energyKcal` real de Suunto cuando existe; si no,
      estimación por tipo y duración (`EnergyEstimate`, kcal/min por disciplina).
- [x] Balance energético diario (14 días) y semanal (8 semanas) = consumidas −
      quemadas, con **estado** déficit/equilibrio/superávit frente al
      mantenimiento (colores de estado Kore). `GET /api/v1/energy/summary`.
- [x] Peso y objetivo exportables en el JSON de sesión (`schemaVersion` 7,
      retrocompatible). Nueva pestaña **Peso** en el frontend.

**Entregable:** ✅ registras tu peso, fijas un rango de mantenimiento y ves tu
balance de energía (consumidas vs quemadas, diario/semanal) con señal de si estás
en equilibrio, cruzando dieta y entrenamiento. Verificado: 54 tests backend en
verde (+8: peso y balance energético) y build/lint frontend limpios.

---

### Fase 13 — Dashboard de inicio ✅ (completada)

Objetivo: que la primera página sea un resumen general de un vistazo.

- [x] Nueva vista **Inicio** (`pages/Dashboard.tsx`) como pestaña **por defecto**
      al entrar.
- [x] Widgets: **hoy** (sesiones planificadas con estado hecho/pendiente +
      entrenos registrados hoy), **carga de la semana** con tendencia (▲/▼ vs
      semana previa), **km de la semana**, **balance energético** de hoy y de la
      semana con estado, **peso reciente** con tendencia y "en rango",
      **adherencia** al plan activo con barra + **próximas sesiones**, **últimos
      entrenos** marcando los de Suunto.
- [x] Reutiliza los endpoints existentes (`/analytics/summary`, `/energy/summary`,
      `/weight`, `/plans`, `/workouts`) compuestos en el cliente — sin backend
      nuevo, como pedía el roadmap.
- [x] Estética Kore: color por disciplina (puntos), tarjeta "Hoy" con acento
      naranja (núcleo/latido).

**Entregable:** ✅ al abrir Kore aparece un dashboard resumen con lo importante
del día y la semana sin navegar por pestañas. Verificado: build y lint frontend
limpios (backend sin cambios; 54 tests siguen en verde).

---

### Fase 14 — Análisis de carga comparativo ✅ (completada)

Objetivo: control fino de la carga del atleta comparando periodos.

- [x] Comparativa de km/tiempo/carga/entrenos **semana actual vs anterior** (por
      defecto) y contra **rangos arbitrarios** (selector de fechas). Endpoint
      `GET /api/v1/analytics/compare` (`LoadComparisonService`).
- [x] Diferencias de carga alineadas **por día** (posición en el rango) y
      **acumuladas** (`dailyDeltas`).
- [x] Desglose por **disciplina** (colores Kore) y por **origen** (manual/Suunto)
      del rango actual.
- [x] **Señales de carga** con estado OK/vigilar/riesgo: **ACWR** (agudo 7d /
      crónico 28d), **monotony** (Foster, media/DE semanal) y **ramp** (variación
      % semana vs previa) — orientativas, para detectar sobrecarga.
- [x] Documento **crudo exportable como JSON** (botón "Exportar JSON"); nueva
      sub-pestaña **Comparativa** en Análisis.

**Entregable:** ✅ comparas km y carga entre semanas/rangos, por día o acumulado,
con desglose por disciplina/origen y señales que avisan si la carga se dispara.
Verificado: 58 tests backend en verde (+4: comparación por día, defaults,
validación) y build/lint frontend limpios. Sin migraciones (todo derivado de
`workouts`).

---

### Fase 15 — Lenguaje propio de import/export de recursos (KDL) ✅ (completada)

Objetivo: un formato propio para importar/exportar **recursos generales sueltos**
(no solo la sesión completa), que conviva entre versiones y dispositivos.

- [x] **Kore Data Language**: JSON con envoltorio `{ kore, kdlVersion, items }`,
      cada item `{ koreType, schemaVersion, payload }` (payload sin ids de BD).
      Tipos: `workout`, `plan`, `exercise`, `ingredient`, `recipe`, `routine`,
      `dietPlan`, `weightEntry`.
- [x] Import/export por recurso **y en lote** (un fichero mezcla tipos y
      versiones). `GET /api/v1/kdl/export[?types=…]` (reutiliza el export de
      sesión) y `POST /api/v1/kdl/import` (item aislado: uno inválido no aborta
      el resto; devuelve `imported`/`skipped`/`byType`/`errors`).
- [x] Convivencia entre versiones: `schemaVersion` por tipo, tolerancia a campos
      desconocidos (mapper permisivo), rechazo de versiones mayores, y hook de
      migradores por tipo (hoy identidad en v1). Sin ids de BD.
- [x] Documentado en `docs/AGENTS.md` (sección KDL) + panel **Recursos (KDL)** en
      Ajustes (exportar/importar fichero). Añadido al manifest de agentes.

**Entregable:** ✅ cualquier recurso se inserta/saca con un fichero JSON estable,
compatible entre versiones y dispositivos. Verificado: 62 tests backend en verde
(+4: import por lotes con aislamiento de errores, export con filtro por tipo,
roundtrip export→import entre sesiones) y build/lint frontend limpios.

---

### Fase 16 — Tipos de sesiones específicas ✅ (completada)

Objetivo: sesiones específicas por deporte y por preparación de competición.

- [x] Catálogo de **plantillas de sesión** (`SessionTemplate`) etiquetadas por
      **disciplina** (running/ciclismo/natación/gimnasio), **objetivo** de
      competición (`RaceGoal`: maratón, media, 10k, 5k, general) y **nivel**
      (`Level`: principiante/intermedio/avanzado), con estructura de la sesión.
      Migración `010`; CRUD `/api/v1/session-templates` (POST = import de agente).
- [x] Semilla de 14 plantillas de ejemplo cubriendo las disciplinas y los
      objetivos por nivel (`seeds/session-templates.json` +
      `POST /session-templates/seed`, idempotente).
- [x] Frontend: sub-pestaña **Plantillas** en Planes con **filtros** por
      disciplina/objetivo/nivel, alta y semilla; color por disciplina.
- [x] Portable: en el export de sesión (`schemaVersion` 8, retrocompatible) y en
      **KDL** (`koreType: sessionTemplate`).

**Entregable:** ✅ tienes una biblioteca de sesiones específicas filtrable por
disciplina, objetivo de carrera y nivel, lista para consultar y llevar entre
dispositivos. Verificado: 67 tests backend en verde (+5: CRUD, seed, roundtrip
de export) y build/lint frontend limpios.

_Pendiente como mejora futura (no bloqueante): "instanciar" una plantilla como
sesión planificada dentro de un plan concreto._

---

### Mejora de frontend (apartado continuo) 🎨

> Apartado fijo en **cada** versión del ROADMAP. Por ahora **solo web**; cuando
> exista la v1 de funcionalidad completa, se empezará la app móvil. Aquí van
> mejoras de UX y nuevas implementaciones sobre lo que ya tenemos.

- [ ] Aplicar el sistema de color Kore de forma consistente (tras Fase 9).
- [ ] Modo oscuro real (la paleta ya está pensada para oscuro por defecto).
- [ ] Gráficos más ricos en Análisis (líneas de tendencia, comparativas).
- [ ] Feedback de acciones (toasts) y estados de carga/errores uniformes.
- [ ] Filtros y búsqueda en los catálogos (ingredientes, ejercicios).
- [ ] Componente de anillos por disciplina reutilizable (identidad Kore).
- [ ] Accesibilidad y navegación por teclado; pulir el responsive existente.

_Nota: `App.tsx` gestiona el estado de vista sin router; si el número de vistas
crece (Inicio, Peso, Catálogos…), evaluar introducir enrutado ligero._

---

<br>

# Historial — ROADMAP v1 (CCollector) ✅ completado

Plataforma personal para administrar entrenamientos de running, analizar
rendimiento (integración con Suunto), planificar sesiones, seguir rutinas de
gimnasio en casa y gestionar recetas y dietas.

Es una aplicación **de uso propio**: nunca se expone a internet, tú controlas
tus datos en todo momento, te los puedes llevar a cualquier dispositivo y hacer
con ellos lo que quieras — sin servicios en la nube ni fuentes externas que te
limiten.

---

## Principios de diseño

Estos principios condicionan todas las fases y no se negocian:

1. **Los datos son tuyos y portables.** Todo recurso (sesión, entreno, plan,
   rutina, receta, dieta, resumen) se puede **exportar e importar como JSON**.
   El JSON es la fuente de verdad portable, no un extra.
2. **Sin nube, sin dependencias externas obligatorias.** La app funciona
   self-hosted en tu dispositivo. Las integraciones (Suunto) son opcionales y se
   configuran con tus propias credenciales.
3. **Sesión local, no cuentas.** "Login" = crear una sesión con un username. No
   hay autenticación real porque la app no se expone; la sesión vive en tu
   dispositivo y se mueve vía export/import.
4. **Preparada para agentes.** Los esquemas JSON son estables y documentados
   para que agentes de IA puedan leer, analizar y planificar sobre tus datos.
5. **Multiplataforma progresiva.** Primero web (+ responsive/mobile web), luego
   app Android nativa. Un backend, varios clientes.

---

## Stack actual (bootstrap ya en marcha)

- **Backend:** Quarkus 3.37 (Java 21), Liquibase para migraciones, **SQLite**
  como base de datos (fichero `backend/local.db`, cero infraestructura, portable
  — encaja con la filosofía self-hosted). Paquete base
  `com.zensyra.ccollector.core` organizado por dominio (`dto`, `resource`,
  `exception`, `rest`; `domain`/`repository`/`service` llegan en Fase 1).
- **Frontend:** React 19 + Vite + TypeScript, oxlint como linter.
- **Arranque:** script `./up` levanta backend (`:8080`) y frontend (`:5173`)
  juntos; Vite proxya `/api` al backend.

---

## Fases

### Fase 0 — Fundaciones ✅ (completada)

Objetivo: que el proyecto arranque, compile y migre contra base de datos.

- [x] Configurar datasource SQLite y activar `migrate-at-start`.
- [x] Sin infraestructura: SQLite en fichero, no hace falta Docker.
- [x] Health check (`/q/health`) y arranque verificado del backend.
- [x] Frontend conectado al backend (proxy dev de Vite, `VITE_API_BASE`).
- [x] Convención de respuestas API (`ResponseDTO`) y `GlobalExceptionMapper`.
- [x] Pipeline mínimo de calidad: tests backend (JUnit5 + rest-assured), lint
      frontend (oxlint).

**Entregable:** `./up` levanta backend + frontend y la migración corre. ✅
Verificado: `/api/v1/ping` responde directo y vía proxy Vite; health UP con la
conexión a la base de datos UP; 2 tests backend en verde; build y lint frontend
limpios.

---

### Fase 1 — MVP núcleo: sesión, log de entrenamientos y portabilidad ✅ (completada)

Los 4 objetivos fundacionales del MVP.

**1.1 · Login simple por username**
- [x] Crear sesión indicando solo el `username` (sin contraseña, login-or-create).
- [x] `CollectorUser` + migración `001-create-users`.
- [x] `AuthResource` / `AuthService` / `UserRepository` funcionales.
- [x] Frontend: pantalla de entrada + sesión persistida en localStorage
      (`ccollector.username`); identidad vía header `X-CCollector-Username`.

**1.2 · Log de entrenamientos propio (manual)**
- [x] Modelo de entrenamiento (fecha, tipo, distancia, duración, FC, esfuerzo
      percibido, notas, origen); unidades canónicas (metros/segundos), ritmo
      derivado.
- [x] CRUD de entrenamientos vía API (`/api/v1/workouts`, con ownership por
      usuario y validación).
- [x] Frontend: crear / editar / listar / eliminar entrenos.

**1.3 · Export / import de sesión portable**
- [x] Endpoint de **export** (`GET /session/export`): vuelca usuario + entrenos
      a un JSON descargable (sin ids de BD).
- [x] Endpoint de **import** (`POST /session/import`): reconstruye la sesión;
      409 si el username ya existe.
- [x] Versionado del formato (`schemaVersion` 1).
- [x] Frontend: exportar (descarga) y importar (subida de fichero).

**Entregable:** ✅ puedes crear tu sesión, registrar entrenos a mano, exportarlo
todo a un JSON, borrarlo, reimportarlo en otro dispositivo y recuperar tus datos.
Verificado: 11 tests backend en verde + flujo end-to-end completo (login → CRUD
→ export → import → conflicto 409); build y lint frontend limpios.

**Nota técnica:** SQLite exige `id` de tipo `INTEGER` para autoincrement, y las
fechas/instantes se guardan como texto ISO-8601 (convertidores JPA) para evitar
el parseo de fechas del driver. Persistencia con Hibernate ORM + Panache;
Liquibase es la fuente de verdad del esquema.

---

### Fase 2 — Integración con Suunto ✅ (completada, pendiente validar con credenciales reales)

Objetivo: enriquecer el log con datos reales importados de Suunto.

- [x] Apartado **Ajustes** para habilitar la integración e introducir
      `client-id`, `client-secret`, `refresh-token` **y `subscription-key`**
      (la Cloud API de Suunto exige además esta 4ª credencial). Migración
      `003-suunto-integration`.
- [x] Almacenamiento seguro de credenciales (cifrado AES-GCM en reposo;
      verificado que la BD no contiene los secretos en claro).
- [x] Cliente OAuth (grant `refresh_token`, Basic auth) + cliente Cloud API
      (`/v2/workouts` con Bearer + `Ocp-Apim-Subscription-Key`); guarda el
      refresh token rotado.
- [x] Sincronización de workouts: importa y mapea al modelo interno,
      deduplicando por `sourceId` (id de origen).
- [x] Cada entreno marca su origen (`MANUAL` vs `SUUNTO`).
- [x] Frontend: página de Ajustes con estado, guardar credenciales y
      "Sincronizar ahora" mostrando importados/omitidos/total.

**Entregable:** ✅ conectas Suunto en Ajustes y sincronizas tus entrenos.
Verificado con clientes REST mockeados (5 tests: sync importa+dedup, errores→502,
sin config→400, sin sesión→401) y flujo de settings real (guardar/ocultar
secretos, cifrado en reposo). **Pendiente:** validar contra credenciales/datos
reales de Suunto los nombres de campo del workout (aislados en
`SuuntoWorkout`/`SuuntoWorkoutMapper`) y el mapeo de `activityId` a tipo.

**Nota sobre portabilidad:** las credenciales de Suunto NO se incluyen en el
export/import de sesión (son específicas del dispositivo y sensibles); sí se
exportan los entrenos ya sincronizados (son datos de entrenamiento).

---

### Fase 3 — Análisis y rendimiento ✅ (completada)

Objetivo: convertir el log en información útil.

- [x] Métricas por entreno: ritmo medio (derivado). _Splits y zonas de FC
      quedan fuera: solo guardamos el resumen del entreno, no series
      temporales._
- [x] Resúmenes agregados: semanal (últimas 12) y mensual (últimos 6), con
      entrenos, distancia, tiempo, ritmo medio y carga.
- [x] Training load (sRPE = duración·RPE, con RPE estimado cuando falta) y
      tendencia semanal; agrega entre fuentes (manual + Suunto).
- [x] Dashboard: tarjetas de totales + gráficos de volumen y carga semanal +
      tabla mensual.
- [x] Resumen exportable como JSON (endpoint crudo `/analytics/summary` +
      botón "Exportar resumen").

**Entregable:** ✅ pantalla de rendimiento con tendencias y resumen exportable.
Verificado: 18 tests backend en verde (totales, carga y bucket semanal);
build y lint frontend limpios. Sin migraciones nuevas (todo derivado de
`workouts`). Fórmula de carga aislada en `TrainingLoad` para cambiarla fácil.

---

### Fase 4 — Planificación de entrenamientos (running) ✅ (completada)

Objetivo: pasar de registrar a planificar.

- [x] Modelo de plan (nombre, objetivo, fechas) con sesiones planificadas
      (fecha, tipo, distancia/duración objetivo, descripción). Migración `004`.
- [x] Timeline de sesiones planificadas vs realizadas (una sesión se marca
      hecha si hay un entreno ese día). _Calendario como timeline ordenado, no
      rejilla mensual._
- [x] Adherencia: completadas/planificadas y % por plan.
- [x] Planes exportables/importables como JSON: incluidos en el export de
      sesión (`schemaVersion` 2, retrocompatible); `POST /plans` sirve además
      como import de un plan generado por un agente.

**Entregable:** ✅ creas un plan, ves su timeline planificado-vs-realizado y su
adherencia. Verificado: 23 tests backend en verde (adherencia, CRUD, validación,
roundtrip export/import de planes) + flujo real (plan con sesiones, adherencia
1/2 = 50%, export con planes); build y lint frontend limpios.

---

### Fase 5 — Rutinas de gimnasio en casa ✅ (completada)

Objetivo: fuerza y trabajo complementario.

- [x] Catálogo de ejercicios (nombre, grupo muscular, material, descripción).
- [x] Rutinas con ejercicios (series, repeticiones, descanso, notas); el
      ejercicio va incrustado en la rutina (snapshot) para que sea autocontenida.
- [x] Registro de sesiones de fuerza (fecha, rutina opcional con snapshot de
      nombre, notas). _Log por serie/peso queda como trabajo futuro._
- [x] Ejercicios, rutinas y sesiones exportables/importables (en el export de
      sesión, `schemaVersion` 3, retrocompatible); `POST /routines` sirve
      también de import para una rutina generada por un agente.

**Entregable:** ✅ creas rutinas de fuerza en casa, mantienes un catálogo y
registras las sesiones. Verificado: 29 tests backend en verde (CRUD de
ejercicios/rutinas/sesiones, roundtrip export/import) + flujo real; build y lint
frontend limpios.

---

### Fase 6 — Recetas y dietas ✅ (completada)

Objetivo: nutrición integrada con el entrenamiento.

- [x] Receta: ingredientes (nombre/cantidad/unidad), pasos (texto multilínea),
      macros (calorías/proteína/carbos/grasa como totales) y raciones.
- [x] Plan de dieta con objetivos nutricionales (kcal + macros) y comidas por
      día (fecha, tipo de comida, receta —snapshot de nombre—, notas).
- [x] Planificación por periodo (inicio/fin).
- [x] Recetas y dietas exportables/importables (export de sesión,
      `schemaVersion` 4, retrocompatible); `POST /recipes` y `POST /diet-plans`
      sirven de import para contenido generado por un agente.

**Entregable:** ✅ gestionas recetas y planificas dietas dentro de la plataforma.
Verificado: 34 tests backend en verde (CRUD de recetas/dietas, validación,
roundtrip export/import) + flujo real; build y lint frontend limpios.

---

### Fase 7 — Integración con agentes de IA ✅ (completada)

Objetivo: que agentes puedan planificar y analizar sobre tus datos. Este es el
propósito último de mantener todo en JSON.

- [x] Esquemas JSON versionados (`schemaVersion` del export, historial 1→4) y
      documentados en `docs/AGENTS.md`.
- [x] API descriptiva: OpenAPI en `/q/openapi` + Swagger UI en `/q/swagger-ui`,
      y un **manifest** (`GET /api/v1/agent/manifest`) con el mapa estable de
      recursos y cómo leerlos/escribirlos.
- [x] Casos de uso cubiertos: `GET /api/v1/agent/context` da sesión completa +
      analítica en una llamada (para generar un plan, analizar carga, proponer
      dieta).
- [x] Import de recursos generados por agentes: el `POST` de cada recurso, más
      `POST /session/import` para sesiones completas (flujo de la Fase 1).

**Entregable:** ✅ un agente descubre la API por el manifest, lee todo por
`/agent/context`, razona, y escribe con los `POST`/import. Verificado: 38 tests
backend en verde (manifest público, context con sesión, OpenAPI servido). En el
frontend, Ajustes muestra los endpoints de agente.

---

### Fase 8 — Mobile y Android 🟡 (web móvil + PWA hechos; Android nativo pendiente)

Objetivo: usar la app cómodamente en el móvil.

- [x] Web responsive / mobile-first pulido (barra de pestañas deslizable,
      tablas con scroll, targets táctiles, sin zoom en iOS).
- [x] **PWA instalable**: manifest + service worker → "Añadir a pantalla de
      inicio" en Android/iOS; se abre como app, con shell offline. Encaja con la
      filosofía self-hosted (sin tienda de apps).
- [x] Portabilidad entre dispositivos vía export/import de sesión (Fase 1).
- [ ] App Android **nativa**: fuera del alcance de este repo (backend + web).
      Es un cliente nuevo con su propio toolchain (Android SDK/Kotlin) y repo
      aparte, consumiendo el mismo backend y el mismo contrato (ver
      `docs/AGENTS.md` y `/q/openapi`). La PWA cubre el uso en móvil mientras
      tanto.

**Entregable parcial:** ✅ la web es usable e instalable como app en el móvil.
La app Android nativa queda como proyecto futuro separado.

---

## Notas transversales

- **Export/import es un requisito de cada fase, no una fase aparte.** Cada nuevo
  recurso nace con su representación JSON portable.
- **Versionar los esquemas** desde el principio (`schemaVersion`) para no romper
  imports antiguos al evolucionar el modelo.
- **Seguridad local:** aunque no haya exposición externa, cifrar credenciales de
  terceros (Suunto) en reposo.
- El orden de las fases es de dependencia, no un compromiso rígido de fechas.
