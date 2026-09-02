import { useState } from 'react'
import type { Plan, PlanRequest, WorkoutType } from '../api'
import { toStepDraft, toStepRequest, type StepDraft } from '../stepDraft'
import StepsEditor from './StepsEditor'

const TYPES: WorkoutType[] = ['RUNNING', 'STRENGTH', 'OTHER']

interface Row {
  date: string
  type: WorkoutType
  km: string
  min: string
  description: string
  done: boolean
  steps: StepDraft[]
  open: boolean
}

function toRows(plan: Plan | null): Row[] {
  if (!plan) return []
  return plan.sessions.map((s) => ({
    date: s.date,
    type: s.type,
    km: s.targetDistanceMeters != null ? String(s.targetDistanceMeters / 1000) : '',
    min: s.targetDurationSeconds != null ? String(Math.round(s.targetDurationSeconds / 60)) : '',
    description: s.description ?? '',
    done: s.done,
    steps: (s.steps ?? []).map(toStepDraft),
    open: false,
  }))
}

function num(value: string): number | null {
  const n = Number(value)
  return value.trim() === '' || Number.isNaN(n) ? null : n
}

interface Props {
  initial: Plan | null
  busy: boolean
  onSave: (req: PlanRequest) => void
  onCancel: () => void
}

export default function PlanForm({ initial, busy, onSave, onCancel }: Props) {
  const [name, setName] = useState(initial?.name ?? '')
  const [goal, setGoal] = useState(initial?.goal ?? '')
  const [startDate, setStartDate] = useState(initial?.startDate ?? new Date().toISOString().slice(0, 10))
  const [endDate, setEndDate] = useState(initial?.endDate ?? '')
  const [rows, setRows] = useState<Row[]>(toRows(initial))

  function addRow() {
    setRows([...rows, { date: startDate, type: 'RUNNING', km: '', min: '', description: '', done: false, steps: [], open: false }])
  }

  function updateRow(i: number, patch: Partial<Row>) {
    setRows(rows.map((r, idx) => (idx === i ? { ...r, ...patch } : r)))
  }

  function removeRow(i: number) {
    setRows(rows.filter((_, idx) => idx !== i))
  }

  function setSteps(i: number, steps: StepDraft[]) {
    setRows(rows.map((r, idx) => (idx === i ? { ...r, steps } : r)))
  }

  function submit(e: React.FormEvent) {
    e.preventDefault()
    onSave({
      name,
      goal: goal.trim() === '' ? null : goal.trim(),
      startDate,
      endDate: endDate === '' ? null : endDate,
      sessions: rows.map((r) => ({
        date: r.date,
        type: r.type,
        targetDistanceMeters: num(r.km) == null ? null : Math.round((num(r.km) as number) * 1000),
        targetDurationSeconds: num(r.min) == null ? null : (num(r.min) as number) * 60,
        description: r.description.trim() === '' ? null : r.description.trim(),
        steps: r.steps.map(toStepRequest),
      })),
    })
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h3>{initial ? 'Editar plan' : 'Nuevo plan'}</h3>
      <div className="grid">
        <label>
          Nombre
          <input value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label>
          Inicio
          <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} required />
        </label>
        <label>
          Fin (opcional)
          <input type="date" value={endDate} min={startDate} onChange={(e) => setEndDate(e.target.value)} />
        </label>
      </div>
      <label>
        Objetivo
        <input value={goal} placeholder="p. ej. bajar de 45' en 10k" onChange={(e) => setGoal(e.target.value)} />
      </label>

      <h4>Sesiones planificadas</h4>
      {rows.length === 0 && <p className="muted">Sin sesiones. Añade la primera.</p>}
      {rows.map((r, i) => (
        <div className="session-block" key={i}>
          <div className="session-row">
            {initial && (r.done ? <span className="done" title="Realizada">✓</span> : <span className="pending">○</span>)}
            <input type="date" value={r.date} onChange={(e) => updateRow(i, { date: e.target.value })} required />
            <select value={r.type} onChange={(e) => updateRow(i, { type: e.target.value as WorkoutType })}>
              {TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
            <input type="number" step="0.01" min="0" placeholder="km" value={r.km} onChange={(e) => updateRow(i, { km: e.target.value })} />
            <input type="number" min="0" placeholder="min" value={r.min} onChange={(e) => updateRow(i, { min: e.target.value })} />
            <input placeholder="descripción" value={r.description} onChange={(e) => updateRow(i, { description: e.target.value })} />
            <button type="button" className="link" onClick={() => updateRow(i, { open: !r.open })} title="Pasos estructurados">
              Pasos{r.steps.length > 0 ? ` (${r.steps.length})` : ''}
            </button>
            <button type="button" className="link danger" onClick={() => removeRow(i)}>
              ✕
            </button>
          </div>

          {r.open && <StepsEditor steps={r.steps} onChange={(steps) => setSteps(i, steps)} />}
        </div>
      ))}
      <button type="button" className="secondary" onClick={addRow}>
        + Añadir sesión
      </button>

      <div className="actions">
        <button type="submit" disabled={busy || name.trim() === ''}>
          Guardar plan
        </button>
        <button type="button" className="secondary" onClick={onCancel} disabled={busy}>
          Cancelar
        </button>
      </div>
    </form>
  )
}
