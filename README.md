# Kore

> Personal, self-hosted training platform for runners, triathletes and Ironman
> athletes. Internally the project is still named **CCollector** (package
> `com.zensyra.ccollector`); **Kore** is the product/brand name.

Kore is a private, own-your-data training log and planner in the spirit of
TrainingPeaks — but small, self-hosted, and built to be driven by AI agents. It
turns "record and review" into "**plan and contrast**": you plan structured
sessions on a calendar, they get compared against what you actually did (manual
or imported from Suunto), and you organise the season into
macro / meso / micro periodization blocks.

## Design principles

- **Personal use only.** Not meant to be exposed to the internet. There is no
  password: you identify a session by a username (login-or-create).
- **Own your data.** Everything you own is portable as a single JSON document
  (`GET /api/v1/session/export`) and can be re-imported on another device.
- **Minimal retention.** Suunto-sourced data is recoverable from Suunto, so it
  is not kept indefinitely; manual data is yours and stays.
- **Secrets stay secret.** Suunto credentials are encrypted at rest
  (AES-256-GCM) and are **never** returned by the API nor included in any
  export/import.
- **Agent-ready.** A machine-readable manifest (`GET /api/v1/agent/manifest`)
  and a data language (KDL) let an AI agent read context and generate content to
  import. See [`AGENT_CONTEXT.md`](AGENT_CONTEXT.md).

## Stack

| Layer | Tech |
|---|---|
| Backend | Java 21, Quarkus 3.37, Hibernate ORM + Panache (repository pattern), Liquibase, SQLite (`backend/local.db`) |
| Frontend | React 19, Vite 8, TypeScript, hash routing, oxlint |
| Integrations | Suunto Cloud (OAuth + workouts sync) |
| Docs | OpenAPI / Swagger UI at `/q/swagger-ui` |

## Run it

Requires **Java 21** (pinned via `mise` in `backend/.mise.toml`) and **Node**.

```bash
./up
```

`./up` starts both servers and stops them together on Ctrl-C:

- Backend — Quarkus dev, <http://localhost:8080> (API under `/api/v1`, health at
  `/q/health`, OpenAPI UI at `/q/swagger-ui`).
- Frontend — Vite dev, <http://localhost:5173> (proxies `/api` → `:8080`).

To run the halves separately:

```bash
cd backend  && ./mvnw quarkus:dev          # or: mise exec -- ./mvnw quarkus:dev
cd frontend && npm install && npm run dev
```

The SQLite schema is created/upgraded automatically by Liquibase at startup
(`quarkus.liquibase.migrate-at-start=true`).

### Verify

```bash
cd backend  && JAVA_HOME=$HOME/.local/share/mise/installs/java/21.0.2 ./mvnw -B test   # 98 tests
cd frontend && npm run lint && npm run build
```

## Configuration

Config lives in `backend/src/main/resources/application.properties`. Sensible
dev defaults are baked in; the only value you must override for real use is the
encryption key:

| Setting | Property | Notes |
|---|---|---|
| HTTP port | `quarkus.http.port` | default `8080` |
| Database | `quarkus.datasource.jdbc.url` | default `jdbc:sqlite:local.db` |
| Encryption key | `ccollector.crypto.secret` | AES key, base64 of 16/24/32 bytes. **In production override with the `CCOLLECTOR_CRYPTO_SECRET` env var** (`openssl rand -base64 32`). |
| Suunto OAuth/API | `quarkus.rest-client.suunto-*.url` | Suunto Cloud endpoints |

> Note: the root `.env` is a leftover reference from the archived predecessor
> platform (Postgres/Strava/Docker) and does **not** describe the current MVP.
> The current app uses SQLite and the properties above.

## App sections

The UI is deliberately scoped this cycle (ROADMAP v4) to four focus sections
plus Settings. Gym, Nutrition, Weight and Analytics still exist in code and by
direct hash route, but are hidden from the nav.

| Section | Route | What it is |
|---|---|---|
| **Inicio** (Home) | `#/home` | Dashboard linking each metric to its section |
| **Calendario** | `#/calendar` | Month/week calendar: workouts done, planned sessions, plan-vs-actual comparison, and the periodization panel |
| **Entrenos** (Workouts) | `#/workouts` | Manual workout log |
| **Planes** (Plans) | `#/plans` | Training plans with structured, stepped sessions |
| Ajustes (Settings) | `#/settings` | Suunto integration config, session export, agent-integration info |

## How the domain fits together

- **Workout** — something you actually did (manual or `SUUNTO`). A flat summary
  (distance, duration, HR, energy, steps…), not a lap-by-lap trace.
- **Training plan** (`/plans`) — a named plan over a date window containing
  planned sessions.
- **Planned session** (`/planned`) — a session placed directly on the calendar
  (no plan), with a status (`PROPOSED`/`ACCEPTED`/`REJECTED`) and optional
  **variants** for a day (accepting one rejects its siblings).
- **Structured steps** — a planned session is a list of steps
  (`WARMUP`/`INTERVAL`/`RECOVERY`/`STEADY`/`COOLDOWN`) with target
  distance/time, pace min–max, HR min–max, repeats and recovery.
- **Comparison** — derived, no storage: matches a planned session with the day's
  workout and bands each metric `BELOW`/`WITHIN`/`ABOVE`.
- **Periodization block** (`/blocks`) — a `MACRO`/`MESO`/`MICRO` block over a
  date range with a focus and optional load stance, nested via `parentId`. Its
  relation to sessions is derived by date overlap.

## Data ownership & portability

- **Full session** — `GET /api/v1/session/export` → one JSON document
  (`schemaVersion 15`, no DB ids); `POST /api/v1/session/import` rebuilds it on a
  fresh session. Never contains Suunto secrets.
- **Piecewise (KDL)** — Kore Data Language: import/export individual resources or
  batches with a common wrapper `{ koreType, schemaVersion, payload }`. This is
  the path an agent uses to add "a whole training block" or "a month of the
  calendar" in one shot. See [`AGENT_CONTEXT.md`](AGENT_CONTEXT.md) and
  [`templates/`](templates/).
- **Ranged training context** — `GET /api/v1/context/training?from&to` returns a
  compact, self-describing bundle for a date range (workouts, planned sessions,
  overlapping blocks, load signals + guidance) meant to hand to an AI agent so it
  can recommend the week's sessions and return them ready to import. In the app:
  **Planes → "Entrenar con un agente"**.

## Repository layout

```
backend/    Quarkus API (com.zensyra.ccollector.core: domain / dto / repository / service / resource)
frontend/   React + Vite SPA (src/pages, src/components, src/api.ts)
docs/       AGENTS.md (API map, Spanish) + brand assets
templates/  Ready-to-import KDL/JSON examples for agents (see AGENT_CONTEXT.md)
ROADMAP.md  Product roadmap (v1→v4; v4 complete)
up          Dev launcher for backend + frontend
```

## Status

ROADMAP **v4 is complete** (phases A–F): compact nav, structured stepped
sessions, per-day planning with variants, planned-vs-actual comparison, batch
import + load intelligence, and long-range periodization. The project is being
**parked** for a while; this README and `AGENT_CONTEXT.md` are the handoff.

## Documentation

- [`AGENT_CONTEXT.md`](AGENT_CONTEXT.md) — context and template-generation guide
  for AI agents (English).
- [`docs/AGENTS.md`](docs/AGENTS.md) — detailed API/resource map (Spanish).
- Live, always-accurate contract: `GET /api/v1/agent/manifest` and
  `/q/swagger-ui`.
