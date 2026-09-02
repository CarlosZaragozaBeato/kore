import { useMemo, useState } from 'react'
import type { Dashboard, Workout, WorkoutType } from '../api'
import { formatPace, metersToKm } from '../format'
import { disciplineLabel } from '../discipline'

const TYPES: WorkoutType[] = ['RUNNING', 'CYCLING', 'SWIMMING', 'STRENGTH', 'OTHER']

// Métricas comparables entre entrenos afines. `get` extrae el número; `fmt` lo
// presenta. La carga de la semana se calcula aparte (necesita analytics).
interface Metric {
  key: string
  label: string
  get: (w: Workout) => number | null
  fmt: (v: number) => string
}

const METRICS: Metric[] = [
  { key: 'pace', label: 'Ritmo', get: (w) => w.paceSecondsPerKm, fmt: (v) => formatPace(v) },
  { key: 'hr', label: 'FC media', get: (w) => w.avgHeartRate, fmt: (v) => `${Math.round(v)} ppm` },
  { key: 'cadence', label: 'Cadencia', get: (w) => w.avgCadenceSpm, fmt: (v) => `${Math.round(v)} spm` },
  { key: 'stride', label: 'Zancada', get: (w) => w.strideLengthMeters, fmt: (v) => `${v.toFixed(2)} m` },
  { key: 'distance', label: 'Distancia', get: (w) => w.distanceMeters, fmt: (v) => `${metersToKm(v)} km` },
]

function weekLoadFor(date: string, analytics: Dashboard | null): number | null {
  const p = analytics?.weekly.find((w) => w.from <= date && date <= w.to)
  return p ? p.load : null
}

function num(value: string): number | null {
  const n = Number(value)
  return value.trim() === '' || Number.isNaN(n) ? null : n
}

export default function WorkoutAnalysis({
  workouts,
  analytics,
}: {
  workouts: Workout[]
  analytics: Dashboard | null
}) {
  const [type, setType] = useState<WorkoutType>('RUNNING')
  const [metricKey, setMetricKey] = useState('cadence')
  const [distMin, setDistMin] = useState('')
  const [distMax, setDistMax] = useState('')
  const [paceMin, setPaceMin] = useState('')
  const [paceMax, setPaceMax] = useState('')

  const metric = METRICS.find((m) => m.key === metricKey) ?? METRICS[0]

  // Entrenos del tipo elegido, acotados opcionalmente por distancia y ritmo para
  // aislar sesiones afines (p. ej. recuperación 12 km a ~5:30). Orden cronológico.
  const filtered = useMemo(() => {
    const dMin = num(distMin)
    const dMax = num(distMax)
    const pMin = num(paceMin)
    const pMax = num(paceMax)
    return workouts
      .filter((w) => w.type === type)
      .filter((w) => {
        const km = w.distanceMeters == null ? null : w.distanceMeters / 1000
        if (dMin != null && (km == null || km < dMin)) return false
        if (dMax != null && (km == null || km > dMax)) return false
        const paceMinKm = w.paceSecondsPerKm == null ? null : w.paceSecondsPerKm / 60
        if (pMin != null && (paceMinKm == null || paceMinKm < pMin)) return false
        if (pMax != null && (paceMinKm == null || paceMinKm > pMax)) return false
        return true
      })
      .sort((a, b) => a.date.localeCompare(b.date))
  }, [workouts, type, distMin, distMax, paceMin, paceMax])

  const values = filtered.map((w) => metric.get(w))
  const present = values.filter((v): v is number => v != null)
  const max = present.length ? Math.max(...present) : 0
  const min = present.length ? Math.min(...present) : 0
  const avg = present.length ? present.reduce((a, b) => a + b, 0) / present.length : null

  return (
    <div className="fade-up">
      <div className="filters">
        <label>
          Disciplina
          <select value={type} onChange={(e) => setType(e.target.value as WorkoutType)}>
            {TYPES.map((t) => (
              <option key={t} value={t}>
                {disciplineLabel(t)}
              </option>
            ))}
          </select>
        </label>
        <label>
          Distancia km (min–máx)
          <span className="duration">
            <input type="number" step="0.1" min="0" placeholder="min" value={distMin} onChange={(e) => setDistMin(e.target.value)} />
            <input type="number" step="0.1" min="0" placeholder="máx" value={distMax} onChange={(e) => setDistMax(e.target.value)} />
          </span>
        </label>
        <label>
          Ritmo min/km (min–máx)
          <span className="duration">
            <input type="number" step="0.1" min="0" placeholder="min" value={paceMin} onChange={(e) => setPaceMin(e.target.value)} />
            <input type="number" step="0.1" min="0" placeholder="máx" value={paceMax} onChange={(e) => setPaceMax(e.target.value)} />
          </span>
        </label>
      </div>

      {filtered.length === 0 ? (
        <p className="muted">No hay entrenos de {disciplineLabel(type).toLowerCase()} con esos criterios.</p>
      ) : (
        <>
          <div className="nav" style={{ margin: '0.6rem 0' }}>
            {METRICS.map((m) => (
              <button
                key={m.key}
                className={m.key === metricKey ? 'tab active' : 'tab'}
                onClick={() => setMetricKey(m.key)}
              >
                {m.label}
              </button>
            ))}
          </div>

          {avg != null && (
            <p className="muted">
              {metric.label}: media {metric.fmt(avg)} · rango {metric.fmt(min)}–{metric.fmt(max)} ·{' '}
              {present.length} de {filtered.length} entrenos con dato.
            </p>
          )}

          {/* Evolución de la métrica elegida a lo largo de las sesiones afines */}
          <div className="chart">
            <h4>{metric.label} por sesión</h4>
            <div className="bars">
              {filtered.map((w, i) => {
                const v = values[i]
                const h = v != null && max > 0 ? Math.max(4, (v / max) * 100) : 0
                return (
                  <div className="bar-col" key={w.id} title={`${w.date}${v != null ? ` · ${metric.fmt(v)}` : ''}`}>
                    <div className="bar grow" style={{ height: `${h}%` }} />
                    <span className="bar-label">{w.date.slice(5)}</span>
                  </div>
                )
              })}
            </div>
          </div>

          <table className="workouts">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Km</th>
                <th>Ritmo</th>
                <th>FC</th>
                <th>Cadencia</th>
                <th>Zancada</th>
                <th>Carga sem.</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((w) => {
                const load = weekLoadFor(w.date, analytics)
                return (
                  <tr key={w.id}>
                    <td>{w.date}</td>
                    <td>{w.distanceMeters != null ? metersToKm(w.distanceMeters) : '—'}</td>
                    <td>{formatPace(w.paceSecondsPerKm)}</td>
                    <td>{w.avgHeartRate ?? '—'}</td>
                    <td>{w.avgCadenceSpm != null ? `${w.avgCadenceSpm}` : '—'}</td>
                    <td>{w.strideLengthMeters != null ? w.strideLengthMeters.toFixed(2) : '—'}</td>
                    <td>{load != null ? Math.round(load) : '—'}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </>
      )}
    </div>
  )
}
