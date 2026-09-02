import { useEffect, useState } from 'react'
import { toast } from '../toast'
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
  listWeight,
  seedExercises,
  updateRoutine,
  updateStrengthSession,
  type Exercise,
  type ExerciseCategory,
  type Routine,
  type RoutineRequest,
  type StrengthSession,
  type StrengthStatus,
} from '../api'
import RoutineForm from '../components/RoutineForm'
import SectionImport from '../components/SectionImport'
import KoreIcon from '../components/icons'
import { Empty } from '../components/state'

type Sub = 'exercises' | 'routines' | 'log'

const EX_CATEGORIES: { value: ExerciseCategory; label: string }[] = [
  { value: 'WARMUP', label: 'Calentamiento' },
  { value: 'STRENGTH', label: 'Fuerza' },
  { value: 'RECOVERY', label: 'Recuperación' },
]

function categoryLabel(c: ExerciseCategory | null): string {
  return EX_CATEGORIES.find((x) => x.value === c)?.label ?? '—'
}

function today(): string {
  return new Date().toISOString().slice(0, 10)
}

/** kcal/min ≈ MET · 3.5 · pesoKg / 200 (fórmula estándar de MET). */
function kcalPerMinute(met: number, weightKg: number): number {
  return (met * 3.5 * weightKg) / 200
}

export default function Gym() {
  const [sub, setSub] = useState<Sub>('exercises')
  const [exercises, setExercises] = useState<Exercise[]>([])
  const [routines, setRoutines] = useState<Routine[]>([])
  const [sessions, setSessions] = useState<StrengthSession[]>([])
  const [weightKg, setWeightKg] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  // exercise form
  const [exName, setExName] = useState('')
  const [exGroup, setExGroup] = useState('')
  const [exEquip, setExEquip] = useState('')
  const [exCategory, setExCategory] = useState<ExerciseCategory | ''>('STRENGTH')
  const [exRequires, setExRequires] = useState(false)
  const [exImage, setExImage] = useState('')
  const [exInstructions, setExInstructions] = useState('')
  const [exMet, setExMet] = useState('')
  const [exDescription, setExDescription] = useState('')
  const [exFilter, setExFilter] = useState<ExerciseCategory | 'all'>('all')
  const [openEx, setOpenEx] = useState<number | null>(null)

  // routine editing
  const [routineMode, setRoutineMode] = useState<'list' | 'new' | Routine>('list')

  // strength log / planning form
  const [logDate, setLogDate] = useState(today())
  const [logRoutineId, setLogRoutineId] = useState('')
  const [logNotes, setLogNotes] = useState('')
  const [logStatus, setLogStatus] = useState<StrengthStatus>('DONE')

  async function refresh() {
    setError(null)
    try {
      const [e, r, s, w] = await Promise.all([
        listExercises(),
        listRoutines(),
        listStrengthSessions(),
        listWeight(),
      ])
      setExercises(e)
      setRoutines(r)
      setSessions(s)
      setWeightKg(w.length > 0 ? w[0].weightKg : null)
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
      toast.success('Hecho')
    } catch (err) {
      toast.error((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  async function addExercise(e: React.FormEvent) {
    e.preventDefault()
    if (exName.trim() === '') return
    const met = exMet.trim() === '' ? null : Number(exMet)
    await run(async () => {
      await createExercise({
        name: exName,
        muscleGroup: exGroup.trim() === '' ? null : exGroup.trim(),
        category: exCategory === '' ? null : exCategory,
        requiresEquipment: exRequires,
        equipment: exEquip.trim() === '' ? null : exEquip.trim(),
        description: exDescription.trim() === '' ? null : exDescription.trim(),
        imageUrl: exImage.trim() === '' ? null : exImage.trim(),
        instructions: exInstructions.trim() === '' ? null : exInstructions.trim(),
        metValue: met != null && !Number.isNaN(met) ? met : null,
      })
      setExName('')
      setExGroup('')
      setExEquip('')
      setExRequires(false)
      setExImage('')
      setExInstructions('')
      setExMet('')
      setExDescription('')
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
        status: logStatus,
      })
      setLogNotes('')
    })
  }

  function markDone(s: StrengthSession) {
    void run(() =>
      updateStrengthSession(s.id, {
        date: s.date,
        routineId: s.routineId,
        notes: s.notes,
        status: 'DONE',
      }),
    )
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
          Gimnasio por día
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
                Categoría
                <select value={exCategory} onChange={(e) => setExCategory(e.target.value as ExerciseCategory | '')}>
                  {EX_CATEGORIES.map((c) => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Grupo muscular
                <input value={exGroup} onChange={(e) => setExGroup(e.target.value)} />
              </label>
              <label>
                Material
                <input value={exEquip} onChange={(e) => setExEquip(e.target.value)} placeholder="p. ej. Mancuernas 7 kg" />
              </label>
              <label>
                MET (intensidad)
                <input type="number" step="0.1" min="0" value={exMet} onChange={(e) => setExMet(e.target.value)} placeholder="p. ej. 5" />
              </label>
              <label>
                Imagen/animación (URL)
                <input value={exImage} onChange={(e) => setExImage(e.target.value)} placeholder="https://… .gif" />
              </label>
              <label className="checkbox">
                <input type="checkbox" checked={exRequires} onChange={(e) => setExRequires(e.target.checked)} />
                Necesita material
              </label>
            </div>
            <label>
              Para qué y por qué
              <input value={exDescription} onChange={(e) => setExDescription(e.target.value)} placeholder="Objetivo del ejercicio" />
            </label>
            <label>
              Cómo se ejecuta
              <textarea value={exInstructions} onChange={(e) => setExInstructions(e.target.value)} rows={2} placeholder="Técnica paso a paso" />
            </label>
            <div className="actions">
              <button type="submit" disabled={busy || exName.trim() === ''}>
                Añadir
              </button>
              <button type="button" className="secondary" disabled={busy} onClick={() => run(seedExercises)}>
                Cargar ejemplos
              </button>
            </div>
          </form>

          <div className="section-head">
            <h3>Catálogo ({exercises.length})</h3>
            <select value={exFilter} onChange={(e) => setExFilter(e.target.value as ExerciseCategory | 'all')} style={{ width: 'auto' }}>
              <option value="all">Todas las categorías</option>
              {EX_CATEGORIES.map((c) => (
                <option key={c.value} value={c.value}>
                  {c.label}
                </option>
              ))}
            </select>
          </div>
          {exercises.length === 0 ? (
            <Empty icon="gym">Sin ejercicios todavía. Usa «Cargar ejemplos» para empezar.</Empty>
          ) : (
            <div className="ex-grid">
              {exercises
                .filter((ex) => exFilter === 'all' || ex.category === exFilter)
                .map((ex) => {
                  const open = openEx === ex.id
                  const kcalMin =
                    ex.metValue != null ? kcalPerMinute(ex.metValue, weightKg ?? 70) : null
                  return (
                    <article
                      key={ex.id}
                      className={`ex-card fade-up${open ? ' open' : ''}`}
                      role="button"
                      tabIndex={0}
                      aria-expanded={open}
                      onClick={() => setOpenEx(open ? null : ex.id)}
                      onKeyDown={(e) => {
                        if (e.target === e.currentTarget && (e.key === 'Enter' || e.key === ' ')) {
                          e.preventDefault()
                          setOpenEx(open ? null : ex.id)
                        }
                      }}
                    >
                      <div className="ex-thumb">
                        {ex.imageUrl ? (
                          <img src={ex.imageUrl} alt={ex.name} loading="lazy" />
                        ) : (
                          <span className="ex-thumb-empty">
                            <KoreIcon name="gym" size={30} />
                          </span>
                        )}
                        <span className={`cat-badge cat-${ex.category ?? 'none'}`}>
                          {categoryLabel(ex.category)}
                        </span>
                      </div>
                      <div className="ex-body">
                        <strong>{ex.name}</strong>
                        <span className="muted">{ex.muscleGroup ?? '—'}</span>
                        <div className="ex-meta">
                          <span>{ex.requiresEquipment ? (ex.equipment ?? 'Con material') : 'Sin material'}</span>
                          {ex.metValue != null && (
                            <span className="chip">
                              {ex.metValue} MET
                              {kcalMin != null && ` · ~${kcalMin.toFixed(1)} kcal/min`}
                            </span>
                          )}
                        </div>
                        {open && (
                          <div className="ex-detail">
                            {ex.description && <p>{ex.description}</p>}
                            {ex.instructions && (
                              <p className="muted">
                                <strong>Cómo:</strong> {ex.instructions}
                              </p>
                            )}
                            {kcalMin != null && (
                              <p className="muted">
                                Gasto estimado con {weightKg ?? 70} kg: ~{kcalMin.toFixed(1)} kcal/min
                                {weightKg == null && ' (peso por defecto 70 kg)'}
                              </p>
                            )}
                            <button
                              className="link danger"
                              onClick={(e) => {
                                e.stopPropagation()
                                run(() => deleteExercise(ex.id))
                              }}
                            >
                              Eliminar
                            </button>
                          </div>
                        )}
                      </div>
                    </article>
                  )
                })}
            </div>
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
            <SectionImport
              types={[
                { key: 'routine', label: 'Rutina' },
                { key: 'exercise', label: 'Ejercicio' },
              ]}
              onImported={refresh}
            />
            {routines.length === 0 ? (
              <Empty icon="gym">Sin rutinas todavía.</Empty>
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
            <h3>Gimnasio por día</h3>
            <p className="muted">
              Registra una sesión ya hecha o <strong>planifícala</strong> para un día concreto del
              calendario. Las planificadas aparecen en el Calendario y puedes marcarlas como hechas.
            </p>
            <div className="grid">
              <label>
                Fecha
                <input type="date" value={logDate} onChange={(e) => setLogDate(e.target.value)} required />
              </label>
              <label>
                Estado
                <select value={logStatus} onChange={(e) => setLogStatus(e.target.value as StrengthStatus)}>
                  <option value="DONE">Realizada</option>
                  <option value="PLANNED">Planificada</option>
                </select>
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
                {logStatus === 'PLANNED' ? 'Planificar' : 'Registrar'}
              </button>
            </div>
          </form>

          <h3>Sesiones ({sessions.length})</h3>
          {sessions.length === 0 ? (
            <Empty icon="gym">Sin sesiones todavía.</Empty>
          ) : (
            <table className="workouts">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Estado</th>
                  <th>Rutina</th>
                  <th>Notas</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {sessions.map((s) => (
                  <tr key={s.id}>
                    <td>{s.date}</td>
                    <td>
                      <span className={`chip status-${s.status.toLowerCase()}`}>
                        {s.status === 'PLANNED' ? 'Planificada' : 'Hecha'}
                      </span>
                    </td>
                    <td>{s.routineName ?? '(libre)'}</td>
                    <td>{s.notes ?? '—'}</td>
                    <td className="row-actions">
                      {s.status === 'PLANNED' && (
                        <button className="link" onClick={() => markDone(s)}>
                          Marcar hecha
                        </button>
                      )}
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
