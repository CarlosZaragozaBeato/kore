# Kore — Agent Context & Template Guide

This document gives an AI agent everything it needs to (1) understand the Kore
training app and (2) generate **templates** — JSON payloads — that can be
imported into it. It is self-contained; you do not need to read the source.

Kore is also known internally as **CCollector**. It is a **personal,
self-hosted** training platform. Treat it as single-user and private.

---

## 1. Golden rules

1. **Never invent or request Suunto/OAuth secrets.** They are encrypted at rest
   and never leave the server. Nothing you generate should contain credentials.
2. **You only ever create three kinds of things, and all three are the same
   payloads a human POSTs:** workouts (what was done), planned content (plans,
   calendar sessions), and periodization blocks. Importing = POSTing that JSON.
3. **Units are explicit and fixed:** distance in **meters**, duration in
   **seconds**, pace in **seconds per km**, heart rate in **bpm**, dates as
   ISO `YYYY-MM-DD`. Never send km, minutes, or `mm:ss` strings.
4. **Unknown fields are ignored** by the importer (tolerant mapper), so forward
   /backward-compatible extras are safe — but missing required fields fail that
   item only (batch imports isolate bad items).
5. **A `workout` (something done) cannot have a future date** — the server
   rejects it. Planned sessions and blocks *can* be in the future (that's the
   point). Don't generate "completed" workouts in the future.

---

## 2. Session & connection

Every request identifies the user with a header — there is no password:

```
X-CCollector-Username: <username>
```

Base URL (dev): `http://localhost:8080/api/v1`. The username is created on first
use (`POST /api/v1/auth/login` with `{"username":"..."}`), but any resource call
with the header also works for an existing session.

### Read the live contract first

Prefer these over this document if they disagree (they are generated from code):

- `GET /api/v1/agent/manifest` — machine-readable map of every resource, its
  routes, the current `schemaVersion`, and the session header name.
- `GET /api/v1/agent/context` — the user's full exported data **plus** an
  analytics dashboard, so you can tailor content to their actual history/load.
- **`GET /api/v1/context/training?from=YYYY-MM-DD&to=YYYY-MM-DD`** — the
  **recommended input for weekly planning**: a compact, self-describing bundle
  for a date range (workouts done, planned sessions, overlapping periodization
  blocks, load signals) plus a `guidance` block telling you the units and the
  exact return format. Ask the user for a range (e.g. the last 4 weeks), read
  this, then return the week's sessions as described below.
- `GET /q/swagger-ui` — OpenAPI UI.

### The training-context bundle (`/context/training`)

Shape (secrets-free, no DB ids):

```jsonc
{
  "schemaVersion": 1,
  "from": "2026-07-01", "to": "2026-07-28",
  "username": "carlos",
  "guidance": { "purpose": "...", "units": "...", "returnFormat": "...", "workoutTypes": [...], "stepKinds": [...] },
  "summary": { "workoutCount": 12, "totalDistanceMeters": 96000, "totalDurationSeconds": 30600,
               "plannedCount": 3, "load": { "acwr": 1.1, "acwrState": "OK", "monotony": ..., "rampPct": ... } },
  "workouts": [ { "date": "...", "type": "RUNNING", "distanceMeters": 8000, "durationSeconds": 2400,
                  "paceSecondsPerKm": 300, "avgHeartRate": 150, ... } ],
  "planned":  [ { "date": "...", "type": "RUNNING", "steps": [ ... ] } ],
  "blocks":   [ { "level": "MESO", "focus": "BUILD", "name": "...", "startDate": "...", "endDate": "...", "loadStance": "BUILD" } ]
}
```

Use `summary.load` to size the week (respect ACWR/monotony/ramp) and `blocks`
to honour the active periodization phase. Then produce planned sessions per
§4.2 and import via `POST /api/v1/kdl/import/planned`.

---

## 3. How to import: two surfaces

### A. Full session document

`POST /api/v1/session/import` takes one big portable document
(`schemaVersion 15`, the same shape `GET /api/v1/session/export` returns). Use
this to seed a **fresh** session/device. It refuses if the username already
exists (so it won't clobber data). Usually you want surface B instead.

### B. KDL — Kore Data Language (recommended for agents)

KDL imports individual resources or batches. Every item is wrapped as:

```json
{ "koreType": "planned", "schemaVersion": 1, "payload": { /* the resource */ } }
```

Two endpoints:

| Endpoint | Body it accepts |
|---|---|
| `POST /api/v1/kdl/import/{type}` | a **single payload**, an **array of payloads**, or a full KDL document. The `{type}` in the path is the `koreType`. This is the easiest path. |
| `POST /api/v1/kdl/import` | a full **KDL document**: `{ "kore":"kdl", "kdlVersion":1, "items":[ {koreType, schemaVersion, payload}, … ] }`. Use to mix several types in one call. |

Import is **not** atomic across a batch: each item is created in its own
transaction, so one bad item is skipped (reported in `errors`), the rest import.
The response is:

```json
{ "imported": 3, "skipped": 1, "errors": ["item #4 (planned): ..."], "byType": { "planned": 3 } }
```

`schemaVersion` per item may be omitted (defaults to current); a value **higher**
than the server supports is rejected for that item. All types are currently at
version `1`.

### Recognised `koreType` values

`workout`, `plan`, `planned`, `block`, `exercise`, `ingredient`, `recipe`,
`routine`, `dietPlan`, `weightEntry`, `sessionTemplate`, `dailyActivity`.

This cycle the active focus is `workout`, `plan`, `planned`, `block`. The
nutrition/gym/weight types still work but their UI is hidden.

---

## 4. Payload shapes (the templates)

Enums (send the exact uppercase token):

| Enum | Values |
|---|---|
| `WorkoutType` | `RUNNING`, `CYCLING`, `SWIMMING`, `STRENGTH`, `OTHER` |
| `StepKind` | `WARMUP`, `INTERVAL`, `RECOVERY`, `STEADY`, `COOLDOWN` |
| `SessionStatus` | `PROPOSED`, `ACCEPTED`, `REJECTED` |
| `BlockLevel` | `MACRO`, `MESO`, `MICRO` |
| `BlockFocus` | `BASE`, `BUILD`, `PEAK`, `TAPER`, `RECOVERY`, `RACE`, `GENERAL` |
| `LoadStance` | `DELOAD`, `MAINTAIN`, `BUILD` |

### 4.1 Structured step (shared by plans & planned sessions)

A planned session's body is a list of these. All targets optional; send what you
mean.

```json
{
  "kind": "INTERVAL",
  "repeat": 5,
  "targetDistanceMeters": 1000,
  "targetDurationSeconds": null,
  "targetPaceMinSecPerKm": 225,
  "targetPaceMaxSecPerKm": 240,
  "targetHrMin": 150,
  "targetHrMax": 170,
  "recoverySeconds": 90,
  "note": "5x1k at threshold"
}
```

- Use **either** `targetDistanceMeters` **or** `targetDurationSeconds` per step.
- `repeat` defaults to 1. For continuous work use `STEADY` with distance/time;
  for reps use `INTERVAL` + `repeat` + `recoverySeconds`.
- Pace is **seconds/km** (e.g. `4:00/km` → `240`; `3:45/km` → `225`).

### 4.2 `planned` — a calendar session (`koreType: "planned"`)

```json
{
  "date": "2026-09-01",
  "type": "RUNNING",
  "targetDistanceMeters": 10000,
  "targetDurationSeconds": null,
  "description": "Threshold intervals",
  "status": "ACCEPTED",
  "variantGroup": null,
  "variantLabel": null,
  "steps": [
    { "kind": "WARMUP", "targetDistanceMeters": 2000 },
    { "kind": "INTERVAL", "repeat": 5, "targetDistanceMeters": 1000, "recoverySeconds": 90,
      "targetPaceMinSecPerKm": 225, "targetPaceMaxSecPerKm": 240 },
    { "kind": "COOLDOWN", "targetDistanceMeters": 1500 }
  ]
}
```

- **A whole week/month** = a JSON **array** of these to
  `POST /api/v1/kdl/import/planned`. Multiple entries can share a `date`.
- **Variants for one day**: give two+ sessions the **same** `variantGroup`
  string and different `variantLabel` (e.g. "Normal load" / "Deload"). In the
  app, accepting one rejects its siblings.
- `status` defaults to `PROPOSED` if omitted. Use `ACCEPTED` for a committed
  plan you want to count immediately.

### 4.3 `plan` — a named training plan (`koreType: "plan"`)

Imports as **one item** (the plan carries its sessions).

```json
{
  "name": "Marathon — 8 weeks",
  "goal": "MARATHON",
  "startDate": "2026-08-01",
  "endDate": "2026-09-26",
  "sessions": [
    { "date": "2026-08-01", "type": "RUNNING", "targetDistanceMeters": 12000, "description": "Easy",
      "steps": [ { "kind": "STEADY", "targetDistanceMeters": 12000, "targetPaceMinSecPerKm": 300, "targetPaceMaxSecPerKm": 330 } ] },
    { "date": "2026-08-03", "type": "RUNNING", "targetDistanceMeters": 8000, "description": "Intervals",
      "steps": [ { "kind": "INTERVAL", "repeat": 6, "targetDistanceMeters": 800, "recoverySeconds": 120 } ] }
  ]
}
```

`goal` is free text in the plan (the strict race-goal enum lives on
`sessionTemplate`, not here).

### 4.4 `block` — periodization (`koreType: "block"`)

A macro/meso/micro tree. **Children nest under `children`**, so a whole season
imports as **one item**. The server assigns `parentId` from the nesting; each
child's `level` must be finer than its parent (`MACRO` > `MESO` > `MICRO`).
Blocks relate to sessions by **date overlap** — keep child ranges inside the
parent's range.

```json
{
  "level": "MACRO",
  "focus": "BASE",
  "name": "Ironman season",
  "startDate": "2026-09-01",
  "endDate": "2027-03-31",
  "loadStance": "BUILD",
  "note": "Base → build → peak → taper",
  "children": [
    {
      "level": "MESO", "focus": "BUILD", "name": "Build block 1",
      "startDate": "2026-09-01", "endDate": "2026-09-28",
      "children": [
        { "level": "MICRO", "focus": "BUILD", "name": "Week 1", "startDate": "2026-09-01", "endDate": "2026-09-07", "loadStance": "BUILD" },
        { "level": "MICRO", "focus": "RECOVERY", "name": "Week 4 (deload)", "startDate": "2026-09-22", "endDate": "2026-09-28", "loadStance": "DELOAD" }
      ]
    }
  ]
}
```

To attach to an **existing** block instead of nesting, set `"parentId": <id>`
(and omit `children`).

### 4.5 `workout` — something actually done (`koreType: "workout"`)

Only for real/manual history. Remember: **no future dates**.

```json
{
  "date": "2026-07-21",
  "type": "RUNNING",
  "distanceMeters": 8000,
  "durationSeconds": 2400,
  "avgHeartRate": 150,
  "maxHeartRate": 172,
  "energyKcal": 620,
  "stepCount": 7400,
  "perceivedEffort": 6,
  "notes": "Felt good"
}
```

`perceivedEffort` is 1–10. Cadence and stride are **derived** from `stepCount`;
don't send them.

---

## 5. Common agent tasks → recipe

| Task | How |
|---|---|
| **Recommend this week's training** | `GET /context/training?from&to` → size from `summary.load` + `blocks` → `POST /kdl/import/planned` with an array (§4.2) |
| Add one session to a day | `POST /kdl/import/planned` with one payload (§4.2) |
| Add a full training week | `POST /kdl/import/planned` with an **array** (§4.2) |
| Offer choices for a day | Two payloads, same `variantGroup`, different `variantLabel` |
| Deliver a structured plan | `POST /kdl/import/plan` with one plan (§4.3) |
| Lay out a season's structure | `POST /kdl/import/block` with one nested macro (§4.4) |
| Backfill past workouts | `POST /kdl/import/workout` with an array (§4.5), past dates only |
| Mix everything at once | `POST /kdl/import` with a KDL document listing items of several `koreType`s |

Personalise using `GET /api/v1/agent/context`: it returns the user's history and
a load dashboard (weekly volume, ACWR/monotony/ramp signals), so you can size
volume, place deload weeks, and choose a `loadStance` that fits their current
fatigue rather than guessing.

Ready-to-edit example files live in [`templates/`](templates/).

---

## 6. Verifying an import

The KDL response tells you exactly what landed (`imported`, `skipped`, `errors`,
`byType`). To confirm from the user's side, read back:

- `GET /api/v1/planned` — standalone calendar sessions
- `GET /api/v1/plans` — plans with their sessions
- `GET /api/v1/blocks` — the periodization tree; `GET /api/v1/blocks/{id}/summary`
  gives planned-vs-done over that block's range
- `GET /api/v1/workouts` — logged workouts

If you asked for a comparison of a planned day vs what was done, use
`GET /api/v1/planned/{id}/comparison`.
