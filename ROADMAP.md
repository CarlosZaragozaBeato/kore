# ROADMAP v1 — CCollector

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

### Fase 3 — Análisis y rendimiento

Objetivo: convertir el log en información útil.

- [ ] Métricas por entreno (ritmo medio, splits, zonas de FC si hay datos).
- [ ] Resúmenes agregados: semana / mes / temporada.
- [ ] Training load y tendencias de carga a lo largo del tiempo (agregable
      entre fuentes: manual + Suunto).
- [ ] Dashboard con evolución de volumen, ritmo y carga.
- [ ] Todos los resúmenes exportables como JSON.

**Entregable:** pantalla de rendimiento con tendencias y resúmenes exportables.

---

### Fase 4 — Planificación de entrenamientos (running)

Objetivo: pasar de registrar a planificar.

- [ ] Modelo de plan de entrenamiento (objetivo, fechas, sesiones planificadas).
- [ ] Calendario de sesiones planificadas vs realizadas.
- [ ] Comparar lo planificado con lo ejecutado (adherencia).
- [ ] Planes exportables/importables como JSON (base para que un agente genere
      planes).

**Entregable:** puedes crear un plan, verlo en calendario y contrastarlo con lo
que realmente entrenaste.

---

### Fase 5 — Rutinas de gimnasio en casa

Objetivo: fuerza y trabajo complementario.

- [ ] Catálogo de ejercicios (grupo muscular, material, descripción).
- [ ] Modelo de rutina (ejercicios, series, repeticiones, descanso).
- [ ] Registro de sesiones de fuerza realizadas.
- [ ] Rutinas y registros exportables/importables como JSON.

**Entregable:** crear rutinas de fuerza en casa, seguirlas y registrar el trabajo.

---

### Fase 6 — Recetas y dietas

Objetivo: nutrición integrada con el entrenamiento.

- [ ] Modelo de receta (ingredientes, pasos, macros/calorías).
- [ ] Modelo de plan de dieta (comidas por día, objetivos nutricionales).
- [ ] Planificación de dietas por periodo.
- [ ] Recetas y dietas exportables/importables como JSON.

**Entregable:** gestionar recetas y planificar dietas dentro de la plataforma.

---

### Fase 7 — Integración con agentes de IA

Objetivo: que agentes puedan planificar y analizar sobre tus datos. Este es el
propósito último de mantener todo en JSON.

- [ ] Documentar y versionar los esquemas JSON de todos los recursos.
- [ ] API estable y descriptiva (contrato claro de lectura/escritura por
      recurso) que un agente pueda consumir.
- [ ] Casos de uso: un agente lee tus entrenos y genera un plan (Fase 4); analiza
      carga y sugiere descarga; propone dietas (Fase 6) según tu volumen.
- [ ] Import de recursos generados por agentes reutilizando el flujo de la Fase 1.

**Entregable:** un agente puede exportar tus datos, razonar sobre ellos y
devolver planes/dietas que la app importa sin fricción.

---

### Fase 8 — Mobile y Android

Objetivo: usar la app cómodamente en el móvil.

- [ ] Web responsive / mobile-first pulido (ya usable desde Fase 1).
- [ ] App Android nativa que consume el mismo backend.
- [ ] Estrategia de sincronización/portabilidad entre dispositivos apoyada en
      export/import (Fase 1).

**Entregable:** app Android funcional sobre el mismo backend y modelo de datos.

---

## Notas transversales

- **Export/import es un requisito de cada fase, no una fase aparte.** Cada nuevo
  recurso nace con su representación JSON portable.
- **Versionar los esquemas** desde el principio (`schemaVersion`) para no romper
  imports antiguos al evolucionar el modelo.
- **Seguridad local:** aunque no haya exposición externa, cifrar credenciales de
  terceros (Suunto) en reposo.
- El orden de las fases es de dependencia, no un compromiso rígido de fechas.
