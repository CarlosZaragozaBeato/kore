import { useEffect, useState } from 'react'
import { getCompare, type LoadComparison as Comparison, type SignalState } from '../api'
import { metersToKm, formatDuration } from '../format'
import { disciplineColor, disciplineLabel } from '../discipline'
import { Empty, Loading } from './state'

const SIGNAL_META: Record<SignalState, { label: string; color: string }> = {
  OK: { label: 'OK', color: 'var(--color-success)' },
  WARN: { label: 'Vigilar', color: 'var(--color-warning)' },
  RISK: { label: 'Riesgo', color: 'var(--color-danger)' },
  UNKNOWN: { label: '—', color: 'var(--color-text-muted)' },
}

function SignalCard({ title, value, state, hint }: { title: string; value: string; state: SignalState; hint: string }) {
  const m = SIGNAL_META[state]
  return (
    <div className="stat">
      <span className="muted">{title}</span>
      <span className="stat-value">{value}</span>
      <span className="disc-badge">
        <span className="dot" style={{ backgroundColor: m.color }} />
        {m.label}
      </span>
      <span className="muted" style={{ fontSize: '0.75rem' }}>{hint}</span>
    </div>
  )
}

function delta(cur: number, prev: number, unit: string): { text: string; cls: string } {
  const d = cur - prev
  const cls = d === 0 ? 'muted' : d > 0 ? 'ok' : 'error'
  const sign = d > 0 ? '▲' : d < 0 ? '▼' : '='
  return { text: `${sign} ${Math.abs(d).toFixed(unit === 'km' ? 2 : 0)} ${unit}`, cls }
}

export default function LoadComparison() {
  const [data, setData] = useState<Comparison | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [cStart, setCStart] = useState('')
  const [cEnd, setCEnd] = useState('')
  const [pStart, setPStart] = useState('')
  const [pEnd, setPEnd] = useState('')

  function load(ranges: Record<string, string> = {}) {
    setLoading(true)
    setError(null)
    getCompare(ranges)
      .then((d) => {
        setData(d)
        setCStart(d.current.from)
        setCEnd(d.current.to)
        setPStart(d.previous.from)
        setPEnd(d.previous.to)
      })
      .catch((err) => setError((err as Error).message))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load({})
  }, [])

  function apply() {
    load({ currentStart: cStart, currentEnd: cEnd, previousStart: pStart, previousEnd: pEnd })
  }

  function exportJson() {
    if (!data) return
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `kore-comparativa-${new Date().toISOString().slice(0, 10)}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <>
      <div className="card form">
        <h3>Rangos a comparar</h3>
        <div className="grid">
          <label>
            Actual · desde
            <input type="date" value={cStart} onChange={(e) => setCStart(e.target.value)} />
          </label>
          <label>
            Actual · hasta
            <input type="date" value={cEnd} onChange={(e) => setCEnd(e.target.value)} />
          </label>
          <label>
            Previo · desde
            <input type="date" value={pStart} onChange={(e) => setPStart(e.target.value)} />
          </label>
          <label>
            Previo · hasta
            <input type="date" value={pEnd} onChange={(e) => setPEnd(e.target.value)} />
          </label>
        </div>
        <div className="actions">
          <button onClick={apply} disabled={loading}>
            Comparar
          </button>
          <button className="secondary" onClick={() => load({})} disabled={loading}>
            Semana actual vs previa
          </button>
          <button className="secondary" onClick={exportJson} disabled={!data}>
            Exportar JSON
          </button>
        </div>
      </div>

      {error && <p className="error">{error}</p>}
      {loading && <Loading />}

      {data && !loading && (
        <>
          <h3>Señales de carga</h3>
          <div className="cards">
            <SignalCard
              title="ACWR (agudo/crónico)"
              value={data.signals.acwr != null ? data.signals.acwr.toFixed(2) : '—'}
              state={data.signals.acwrState}
              hint="zona 0.8–1.3"
            />
            <SignalCard
              title="Monotonía"
              value={data.signals.monotony != null ? data.signals.monotony.toFixed(2) : '—'}
              state={data.signals.monotonyState}
              hint="alta > 2"
            />
            <SignalCard
              title="Ramp semanal"
              value={data.signals.rampPct != null ? `${data.signals.rampPct.toFixed(0)}%` : '—'}
              state={data.signals.rampState}
              hint="subida brusca > 50%"
            />
          </div>

          <h3>Totales: actual vs previo</h3>
          <table className="workouts">
            <thead>
              <tr>
                <th>Métrica</th>
                <th>Actual</th>
                <th>Previo</th>
                <th>Diferencia</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Distancia</td>
                <td>{metersToKm(data.current.distanceMeters)} km</td>
                <td>{metersToKm(data.previous.distanceMeters)} km</td>
                <td className={delta(data.current.distanceMeters / 1000, data.previous.distanceMeters / 1000, 'km').cls}>
                  {delta(data.current.distanceMeters / 1000, data.previous.distanceMeters / 1000, 'km').text}
                </td>
              </tr>
              <tr>
                <td>Tiempo</td>
                <td>{formatDuration(data.current.durationSeconds)}</td>
                <td>{formatDuration(data.previous.durationSeconds)}</td>
                <td className={delta(data.current.durationSeconds / 60, data.previous.durationSeconds / 60, 'min').cls}>
                  {delta(data.current.durationSeconds / 60, data.previous.durationSeconds / 60, 'min').text}
                </td>
              </tr>
              <tr>
                <td>Carga (sRPE)</td>
                <td>{data.current.load}</td>
                <td>{data.previous.load}</td>
                <td className={delta(data.current.load, data.previous.load, '').cls}>
                  {delta(data.current.load, data.previous.load, '').text}
                </td>
              </tr>
              <tr>
                <td>Entrenos</td>
                <td>{data.current.workouts}</td>
                <td>{data.previous.workouts}</td>
                <td className={delta(data.current.workouts, data.previous.workouts, '').cls}>
                  {delta(data.current.workouts, data.previous.workouts, '').text}
                </td>
              </tr>
            </tbody>
          </table>

          <h3>Carga por día (y acumulada)</h3>
          <table className="workouts">
            <thead>
              <tr>
                <th>Día</th>
                <th>Actual</th>
                <th>Previo</th>
                <th>Diferencia</th>
                <th>Acumulada</th>
              </tr>
            </thead>
            <tbody>
              {data.dailyDeltas.map((d) => (
                <tr key={d.dayIndex}>
                  <td>
                    {d.currentDate}
                    <span className="muted"> / {d.previousDate}</span>
                  </td>
                  <td>{d.currentLoad}</td>
                  <td>{d.previousLoad}</td>
                  <td className={d.deltaLoad === 0 ? 'muted' : d.deltaLoad > 0 ? 'ok' : 'error'}>
                    {d.deltaLoad > 0 ? '+' : ''}
                    {d.deltaLoad}
                  </td>
                  <td className={d.cumulativeDeltaLoad === 0 ? 'muted' : d.cumulativeDeltaLoad > 0 ? 'ok' : 'error'}>
                    {d.cumulativeDeltaLoad > 0 ? '+' : ''}
                    {d.cumulativeDeltaLoad}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <h3>Desglose del rango actual</h3>
          <div className="chart">
            <h4>Por disciplina</h4>
            {data.current.byDiscipline.length === 0 ? (
              <Empty icon="workouts">Sin entrenos en el rango.</Empty>
            ) : (
              <ul className="timeline">
                {data.current.byDiscipline.map((b) => (
                  <li key={b.key}>
                    <span className="dot" style={{ backgroundColor: disciplineColor(b.key), display: 'inline-block' }} />
                    <span>{disciplineLabel(b.key)}</span>
                    <span className="muted">
                      {b.workouts} · {metersToKm(b.distanceMeters)} km · carga {b.load}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>
          <div className="chart">
            <h4>Por origen</h4>
            <ul className="timeline">
              {data.current.bySource.map((b) => (
                <li key={b.key}>
                  <span>{b.key === 'SUUNTO' ? 'Suunto' : 'Manual'}</span>
                  <span className="muted">
                    {b.workouts} · {metersToKm(b.distanceMeters)} km · carga {b.load}
                  </span>
                </li>
              ))}
            </ul>
          </div>
        </>
      )}
    </>
  )
}
