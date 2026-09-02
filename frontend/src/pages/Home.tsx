import { useEffect, useState } from 'react'
import { Empty } from '../components/state'
import {
  createWorkout,
  deleteWorkout,
  getAnalytics,
  listWorkouts,
  updateWorkout,
  type Dashboard,
  type Workout,
  type WorkoutRequest,
} from '../api'
import { formatDuration, formatPace, metersToKm } from '../format'
import { disciplineColor, disciplineLabel } from '../discipline'
import WorkoutForm from '../components/WorkoutForm'
import WorkoutDetail from '../components/WorkoutDetail'
import WorkoutAnalysis from '../components/WorkoutAnalysis'

type Sub = 'log' | 'analysis'

function weekLoadFor(date: string, analytics: Dashboard | null): number | null {
  const p = analytics?.weekly.find((w) => w.from <= date && date <= w.to)
  return p ? p.load : null
}

export default function Home() {
  const [workouts, setWorkouts] = useState<Workout[]>([])
  const [analytics, setAnalytics] = useState<Dashboard | null>(null)
  const [sub, setSub] = useState<Sub>('log')
  const [selected, setSelected] = useState<Workout | null>(null)
  const [editing, setEditing] = useState<Workout | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function refresh() {
    setError(null)
    try {
      const [wo, a] = await Promise.all([listWorkouts(), getAnalytics()])
      setWorkouts(wo)
      setAnalytics(a)
    } catch (err) {
      setError((err as Error).message)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  // Mantén el detalle sincronizado con la lista tras editar/refrescar.
  const shown = selected ? workouts.find((w) => w.id === selected.id) ?? null : null

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
      setSelected(null)
      await refresh()
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  // Vista de detalle de un entreno seleccionado.
  if (shown) {
    return (
      <>
        {error && <p className="error">{error}</p>}
        <WorkoutDetail
          workout={shown}
          weekLoad={weekLoadFor(shown.date, analytics)}
          onBack={() => setSelected(null)}
          onEdit={() => {
            setEditing(shown)
            setSelected(null)
            setSub('log')
          }}
          onDelete={() => remove(shown)}
        />
      </>
    )
  }

  return (
    <>
      {error && <p className="error">{error}</p>}

      <div className="nav" style={{ marginBottom: '1rem' }}>
        <button className={sub === 'log' ? 'tab active' : 'tab'} onClick={() => setSub('log')}>
          Registro
        </button>
        <button className={sub === 'analysis' ? 'tab active' : 'tab'} onClick={() => setSub('analysis')}>
          Análisis
        </button>
      </div>

      {sub === 'analysis' ? (
        <section>
          <h2>Análisis por tipo</h2>
          <WorkoutAnalysis workouts={workouts} analytics={analytics} />
        </section>
      ) : (
        <>
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
              <Empty icon="workouts">Aún no hay entrenamientos. Añade el primero arriba.</Empty>
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
                    <th>kcal</th>
                    <th>RPE</th>
                    <th>Origen</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {workouts.map((w) => (
                    <tr
                      key={w.id}
                      className="row-link"
                      role="button"
                      tabIndex={0}
                      aria-label={`Ver detalle del ${w.date}`}
                      onClick={() => setSelected(w)}
                      onKeyDown={(e) => {
                        if (e.target === e.currentTarget && e.key === 'Enter') {
                          setSelected(w)
                        }
                      }}
                    >
                      <td>{w.date}</td>
                      <td>
                        <span className="disc-badge">
                          <span className="dot" style={{ backgroundColor: disciplineColor(w.type) }} />
                          {disciplineLabel(w.type)}
                        </span>
                      </td>
                      <td>{w.distanceMeters != null ? `${metersToKm(w.distanceMeters)} km` : '—'}</td>
                      <td>{formatDuration(w.durationSeconds)}</td>
                      <td>{formatPace(w.paceSecondsPerKm)}</td>
                      <td>{w.avgHeartRate ?? '—'}</td>
                      <td>{w.energyKcal != null ? Math.round(w.energyKcal) : '—'}</td>
                      <td>{w.perceivedEffort ?? '—'}</td>
                      <td>{w.source}</td>
                      <td className="row-actions">
                        <button
                          className="link"
                          onClick={(e) => {
                            e.stopPropagation()
                            setEditing(w)
                          }}
                        >
                          Editar
                        </button>
                        <button
                          className="link danger"
                          onClick={(e) => {
                            e.stopPropagation()
                            void remove(w)
                          }}
                        >
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
      )}
    </>
  )
}
