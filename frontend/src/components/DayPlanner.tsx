import { useState } from 'react'
import {
  createPlannedSession,
  updatePlannedSession,
  type PlannedSession,
  type SessionStatus,
  type WorkoutType,
} from '../api'
import { toStepDraft, toStepRequest, type StepDraft } from '../stepDraft'
import StepsEditor from './StepsEditor'
import { toast } from '../toast'

const TYPES: WorkoutType[] = ['RUNNING', 'STRENGTH', 'OTHER']

function num(value: string): number | null {
  const n = Number(value)
  return value.trim() === '' || Number.isNaN(n) ? null : n
}

interface Props {
  date: string
  initial: PlannedSession | null
  /** Grupo de variantes al que se añade (para proponer alternativas del mismo día). */
  variantGroup?: string | null
  onSaved: () => void
  onCancel: () => void
}

export default function DayPlanner({ date, initial, variantGroup, onSaved, onCancel }: Props) {
  const [type, setType] = useState<WorkoutType>(initial?.type ?? 'RUNNING')
  const [km, setKm] = useState(initial?.targetDistanceMeters != null ? String(initial.targetDistanceMeters / 1000) : '')
  const [min, setMin] = useState(
    initial?.targetDurationSeconds != null ? String(Math.round(initial.targetDurationSeconds / 60)) : '',
  )
  const [description, setDescription] = useState(initial?.description ?? '')
  const [variantLabel, setVariantLabel] = useState(initial?.variantLabel ?? '')
  const [status, setStatus] = useState<SessionStatus>(initial?.status ?? 'PROPOSED')
  const [steps, setSteps] = useState<StepDraft[]>((initial?.steps ?? []).map(toStepDraft))
  const [busy, setBusy] = useState(false)

  // Grupo estable: el de la variante/edición, o uno nuevo (permite añadir variantes luego).
  const group = initial?.variantGroup ?? variantGroup ?? (typeof crypto !== 'undefined' ? crypto.randomUUID() : String(Date.now()))

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setBusy(true)
    const req = {
      date,
      type,
      targetDistanceMeters: num(km) == null ? null : Math.round((num(km) as number) * 1000),
      targetDurationSeconds: num(min) == null ? null : (num(min) as number) * 60,
      description: description.trim() === '' ? null : description.trim(),
      status,
      variantGroup: group,
      variantLabel: variantLabel.trim() === '' ? null : variantLabel.trim(),
      steps: steps.map(toStepRequest),
    }
    try {
      if (initial?.id != null) {
        await updatePlannedSession(initial.id, req)
      } else {
        await createPlannedSession(req)
      }
      toast.success('Sesión planificada')
      onSaved()
    } catch (err) {
      toast.error((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card form day-planner" onSubmit={submit}>
      <div className="section-head">
        <h4 style={{ margin: 0 }}>
          {initial ? 'Editar sesión' : variantGroup ? 'Añadir variante' : 'Planificar sesión'} · {date}
        </h4>
      </div>
      <div className="session-row">
        <select value={type} onChange={(e) => setType(e.target.value as WorkoutType)}>
          {TYPES.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>
        <input type="number" step="0.01" min="0" placeholder="km" value={km} onChange={(e) => setKm(e.target.value)} />
        <input type="number" min="0" placeholder="min" value={min} onChange={(e) => setMin(e.target.value)} />
        <input placeholder="variante (p. ej. Carga normal)" value={variantLabel} onChange={(e) => setVariantLabel(e.target.value)} />
        <select value={status} onChange={(e) => setStatus(e.target.value as SessionStatus)} title="Estado">
          <option value="PROPOSED">Propuesta</option>
          <option value="ACCEPTED">Aceptada</option>
        </select>
      </div>
      <input placeholder="descripción" value={description} onChange={(e) => setDescription(e.target.value)} />

      <StepsEditor steps={steps} onChange={setSteps} />

      <div className="actions">
        <button type="submit" disabled={busy}>
          {initial ? 'Guardar' : 'Añadir al calendario'}
        </button>
        <button type="button" className="secondary" onClick={onCancel} disabled={busy}>
          Cancelar
        </button>
      </div>
    </form>
  )
}
