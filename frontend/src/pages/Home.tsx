import { useEffect, useState } from 'react'
import {
  createWorkout,
  deleteWorkout,
  exportSession,
  listWorkouts,
  updateWorkout,
  type Workout,
  type WorkoutRequest,
} from '../api'
import { formatDuration, formatPace, metersToKm } from '../format'
import WorkoutForm from '../components/WorkoutForm'

interface Props {
  username: string
  onLogout: () => void
}

export default function Home({ username, onLogout }: Props) {
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

  async function doExport() {
    setError(null)
    try {
      const doc = await exportSession()
      const blob = new Blob([JSON.stringify(doc, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `ccollector-${username}-${new Date().toISOString().slice(0, 10)}.json`
      a.click()
      URL.revokeObjectURL(url)
    } catch (err) {
      setError((err as Error).message)
    }
  }

  return (
    <main className="app">
      <header className="topbar">
        <div>
          <strong>CCollector</strong> · <span className="muted">{username}</span>
        </div>
        <div className="actions">
          <button className="secondary" onClick={doExport}>
            Exportar sesión
          </button>
          <button className="secondary" onClick={onLogout}>
            Salir
          </button>
        </div>
      </header>

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
    </main>
  )
}
