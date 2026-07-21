import { useEffect, useState } from 'react'
import { getAnalytics, type Dashboard, type PeriodSummary } from '../api'
import { formatDuration, formatPace, metersToKm } from '../format'

function BarChart({
  title,
  data,
  value,
  unit,
}: {
  title: string
  data: PeriodSummary[]
  value: (p: PeriodSummary) => number
  unit: string
}) {
  const max = Math.max(1, ...data.map(value))
  return (
    <div className="chart">
      <h4>{title}</h4>
      <div className="bars">
        {data.map((p) => {
          const v = value(p)
          return (
            <div key={p.label} className="bar-col" title={`${p.label}: ${v.toFixed(0)} ${unit}`}>
              <div className="bar" style={{ height: `${(v / max) * 100}%` }} />
              <span className="bar-label">{p.label.replace(/^\d+-/, '')}</span>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default function Analytics() {
  const [data, setData] = useState<Dashboard | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getAnalytics()
      .then(setData)
      .catch((err) => setError((err as Error).message))
  }, [])

  if (error) return <p className="error">{error}</p>
  if (!data) return <p className="muted">Cargando…</p>

  const t = data.totals

  function exportSummary() {
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `ccollector-resumen-${new Date().toISOString().slice(0, 10)}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <section>
      <div className="section-head">
        <h2>Análisis</h2>
        <button className="secondary" onClick={exportSummary} disabled={t.workouts === 0}>
          Exportar resumen
        </button>
      </div>

      {t.workouts === 0 ? (
        <p className="muted">Aún no hay datos. Registra o sincroniza entrenamientos.</p>
      ) : (
        <>
          <div className="cards">
            <div className="stat">
              <span className="stat-value">{t.workouts}</span>
              <span className="muted">entrenos</span>
            </div>
            <div className="stat">
              <span className="stat-value">{metersToKm(t.distanceMeters)}</span>
              <span className="muted">km totales</span>
            </div>
            <div className="stat">
              <span className="stat-value">{formatDuration(t.durationSeconds)}</span>
              <span className="muted">tiempo total</span>
            </div>
            <div className="stat">
              <span className="stat-value">{formatPace(t.avgPaceSecondsPerKm)}</span>
              <span className="muted">ritmo medio</span>
            </div>
            <div className="stat">
              <span className="stat-value">{t.load.toFixed(0)}</span>
              <span className="muted">carga total</span>
            </div>
          </div>

          <BarChart
            title="Volumen semanal (km)"
            data={data.weekly}
            value={(p) => p.distanceMeters / 1000}
            unit="km"
          />
          <BarChart title="Carga semanal (sRPE)" data={data.weekly} value={(p) => p.load} unit="" />

          <h3>Resumen mensual</h3>
          <table className="workouts">
            <thead>
              <tr>
                <th>Mes</th>
                <th>Entrenos</th>
                <th>Distancia</th>
                <th>Tiempo</th>
                <th>Ritmo</th>
                <th>Carga</th>
              </tr>
            </thead>
            <tbody>
              {data.monthly.map((m) => (
                <tr key={m.label}>
                  <td>{m.label}</td>
                  <td>{m.workouts}</td>
                  <td>{m.distanceMeters > 0 ? `${metersToKm(m.distanceMeters)} km` : '—'}</td>
                  <td>{formatDuration(m.durationSeconds)}</td>
                  <td>{formatPace(m.avgPaceSecondsPerKm)}</td>
                  <td>{m.load.toFixed(0)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </section>
  )
}
