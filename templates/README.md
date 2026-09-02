# Kore import templates

Ready-to-edit example payloads an agent (or a human) can import into Kore. Read
[`../AGENT_CONTEXT.md`](../AGENT_CONTEXT.md) for the full field reference, units,
and rules. All examples use the KDL section import.

Every request needs the session header:

```
X-CCollector-Username: <your-username>
```

| File | `koreType` | Import with |
|---|---|---|
| [`planned-week.json`](planned-week.json) | `planned` | `POST /api/v1/kdl/import/planned` — an array = a whole week; includes a same-day **variant** pair (`variantGroup` `w1-sat`). |
| [`training-plan.json`](training-plan.json) | `plan` | `POST /api/v1/kdl/import/plan` — one plan carrying its sessions (imports as a single item). |
| [`periodization-season.json`](periodization-season.json) | `block` | `POST /api/v1/kdl/import/block` — one macro with nested meso/micro children (a whole season in one item). |
| [`workouts-backfill.json`](workouts-backfill.json) | `workout` | `POST /api/v1/kdl/import/workout` — an array of **past** workouts (future dates are rejected). |
| [`kdl-mixed-document.json`](kdl-mixed-document.json) | (mixed) | `POST /api/v1/kdl/import` — a full KDL document mixing a `block`, a `plan` and `planned` sessions in one call. |

Example call:

```bash
curl -X POST http://localhost:8080/api/v1/kdl/import/planned \
  -H 'Content-Type: application/json' \
  -H 'X-CCollector-Username: carlos' \
  --data-binary @planned-week.json
```

The response reports `imported`, `skipped`, `errors`, and `byType`. A bad item
is skipped without failing the rest of the batch.

> Units: meters, seconds, seconds/km, bpm, ISO dates. Update the dates before
> importing — the planned/block examples use future dates (fine), the workout
> example uses past dates (required).
