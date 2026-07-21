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

- **Backend:** Quarkus 3.37 (Java), Liquibase para migraciones, Postgres como
  base de datos. Paquete base `com.zensyra.ccollector.core` organizado por
  dominio (`domain`, `dto`, `repository`, `resource`, `service`, `exception`).
- **Frontend:** React 19 + Vite + TypeScript, oxlint como linter.
- **Migraciones reservadas:** `001-create-users`, `002-suunto-integration`.

El esqueleto actual tiene la estructura de paquetes de `auth` esbozada y el
datasource aún sin configurar (`migrate-at-start=false`) hasta cerrar la Fase 0.

---

## Fases

### Fase 0 — Fundaciones ✅ (en curso)

Objetivo: que el proyecto arranque, compile y migre contra base de datos.

- [ ] Configurar datasource Postgres y activar `migrate-at-start`.
- [ ] `docker-compose` para levantar Postgres en local con un comando.
- [ ] Health check y arranque verificado del backend.
- [ ] Frontend conectado al backend (proxy dev de Vite, variable de API base).
- [ ] Convención de respuestas API (`ResponseDTO`) y manejo de errores.
- [ ] Pipeline mínimo de calidad: tests backend (JUnit5), lint frontend.

**Entregable:** `./up` levanta backend + frontend + DB y la migración corre.

---

### Fase 1 — MVP núcleo: sesión, log de entrenamientos y portabilidad

Los 4 objetivos fundacionales del MVP.

**1.1 · Login simple por username**
- [ ] Crear sesión indicando solo el `username` (sin contraseña).
- [ ] `CollectorUser` + migración `001-create-users`.
- [ ] `AuthResource` / `AuthService` / `UserRepository` funcionales.
- [ ] Frontend: pantalla de entrada + persistencia de la sesión en el
      dispositivo (localStorage / token local).

**1.2 · Log de entrenamientos propio (manual)**
- [ ] Modelo de entrenamiento (fecha, tipo, distancia, duración, ritmo,
      FC, notas, sensaciones).
- [ ] CRUD de entrenamientos vía API.
- [ ] Frontend: crear / editar / listar / ver detalle de un entreno.

**1.3 · Export / import de sesión portable**
- [ ] Endpoint de **export**: vuelca toda la sesión (usuario + entrenos +
      settings) a un único JSON descargable.
- [ ] Endpoint de **import**: reconstruye una sesión desde ese JSON.
- [ ] Versionado del formato de export (`schemaVersion`) para compatibilidad.
- [ ] Frontend: botones de exportar/importar en el área de sesión.

**Entregable:** puedes crear tu sesión, registrar entrenos a mano, exportarlo
todo a un JSON, borrarlo, reimportarlo en otro dispositivo y recuperar tus datos.

---

### Fase 2 — Integración con Suunto

Objetivo: enriquecer el log con datos reales importados de Suunto.

- [ ] Apartado **Settings** para habilitar la integración e introducir
      `client-id`, `client-secret`, `refresh-token`. Migración
      `002-suunto-integration`.
- [ ] Almacenamiento seguro de credenciales (cifrado en reposo).
- [ ] Cliente OAuth: refresco de token y llamadas a la API de Suunto.
- [ ] Sincronización de workouts: importar entrenos y mapearlos al modelo
      interno (evitando duplicados — política de dedup por id de origen).
- [ ] Marcar el origen de cada entreno (`manual` vs `suunto`).
- [ ] Frontend: estado de la integración, sincronizar bajo demanda, ver
      resultado del sync.

**Entregable:** conectas Suunto en Settings y tus entrenos reales aparecen en el
log automáticamente.

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
