import { useEffect, useState } from 'react'
import { getCorrelations, type Correlation, type CorrelationReport } from '../api'
import { Loading } from './state'

function strengthClass(c: Correlation): string {
  if (c.r == null) return 'corr-none'
  const a = Math.abs(c.r)
  if (a < 0.2) return 'corr-none'
  if (a < 0.4) return 'corr-weak'
  if (a < 0.6) return 'corr-moderate'
  return 'corr-strong'
}

function arrow(c: Correlation): string {
  if (c.r == null) return '·'
  return c.direction === 'positiva' ? '↑' : c.direction === 'negativa' ? '↓' : '·'
}

export default function Correlations() {
  const [data, setData] = useState<CorrelationReport | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getCorrelations()
      .then(setData)
      .catch((err) => setError((err as Error).message))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Loading label="Cruzando secciones…" />
  if (error) return <p className="error">{error}</p>
  if (!data) return null

  return (
    <>
      <p className="muted">
        Relación semana a semana entre entrenamiento, nutrición, actividad y peso. El coeficiente
        (r) va de −1 a 1: cerca de ±1 = relación fuerte; cerca de 0 = ninguna. No implica causa.
      </p>

      <div className="corr-grid">
        {data.correlations.map((c, i) => (
          <div key={i} className={`card corr-card fade-up ${strengthClass(c)}`}>
            <div className="corr-head">
              <strong>
                {c.aLabel} <span className="muted">×</span> {c.bLabel}
              </strong>
              <span className="corr-r">
                {arrow(c)} {c.r != null ? c.r.toFixed(2) : '—'}
              </span>
            </div>
            <div className="muted corr-meta">
              {c.r != null ? `${c.strength} · ${c.direction}` : 'sin datos'} · {c.n} semana
              {c.n === 1 ? '' : 's'}
            </div>
            <p className="corr-text">{c.interpretation}</p>
          </div>
        ))}
      </div>

      <p className="muted" style={{ marginTop: '1rem', fontSize: '0.85rem' }}>
        {data.note}
      </p>
    </>
  )
}
