import { useState } from 'react'
import type { Exercise, Routine, RoutineRequest } from '../api'

interface Row {
  exerciseName: string
  sets: string
  reps: string
  rest: string
  notes: string
}

function toRows(routine: Routine | null): Row[] {
  if (!routine) return []
  return routine.items.map((i) => ({
    exerciseName: i.exerciseName,
    sets: i.sets != null ? String(i.sets) : '',
    reps: i.reps != null ? String(i.reps) : '',
    rest: i.restSeconds != null ? String(i.restSeconds) : '',
    notes: i.notes ?? '',
  }))
}

function int(value: string): number | null {
  const n = parseInt(value, 10)
  return value.trim() === '' || Number.isNaN(n) ? null : n
}

interface Props {
  initial: Routine | null
  exercises: Exercise[]
  busy: boolean
  onSave: (req: RoutineRequest) => void
  onCancel: () => void
}

export default function RoutineForm({ initial, exercises, busy, onSave, onCancel }: Props) {
  const [name, setName] = useState(initial?.name ?? '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [rows, setRows] = useState<Row[]>(toRows(initial))

  function addRow() {
    setRows([...rows, { exerciseName: '', sets: '', reps: '', rest: '', notes: '' }])
  }

  function updateRow(i: number, patch: Partial<Row>) {
    setRows(rows.map((r, idx) => (idx === i ? { ...r, ...patch } : r)))
  }

  function removeRow(i: number) {
    setRows(rows.filter((_, idx) => idx !== i))
  }

  function submit(e: React.FormEvent) {
    e.preventDefault()
    onSave({
      name,
      description: description.trim() === '' ? null : description.trim(),
      items: rows
        .filter((r) => r.exerciseName.trim() !== '')
        .map((r) => ({
          exerciseName: r.exerciseName.trim(),
          sets: int(r.sets),
          reps: int(r.reps),
          restSeconds: int(r.rest),
          notes: r.notes.trim() === '' ? null : r.notes.trim(),
        })),
    })
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h3>{initial ? 'Editar rutina' : 'Nueva rutina'}</h3>
      <label>
        Nombre
        <input value={name} onChange={(e) => setName(e.target.value)} required />
      </label>
      <label>
        Descripción
        <input value={description} onChange={(e) => setDescription(e.target.value)} />
      </label>

      <datalist id="exercise-names">
        {exercises.map((e) => (
          <option key={e.id} value={e.name} />
        ))}
      </datalist>

      <h4>Ejercicios</h4>
      {rows.length === 0 && <p className="muted">Sin ejercicios. Añade el primero.</p>}
      {rows.map((r, i) => (
        <div className="session-row" key={i}>
          <input
            list="exercise-names"
            placeholder="ejercicio"
            value={r.exerciseName}
            onChange={(e) => updateRow(i, { exerciseName: e.target.value })}
          />
          <input type="number" min="0" placeholder="series" value={r.sets} onChange={(e) => updateRow(i, { sets: e.target.value })} />
          <input type="number" min="0" placeholder="reps" value={r.reps} onChange={(e) => updateRow(i, { reps: e.target.value })} />
          <input type="number" min="0" placeholder="descanso s" value={r.rest} onChange={(e) => updateRow(i, { rest: e.target.value })} />
          <input placeholder="notas" value={r.notes} onChange={(e) => updateRow(i, { notes: e.target.value })} />
          <button type="button" className="link danger" onClick={() => removeRow(i)}>
            ✕
          </button>
        </div>
      ))}
      <button type="button" className="secondary" onClick={addRow}>
        + Añadir ejercicio
      </button>

      <div className="actions">
        <button type="submit" disabled={busy || name.trim() === ''}>
          Guardar rutina
        </button>
        <button type="button" className="secondary" onClick={onCancel} disabled={busy}>
          Cancelar
        </button>
      </div>
    </form>
  )
}
