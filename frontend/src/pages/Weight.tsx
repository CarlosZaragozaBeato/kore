import { useEffect, useState } from 'react'
import { toast } from '../toast'
import { Empty } from '../components/state'
import {
  deleteDailyActivity,
  deleteWeight,
  getEnergy,
  getWeightGoal,
  listDailyActivity,
  listWeight,
  setWeightGoal,
  upsertDailyActivity,
  upsertWeight,
  type DailyActivity,
  type EnergySummary,
  type WeightEntry,
  type WeightGoal,
} from '../api'
import { StateBadge } from '../energyState'

function today(): string {
  return new Date().toISOString().slice(0, 10)
}

function num(value: string): number | null {
  const n = Number(value)
  return value.trim() === '' || Number.isNaN(n) ? null : n
}

export default function Weight() {
  const [entries, setEntries] = useState<WeightEntry[]>([])
  const [goal, setGoal] = useState<WeightGoal>({ minKg: null, maxKg: null, maintenanceKcal: null })
  const [energy, setEnergy] = useState<EnergySummary | null>(null)
  const [activity, setActivity] = useState<DailyActivity[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // weight form
  const [date, setDate] = useState(today())
  const [kg, setKg] = useState('')

  // goal form
  const [minKg, setMinKg] = useState('')
  const [maxKg, setMaxKg] = useState('')
  const [maint, setMaint] = useState('')

  // daily activity form
  const [actDate, setActDate] = useState(today())
  const [steps, setSteps] = useState('')
  const [burn, setBurn] = useState('')

  async function refresh() {
    setError(null)
    try {
      const [e, g, en, act] = await Promise.all([
        listWeight(),
        getWeightGoal(),
        getEnergy(),
        listDailyActivity(),
      ])
      setEntries(e)
      setGoal(g)
      setEnergy(en)
      setActivity(act)
      setMinKg(g.minKg != null ? String(g.minKg) : '')
      setMaxKg(g.maxKg != null ? String(g.maxKg) : '')
      setMaint(g.maintenanceKcal != null ? String(g.maintenanceKcal) : '')
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

  async function addWeight(e: React.FormEvent) {
    e.preventDefault()
    const w = num(kg)
    if (w == null || w <= 0) return
    await run(async () => {
      await upsertWeight({ date, weightKg: w })
      setKg('')
    })
  }

  async function saveGoal(e: React.FormEvent) {
    e.preventDefault()
    await run(() =>
      setWeightGoal({ minKg: num(minKg), maxKg: num(maxKg), maintenanceKcal: num(maint) }),
    )
  }

  async function addActivity(e: React.FormEvent) {
    e.preventDefault()
    const s = num(steps)
    const b = num(burn)
    if (s == null && b == null) return
    await run(async () => {
      await upsertDailyActivity({ date: actDate, steps: s, burnedKcal: b })
      setSteps('')
      setBurn('')
    })
  }

  const latest = entries[0]
  const previous = entries[1]
  const delta = latest && previous ? latest.weightKg - previous.weightKg : null
  const inRange =
    latest && goal.minKg != null && goal.maxKg != null
      ? latest.weightKg >= goal.minKg && latest.weightKg <= goal.maxKg
      : null

  return (
    <section>
      {error && <p className="error">{error}</p>}

      <div className="cards">
        <div className="stat">
          <span className="muted">Peso actual</span>
          <span className="stat-value">{latest ? `${latest.weightKg} kg` : '—'}</span>
          {delta != null && (
            <span className={delta === 0 ? 'muted' : delta > 0 ? 'error' : 'ok'}>
              {delta > 0 ? '▲' : delta < 0 ? '▼' : '='} {Math.abs(delta).toFixed(1)} kg
            </span>
          )}
        </div>
        <div className="stat">
          <span className="muted">Objetivo (rango)</span>
          <span className="stat-value">
            {goal.minKg != null && goal.maxKg != null ? `${goal.minKg}–${goal.maxKg}` : '—'}
          </span>
          {inRange != null && (
            <span className={inRange ? 'ok' : 'error'}>{inRange ? 'En rango' : 'Fuera de rango'}</span>
          )}
        </div>
        <div className="stat">
          <span className="muted">Mantenimiento</span>
          <span className="stat-value">
            {goal.maintenanceKcal != null ? `${goal.maintenanceKcal}` : '—'}
          </span>
          <span className="muted">kcal/día</span>
        </div>
      </div>

      <div className="form card">
        <h3>Registrar peso</h3>
        <form onSubmit={addWeight}>
          <div className="grid">
            <label>
              Fecha
              <input type="date" value={date} max={today()} onChange={(e) => setDate(e.target.value)} required />
            </label>
            <label>
              Peso (kg)
              <input type="number" step="0.1" min="0" value={kg} onChange={(e) => setKg(e.target.value)} required />
            </label>
          </div>
          <div className="actions">
            <button type="submit" disabled={busy}>
              Guardar
            </button>
          </div>
        </form>
      </div>

      <div className="form card">
        <h3>Objetivo de mantenimiento</h3>
        <p className="muted">Un rango de peso para no subir ni bajar, y tu gasto diario de referencia.</p>
        <form onSubmit={saveGoal}>
          <div className="grid">
            <label>
              Peso mín. (kg)
              <input type="number" step="0.1" min="0" value={minKg} onChange={(e) => setMinKg(e.target.value)} />
            </label>
            <label>
              Peso máx. (kg)
              <input type="number" step="0.1" min="0" value={maxKg} onChange={(e) => setMaxKg(e.target.value)} />
            </label>
            <label>
              Mantenimiento (kcal/día)
              <input type="number" step="1" min="0" value={maint} onChange={(e) => setMaint(e.target.value)} />
            </label>
          </div>
          <div className="actions">
            <button type="submit" className="secondary" disabled={busy}>
              Guardar objetivo
            </button>
          </div>
        </form>
      </div>

      <div className="form card">
        <h3>Actividad diaria</h3>
        <p className="muted">
          Pasos y quema total del día (de Suunto o a mano). La quema diaria sustituye a la estimación
          por entrenos en el balance, para no contar dos veces.
        </p>
        <form onSubmit={addActivity}>
          <div className="grid">
            <label>
              Fecha
              <input type="date" value={actDate} max={today()} onChange={(e) => setActDate(e.target.value)} required />
            </label>
            <label>
              Pasos
              <input type="number" step="1" min="0" value={steps} onChange={(e) => setSteps(e.target.value)} />
            </label>
            <label>
              Quema del día (kcal)
              <input type="number" step="1" min="0" value={burn} onChange={(e) => setBurn(e.target.value)} />
            </label>
          </div>
          <div className="actions">
            <button type="submit" disabled={busy}>
              Guardar día
            </button>
          </div>
        </form>

        {activity.length > 0 && (
          <table className="workouts" style={{ marginTop: '0.8rem' }}>
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Pasos</th>
                <th>Quema</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {activity.map((a) => (
                <tr key={a.id}>
                  <td>{a.date}</td>
                  <td>{a.steps != null ? a.steps.toLocaleString('es-ES') : '—'}</td>
                  <td>{a.burnedKcal != null ? `${Math.round(a.burnedKcal)} kcal` : '—'}</td>
                  <td className="row-actions">
                    <button className="link danger" onClick={() => run(() => deleteDailyActivity(a.id))}>
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <h3>Balance energético</h3>
      {energy && energy.maintenanceKcal == null && (
        <p className="muted">
          Fija tu kcal/día de mantenimiento arriba para evaluar déficit/equilibrio/superávit.
        </p>
      )}

      <div className="chart">
        <h4>Últimos 14 días (consumidas − quemadas)</h4>
        {energy && energy.days.some((d) => d.consumedKcal > 0 || d.burnedKcal > 0) ? (
          <table className="workouts">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Consumidas</th>
                <th>Quemadas</th>
                <th>Balance</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {energy.days
                .filter((d) => d.consumedKcal > 0 || d.burnedKcal > 0)
                .map((d) => (
                  <tr key={d.date}>
                    <td>{d.date}</td>
                    <td>{d.consumedKcal}</td>
                    <td>{d.burnedKcal}</td>
                    <td>{d.balanceKcal}</td>
                    <td>
                      <StateBadge state={d.state} />
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        ) : (
          <p className="muted">Sin datos de kcal todavía (añade dietas con recetas y entrenos).</p>
        )}
      </div>

      <div className="chart">
        <h4>Por semana (últimas 8)</h4>
        <table className="workouts">
          <thead>
            <tr>
              <th>Semana</th>
              <th>Consumidas</th>
              <th>Quemadas</th>
              <th>Balance</th>
              <th>Estado</th>
            </tr>
          </thead>
          <tbody>
            {energy?.weeks.map((w) => (
              <tr key={w.label}>
                <td>{w.label}</td>
                <td>{w.consumedKcal}</td>
                <td>{w.burnedKcal}</td>
                <td>{w.balanceKcal}</td>
                <td>
                  <StateBadge state={w.state} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h3>Histórico de peso ({entries.length})</h3>
      {entries.length === 0 ? (
        <Empty icon="weight">Sin mediciones todavía.</Empty>
      ) : (
        <table className="workouts">
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Peso</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {entries.map((e) => (
              <tr key={e.id}>
                <td>{e.date}</td>
                <td>{e.weightKg} kg</td>
                <td className="row-actions">
                  <button className="link danger" onClick={() => run(() => deleteWeight(e.id))}>
                    Eliminar
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
