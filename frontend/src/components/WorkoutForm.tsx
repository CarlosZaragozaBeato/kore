import { useState } from 'react'
import type { Workout, WorkoutRequest, WorkoutType } from '../api'

const TYPES: WorkoutType[] = ['RUNNING', 'STRENGTH', 'OTHER']

function today(): string {
  return new Date().toISOString().slice(0, 10)
}

function num(value: string): number | null {
  const n = Number(value)
  return value.trim() === '' || Number.isNaN(n) ? null : n
}

interface Props {
  initial: Workout | null
  busy: boolean
  onSubmit: (req: WorkoutRequest) => void
  onCancel?: () => void
}

export default function WorkoutForm({ initial, busy, onSubmit, onCancel }: Props) {
  const [date, setDate] = useState(initial?.date ?? today())
  const [type, setType] = useState<WorkoutType>(initial?.type ?? 'RUNNING')
  const [distanceKm, setDistanceKm] = useState(
    initial?.distanceMeters != null ? String(initial.distanceMeters / 1000) : '',
  )
  const [durMin, setDurMin] = useState(
    initial?.durationSeconds != null ? String(Math.floor(initial.durationSeconds / 60)) : '',
  )
  const [durSec, setDurSec] = useState(
    initial?.durationSeconds != null ? String(initial.durationSeconds % 60) : '',
  )
  const [hr, setHr] = useState(initial?.avgHeartRate != null ? String(initial.avgHeartRate) : '')
  const [effort, setEffort] = useState(
    initial?.perceivedEffort != null ? String(initial.perceivedEffort) : '',
  )
  const [notes, setNotes] = useState(initial?.notes ?? '')

  function submit(e: React.FormEvent) {
    e.preventDefault()
    const km = num(distanceKm)
    const min = num(durMin)
    const sec = num(durSec)
    const durationSeconds = min == null && sec == null ? null : (min ?? 0) * 60 + (sec ?? 0)
    onSubmit({
      date,
      type,
      distanceMeters: km == null ? null : Math.round(km * 1000),
      durationSeconds,
      avgHeartRate: num(hr),
      perceivedEffort: num(effort),
      notes: notes.trim() === '' ? null : notes.trim(),
    })
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h3>{initial ? 'Editar entrenamiento' : 'Nuevo entrenamiento'}</h3>
      <div className="grid">
        <label>
          Fecha
          <input type="date" value={date} max={today()} onChange={(e) => setDate(e.target.value)} required />
        </label>
        <label>
          Tipo
          <select value={type} onChange={(e) => setType(e.target.value as WorkoutType)}>
            {TYPES.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </label>
        <label>
          Distancia (km)
          <input type="number" step="0.01" min="0" value={distanceKm} onChange={(e) => setDistanceKm(e.target.value)} />
        </label>
        <label>
          Duración
          <span className="duration">
            <input type="number" min="0" placeholder="min" value={durMin} onChange={(e) => setDurMin(e.target.value)} />
            <input type="number" min="0" max="59" placeholder="seg" value={durSec} onChange={(e) => setDurSec(e.target.value)} />
          </span>
        </label>
        <label>
          FC media (ppm)
          <input type="number" min="0" value={hr} onChange={(e) => setHr(e.target.value)} />
        </label>
        <label>
          Esfuerzo (1-10)
          <input type="number" min="1" max="10" value={effort} onChange={(e) => setEffort(e.target.value)} />
        </label>
      </div>
      <label>
        Notas
        <textarea value={notes} rows={2} onChange={(e) => setNotes(e.target.value)} />
      </label>
      <div className="actions">
        <button type="submit" disabled={busy}>
          {initial ? 'Guardar' : 'Añadir'}
        </button>
        {onCancel && (
          <button type="button" className="secondary" onClick={onCancel} disabled={busy}>
            Cancelar
          </button>
        )}
      </div>
    </form>
  )
}
