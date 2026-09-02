import { useEffect, useState } from 'react'
import { toast } from '../toast'
import { Empty } from './state'
import {
  createSessionTemplate,
  deleteSessionTemplate,
  listSessionTemplates,
  seedSessionTemplates,
  type Level,
  type RaceGoal,
  type SessionTemplate,
  type WorkoutType,
} from '../api'
import { disciplineColor, disciplineLabel } from '../discipline'
import { metersToKm, formatDuration } from '../format'

const DISCIPLINES: WorkoutType[] = ['RUNNING', 'CYCLING', 'SWIMMING', 'STRENGTH', 'OTHER']
const GOALS: { value: RaceGoal; label: string }[] = [
  { value: 'MARATHON', label: 'Maratón' },
  { value: 'HALF_MARATHON', label: 'Media maratón' },
  { value: 'TEN_K', label: '10K' },
  { value: 'FIVE_K', label: '5K' },
  { value: 'GENERAL', label: 'General' },
]
const LEVELS: { value: Level; label: string }[] = [
  { value: 'BEGINNER', label: 'Principiante' },
  { value: 'INTERMEDIATE', label: 'Intermedio' },
  { value: 'ADVANCED', label: 'Avanzado' },
]

const goalLabel = (g: RaceGoal | null) => GOALS.find((x) => x.value === g)?.label ?? '—'
const levelLabel = (l: Level | null) => LEVELS.find((x) => x.value === l)?.label ?? '—'

function num(value: string): number | null {
  const n = Number(value)
  return value.trim() === '' || Number.isNaN(n) ? null : n
}

const EMPTY = {
  name: '',
  discipline: 'RUNNING' as WorkoutType,
  goal: 'GENERAL' as RaceGoal,
  level: 'INTERMEDIATE' as Level,
  km: '',
  min: '',
  structure: '',
  notes: '',
}

export default function SessionTemplates() {
  const [items, setItems] = useState<SessionTemplate[]>([])
  const [form, setForm] = useState({ ...EMPTY })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // filtros
  const [fDisc, setFDisc] = useState<WorkoutType | 'all'>('all')
  const [fGoal, setFGoal] = useState<RaceGoal | 'all'>('all')
  const [fLevel, setFLevel] = useState<Level | 'all'>('all')

  async function refresh() {
    setError(null)
    try {
      setItems(await listSessionTemplates())
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

  async function add(e: React.FormEvent) {
    e.preventDefault()
    if (form.name.trim() === '') return
    const min = num(form.min)
    const km = num(form.km)
    await run(async () => {
      await createSessionTemplate({
        name: form.name.trim(),
        discipline: form.discipline,
        goal: form.goal,
        level: form.level,
        targetDistanceMeters: km == null ? null : Math.round(km * 1000),
        targetDurationSeconds: min == null ? null : min * 60,
        structure: form.structure.trim() === '' ? null : form.structure.trim(),
        notes: form.notes.trim() === '' ? null : form.notes.trim(),
      })
      setForm({ ...EMPTY })
    })
  }

  const filtered = items.filter(
    (t) =>
      (fDisc === 'all' || t.discipline === fDisc) &&
      (fGoal === 'all' || t.goal === fGoal) &&
      (fLevel === 'all' || t.level === fLevel),
  )

  return (
    <>
      {error && <p className="error">{error}</p>}

      <form className="card form" onSubmit={add}>
        <h3>Nueva plantilla de sesión</h3>
        <div className="grid">
          <label>
            Nombre
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </label>
          <label>
            Disciplina
            <select value={form.discipline} onChange={(e) => setForm({ ...form, discipline: e.target.value as WorkoutType })}>
              {DISCIPLINES.map((d) => (
                <option key={d} value={d}>
                  {disciplineLabel(d)}
                </option>
              ))}
            </select>
          </label>
          <label>
            Objetivo
            <select value={form.goal} onChange={(e) => setForm({ ...form, goal: e.target.value as RaceGoal })}>
              {GOALS.map((g) => (
                <option key={g.value} value={g.value}>
                  {g.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Nivel
            <select value={form.level} onChange={(e) => setForm({ ...form, level: e.target.value as Level })}>
              {LEVELS.map((l) => (
                <option key={l.value} value={l.value}>
                  {l.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Distancia (km)
            <input type="number" step="0.1" min="0" value={form.km} onChange={(e) => setForm({ ...form, km: e.target.value })} />
          </label>
          <label>
            Duración (min)
            <input type="number" min="0" value={form.min} onChange={(e) => setForm({ ...form, min: e.target.value })} />
          </label>
        </div>
        <label>
          Estructura
          <textarea rows={2} value={form.structure} onChange={(e) => setForm({ ...form, structure: e.target.value })} />
        </label>
        <div className="actions">
          <button type="submit" disabled={busy || form.name.trim() === ''}>
            Añadir
          </button>
          <button type="button" className="secondary" disabled={busy} onClick={() => run(seedSessionTemplates)}>
            Cargar ejemplos
          </button>
        </div>
      </form>

      <div className="form card">
        <div className="grid">
          <label>
            Disciplina
            <select value={fDisc} onChange={(e) => setFDisc(e.target.value as WorkoutType | 'all')}>
              <option value="all">Todas</option>
              {DISCIPLINES.map((d) => (
                <option key={d} value={d}>
                  {disciplineLabel(d)}
                </option>
              ))}
            </select>
          </label>
          <label>
            Objetivo
            <select value={fGoal} onChange={(e) => setFGoal(e.target.value as RaceGoal | 'all')}>
              <option value="all">Todos</option>
              {GOALS.map((g) => (
                <option key={g.value} value={g.value}>
                  {g.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Nivel
            <select value={fLevel} onChange={(e) => setFLevel(e.target.value as Level | 'all')}>
              <option value="all">Todos</option>
              {LEVELS.map((l) => (
                <option key={l.value} value={l.value}>
                  {l.label}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      <h3>Plantillas ({filtered.length})</h3>
      {filtered.length === 0 ? (
        <Empty icon="plans">Sin plantillas. Usa «Cargar ejemplos» para empezar.</Empty>
      ) : (
        filtered.map((t) => (
          <div className="card plan" key={t.id}>
            <div className="plan-head">
              <div>
                <span className="disc-badge">
                  <span className="dot" style={{ backgroundColor: disciplineColor(t.discipline) }} />
                  <strong>{t.name}</strong>
                </span>
                <div className="muted">
                  {disciplineLabel(t.discipline)} · {goalLabel(t.goal)} · {levelLabel(t.level)}
                  {t.targetDistanceMeters != null ? ` · ${metersToKm(t.targetDistanceMeters)} km` : ''}
                  {t.targetDurationSeconds != null ? ` · ${formatDuration(t.targetDurationSeconds)}` : ''}
                </div>
              </div>
              <button className="link danger" onClick={() => run(() => deleteSessionTemplate(t.id))}>
                Eliminar
              </button>
            </div>
            {t.structure && <p style={{ whiteSpace: 'pre-line', margin: '0.4rem 0 0' }}>{t.structure}</p>}
            {t.notes && <p className="muted" style={{ margin: '0.3rem 0 0' }}>{t.notes}</p>}
          </div>
        ))
      )}
    </>
  )
}
