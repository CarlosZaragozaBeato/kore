import { useEffect, useState } from 'react'
import { createPlan, deletePlan, listPlans, updatePlan, type Plan, type PlanRequest } from '../api'
import PlanForm from '../components/PlanForm'
import { formatStep } from '../steps'
import { Empty } from '../components/state'
import SessionTemplates from '../components/SessionTemplates'
import SectionImport from '../components/SectionImport'
import TrainingContextExport from '../components/TrainingContextExport'

type Mode = { kind: 'list' } | { kind: 'new' } | { kind: 'edit'; plan: Plan }
type Sub = 'plans' | 'templates'

function SubNav({ sub, setSub }: { sub: Sub; setSub: (s: Sub) => void }) {
  return (
    <div className="nav" style={{ marginBottom: '1rem' }}>
      <button className={sub === 'plans' ? 'tab active' : 'tab'} onClick={() => setSub('plans')}>
        Planes
      </button>
      <button className={sub === 'templates' ? 'tab active' : 'tab'} onClick={() => setSub('templates')}>
        Plantillas
      </button>
    </div>
  )
}

export default function Plans() {
  const [sub, setSub] = useState<Sub>('plans')
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

  if (sub === 'templates') {
    return (
      <section>
        <SubNav sub={sub} setSub={setSub} />
        <SessionTemplates />
      </section>
    )
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
      <SubNav sub={sub} setSub={setSub} />
      <div className="section-head">
        <h2>Planes</h2>
        <button onClick={() => setMode({ kind: 'new' })}>Nuevo plan</button>
      </div>

      <div className="card agent-handoff">
        <h3 style={{ margin: '0 0 0.2rem' }}>Entrenar con un agente</h3>
        <p className="muted" style={{ marginTop: 0 }}>
          Descarga tu contexto de entreno de un rango, pídele a un agente las sesiones de la semana e
          impórtalas de vuelta. Como <strong>Plan</strong> (con nombre y fechas) o como{' '}
          <strong>Sesiones de calendario</strong> (caen directas en el calendario, con sus rangos de
          ritmo).
        </p>
        <TrainingContextExport />
        <SectionImport
          types={[
            { key: 'planned', label: 'Sesiones de calendario' },
            { key: 'plan', label: 'Plan' },
          ]}
          onImported={refresh}
        />
      </div>

      {error && <p className="error">{error}</p>}

      {plans.length === 0 ? (
        <Empty icon="plans">Aún no hay planes. Crea el primero.</Empty>
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
                  {s.steps && s.steps.length > 0 && (
                    <ol className="step-summary">
                      {s.steps.map((st, si) => (
                        <li key={si}>{formatStep(st)}</li>
                      ))}
                    </ol>
                  )}
                </li>
              ))}
            </ul>
          </div>
        ))
      )}
    </section>
  )
}
