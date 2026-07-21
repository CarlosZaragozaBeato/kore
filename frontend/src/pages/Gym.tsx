import { useEffect, useState } from 'react'
import {
  createExercise,
  createRoutine,
  createStrengthSession,
  deleteExercise,
  deleteRoutine,
  deleteStrengthSession,
  listExercises,
  listRoutines,
  listStrengthSessions,
  updateRoutine,
  type Exercise,
  type Routine,
  type RoutineRequest,
  type StrengthSession,
} from '../api'
import RoutineForm from '../components/RoutineForm'

type Sub = 'exercises' | 'routines' | 'log'

export default function Gym() {
  const [sub, setSub] = useState<Sub>('exercises')
  const [exercises, setExercises] = useState<Exercise[]>([])
  const [routines, setRoutines] = useState<Routine[]>([])
  const [sessions, setSessions] = useState<StrengthSession[]>([])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  // exercise form
  const [exName, setExName] = useState('')
  const [exGroup, setExGroup] = useState('')
  const [exEquip, setExEquip] = useState('')

  // routine editing
  const [routineMode, setRoutineMode] = useState<'list' | 'new' | Routine>('list')

  // strength log form
  const [logDate, setLogDate] = useState(new Date().toISOString().slice(0, 10))
  const [logRoutineId, setLogRoutineId] = useState('')
  const [logNotes, setLogNotes] = useState('')

  async function refresh() {
    setError(null)
    try {
      const [e, r, s] = await Promise.all([listExercises(), listRoutines(), listStrengthSessions()])
      setExercises(e)
      setRoutines(r)
      setSessions(s)
    } catch (err) {
      setError((err as Error).message)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  async function run(fn: () => Promise<unknown>) {
    setBusy(true)
    setError(null)
    try {
      await fn()
      await refresh()
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  async function addExercise(e: React.FormEvent) {
    e.preventDefault()
    if (exName.trim() === '') return
    await run(async () => {
      await createExercise({
        name: exName,
        muscleGroup: exGroup.trim() === '' ? null : exGroup.trim(),
        equipment: exEquip.trim() === '' ? null : exEquip.trim(),
        description: null,
      })
      setExName('')
      setExGroup('')
      setExEquip('')
    })
  }

  async function saveRoutine(req: RoutineRequest) {
    await run(async () => {
      if (routineMode !== 'list' && routineMode !== 'new') {
        await updateRoutine(routineMode.id, req)
      } else {
        await createRoutine(req)
      }
      setRoutineMode('list')
    })
  }

  async function addSession(e: React.FormEvent) {
    e.preventDefault()
    await run(async () => {
      await createStrengthSession({
        date: logDate,
        routineId: logRoutineId === '' ? null : Number(logRoutineId),
        notes: logNotes.trim() === '' ? null : logNotes.trim(),
      })
      setLogNotes('')
    })
  }

  return (
    <section>
      <div className="nav" style={{ marginBottom: '1rem' }}>
        <button className={sub === 'exercises' ? 'tab active' : 'tab'} onClick={() => setSub('exercises')}>
          Ejercicios
        </button>
        <button className={sub === 'routines' ? 'tab active' : 'tab'} onClick={() => setSub('routines')}>
          Rutinas
        </button>
        <button className={sub === 'log' ? 'tab active' : 'tab'} onClick={() => setSub('log')}>
          Registro
        </button>
      </div>

      {error && <p className="error">{error}</p>}

      {sub === 'exercises' && (
        <>
          <form className="card form" onSubmit={addExercise}>
            <h3>Nuevo ejercicio</h3>
            <div className="grid">
              <label>
                Nombre
                <input value={exName} onChange={(e) => setExName(e.target.value)} required />
              </label>
              <label>
                Grupo muscular
                <input value={exGroup} onChange={(e) => setExGroup(e.target.value)} />
              </label>
              <label>
                Material
                <input value={exEquip} onChange={(e) => setExEquip(e.target.value)} />
              </label>
            </div>
            <div className="actions">
              <button type="submit" disabled={busy || exName.trim() === ''}>
                Añadir
              </button>
            </div>
          </form>

          <h3>Catálogo ({exercises.length})</h3>
          {exercises.length === 0 ? (
            <p className="muted">Sin ejercicios todavía.</p>
          ) : (
            <table className="workouts">
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Grupo</th>
                  <th>Material</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {exercises.map((ex) => (
                  <tr key={ex.id}>
                    <td>{ex.name}</td>
                    <td>{ex.muscleGroup ?? '—'}</td>
                    <td>{ex.equipment ?? '—'}</td>
                    <td className="row-actions">
                      <button className="link danger" onClick={() => run(() => deleteExercise(ex.id))}>
                        Eliminar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}

      {sub === 'routines' &&
        (routineMode !== 'list' ? (
          <RoutineForm
            initial={routineMode === 'new' ? null : routineMode}
            exercises={exercises}
            busy={busy}
            onSave={saveRoutine}
            onCancel={() => setRoutineMode('list')}
          />
        ) : (
          <>
            <div className="section-head">
              <h3>Rutinas</h3>
              <button onClick={() => setRoutineMode('new')}>Nueva rutina</button>
            </div>
            {routines.length === 0 ? (
              <p className="muted">Sin rutinas todavía.</p>
            ) : (
              routines.map((r) => (
                <div className="card plan" key={r.id}>
                  <div className="plan-head">
                    <div>
                      <strong>{r.name}</strong>
                      {r.description && <span className="muted"> · {r.description}</span>}
                    </div>
                    <div className="actions">
                      <button className="secondary" onClick={() => setRoutineMode(r)}>
                        Editar
                      </button>
                      <button className="link danger" onClick={() => run(() => deleteRoutine(r.id))}>
                        Eliminar
                      </button>
                    </div>
                  </div>
                  <ul className="timeline">
                    {r.items.map((it, idx) => (
                      <li key={idx}>
                        <span>{it.exerciseName}</span>
                        <span className="muted">
                          {it.sets ?? '?'}×{it.reps ?? '?'}
                          {it.restSeconds ? ` · ${it.restSeconds}s` : ''}
                        </span>
                        {it.notes && <span className="muted">{it.notes}</span>}
                      </li>
                    ))}
                  </ul>
                </div>
              ))
            )}
          </>
        ))}

      {sub === 'log' && (
        <>
          <form className="card form" onSubmit={addSession}>
            <h3>Registrar sesión de fuerza</h3>
            <div className="grid">
              <label>
                Fecha
                <input type="date" value={logDate} max={new Date().toISOString().slice(0, 10)} onChange={(e) => setLogDate(e.target.value)} required />
              </label>
              <label>
                Rutina
                <select value={logRoutineId} onChange={(e) => setLogRoutineId(e.target.value)}>
                  <option value="">(libre)</option>
                  {routines.map((r) => (
                    <option key={r.id} value={r.id}>
                      {r.name}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <label>
              Notas
              <input value={logNotes} onChange={(e) => setLogNotes(e.target.value)} />
            </label>
            <div className="actions">
              <button type="submit" disabled={busy}>
                Registrar
              </button>
            </div>
          </form>

          <h3>Sesiones ({sessions.length})</h3>
          {sessions.length === 0 ? (
            <p className="muted">Sin sesiones registradas.</p>
          ) : (
            <table className="workouts">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Rutina</th>
                  <th>Notas</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {sessions.map((s) => (
                  <tr key={s.id}>
                    <td>{s.date}</td>
                    <td>{s.routineName ?? '(libre)'}</td>
                    <td>{s.notes ?? '—'}</td>
                    <td className="row-actions">
                      <button className="link danger" onClick={() => run(() => deleteStrengthSession(s.id))}>
                        Eliminar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </section>
  )
}
