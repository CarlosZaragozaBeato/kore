import { useEffect, useState } from 'react'
import { createPlan, deletePlan, listPlans, updatePlan, type Plan, type PlanRequest } from '../api'
import PlanForm from '../components/PlanForm'

type Mode = { kind: 'list' } | { kind: 'new' } | { kind: 'edit'; plan: Plan }

export default function Plans() {
  const [plans, setPlans] = useState<Plan[]>([])
  const [mode, setMode] = useState<Mode>({ kind: 'list' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function refresh() {
    setError(null)
    try {
      setPlans(await listPlans())
    } catch (err) {
      setError((err as Error).message)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  async function save(req: PlanRequest) {
    setBusy(true)
    setError(null)
    try {
      if (mode.kind === 'edit') {
        await updatePlan(mode.plan.id, req)
      } else {
        await createPlan(req)
      }
      setMode({ kind: 'list' })
      await refresh()
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  async function remove(plan: Plan) {
    if (!confirm(`¿Eliminar el plan "${plan.name}"?`)) return
    setBusy(true)
    try {
      await deletePlan(plan.id)
      await refresh()
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  if (mode.kind !== 'list') {
    return (
      <section>
        {error && <p className="error">{error}</p>}
        <PlanForm
          initial={mode.kind === 'edit' ? mode.plan : null}
          busy={busy}
          onSave={save}
          onCancel={() => setMode({ kind: 'list' })}
        />
      </section>
    )
  }

  return (
    <section>
      <div className="section-head">
        <h2>Planes</h2>
        <button onClick={() => setMode({ kind: 'new' })}>Nuevo plan</button>
      </div>

      {error && <p className="error">{error}</p>}

      {plans.length === 0 ? (
        <p className="muted">Aún no hay planes. Crea el primero.</p>
      ) : (
        plans.map((plan) => (
          <div className="card plan" key={plan.id}>
            <div className="plan-head">
              <div>
                <strong>{plan.name}</strong>
                {plan.goal && <span className="muted"> · {plan.goal}</span>}
                <div className="muted">
                  {plan.startDate}
                  {plan.endDate ? ` → ${plan.endDate}` : ''}
                </div>
              </div>
              <div className="actions">
                <button className="secondary" onClick={() => setMode({ kind: 'edit', plan })}>
                  Editar
                </button>
                <button className="link danger" onClick={() => remove(plan)}>
                  Eliminar
                </button>
              </div>
            </div>

            <div className="adherence">
              <div className="adherence-bar">
                <div className="adherence-fill" style={{ width: `${plan.adherencePct}%` }} />
              </div>
              <span className="muted">
                {plan.completedCount}/{plan.plannedCount} sesiones · {plan.adherencePct}%
              </span>
            </div>

            <ul className="timeline">
              {plan.sessions.map((s) => (
                <li key={s.id} className={s.done ? 'done' : ''}>
                  <span className="mark">{s.done ? '✓' : '○'}</span>
                  <span>{s.date}</span>
                  <span>{s.type}</span>
                  <span>
                    {s.targetDistanceMeters != null ? `${(s.targetDistanceMeters / 1000).toFixed(1)} km` : ''}
                  </span>
                  {s.description && <span className="muted">{s.description}</span>}
                </li>
              ))}
            </ul>
          </div>
        ))
      )}
    </section>
  )
}
