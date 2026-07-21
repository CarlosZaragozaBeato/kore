import { useEffect, useState } from 'react'
import {
  createWorkout,
  deleteWorkout,
  listWorkouts,
  updateWorkout,
  type Workout,
  type WorkoutRequest,
} from '../api'
import { formatDuration, formatPace, metersToKm } from '../format'
import WorkoutForm from '../components/WorkoutForm'

export default function Home() {
  const [workouts, setWorkouts] = useState<Workout[]>([])
  const [editing, setEditing] = useState<Workout | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function refresh() {
    setError(null)
    try {
      setWorkouts(await listWorkouts())
    } catch (err) {
      setError((err as Error).message)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  async function save(req: WorkoutRequest) {
    setBusy(true)
    setError(null)
    try {
      if (editing) {
        await updateWorkout(editing.id, req)
      } else {
        await createWorkout(req)
      }
      setEditing(null)
      await refresh()
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  async function remove(w: Workout) {
    if (!confirm(`¿Eliminar el entrenamiento del ${w.date}?`)) return
    setBusy(true)
    try {
      await deleteWorkout(w.id)
      await refresh()
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      {error && <p className="error">{error}</p>}

      <WorkoutForm
        key={editing?.id ?? 'new'}
        initial={editing}
        busy={busy}
        onSubmit={save}
        onCancel={editing ? () => setEditing(null) : undefined}
      />

      <section>
        <h2>Entrenamientos ({workouts.length})</h2>
        {workouts.length === 0 ? (
          <p className="muted">Aún no hay entrenamientos. Añade el primero arriba.</p>
        ) : (
          <table className="workouts">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Tipo</th>
                <th>Distancia</th>
                <th>Duración</th>
                <th>Ritmo</th>
                <th>FC</th>
                <th>RPE</th>
                <th>Origen</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {workouts.map((w) => (
                <tr key={w.id}>
                  <td>{w.date}</td>
                  <td>{w.type}</td>
                  <td>{w.distanceMeters != null ? `${metersToKm(w.distanceMeters)} km` : '—'}</td>
                  <td>{formatDuration(w.durationSeconds)}</td>
                  <td>{formatPace(w.paceSecondsPerKm)}</td>
                  <td>{w.avgHeartRate ?? '—'}</td>
                  <td>{w.perceivedEffort ?? '—'}</td>
                  <td>{w.source}</td>
                  <td className="row-actions">
                    <button className="link" onClick={() => setEditing(w)}>
                      Editar
                    </button>
                    <button className="link danger" onClick={() => remove(w)}>
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  )
}
