import { useEffect, useState } from 'react'
import {
  getComparison,
  type AdherenceBand,
  type Comparison,
  type ComparisonMetric,
  type ComparisonMetricKey,
} from '../api'
import { formatDuration, formatPace, metersToKm } from '../format'

const METRIC_LABEL: Record<ComparisonMetricKey, string> = {
  distance: 'Distancia',
  duration: 'Duración',
  pace: 'Ritmo',
  hr: 'FC media',
}

/** Formatea un valor de métrica según su unidad. */
function fmt(key: ComparisonMetricKey, value: number | null): string {
  if (value == null) return '—'
  switch (key) {
    case 'distance':
      return `${metersToKm(value)} km`
    case 'duration':
      return formatDuration(value)
    case 'pace':
      return formatPace(value)
    case 'hr':
      return `${Math.round(value)} ppm`
  }
}

/** Objetivo: banda [low, high] o valor único cuando coinciden. */
function fmtTarget(m: ComparisonMetric): string {
  if (m.targetLow === m.targetHigh) return fmt(m.key, m.target)
  if (m.key === 'distance' || m.key === 'duration') return fmt(m.key, m.target)
  return `${fmt(m.key, m.targetLow)}–${fmt(m.key, m.targetHigh)}`
}

/** Etiqueta y color de la banda, con el matiz propio de cada métrica. */
function bandInfo(key: ComparisonMetricKey, band: AdherenceBand): { label: string; cls: string } {
  if (band === 'WITHIN') return { label: 'En objetivo', cls: 'band-ok' }
  if (band === 'NO_DATA') return { label: 'Sin dato', cls: 'band-na' }
  if (key === 'pace') {
    return band === 'BELOW'
      ? { label: 'Más rápido', cls: 'band-below' }
      : { label: 'Más lento', cls: 'band-above' }
  }
  if (key === 'hr') {
    return band === 'BELOW'
      ? { label: 'Más suave', cls: 'band-below' }
      : { label: 'Más fuerte', cls: 'band-above' }
  }
  return band === 'BELOW'
    ? { label: 'Por debajo', cls: 'band-below' }
    : { label: 'Por encima', cls: 'band-above' }
}

export default function SessionComparison({ sessionId }: { sessionId: number }) {
  const [data, setData] = useState<Comparison | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let alive = true
    getComparison(sessionId)
      .then((c) => alive && setData(c))
      .catch((err) => alive && setError((err as Error).message))
    return () => {
      alive = false
    }
  }, [sessionId])

  if (error) return <p className="error">{error}</p>
  if (!data) return <p className="muted">Comparando…</p>

  if (!data.matched) {
    return <p className="muted comparison-empty">Aún no hay entreno registrado este día para comparar.</p>
  }

  return (
    <div className="comparison">
      <table className="comparison-table">
        <thead>
          <tr>
            <th>Métrica</th>
            <th>Objetivo</th>
            <th>Realizado</th>
            <th>Δ</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {data.metrics.map((m) => {
            const info = bandInfo(m.key, m.band)
            return (
              <tr key={m.key}>
                <td>{METRIC_LABEL[m.key]}</td>
                <td className="muted">{fmtTarget(m)}</td>
                <td>{fmt(m.key, m.actual)}</td>
                <td className="muted">{m.deltaPct == null ? '—' : `${m.deltaPct > 0 ? '+' : ''}${m.deltaPct}%`}</td>
                <td>
                  <span className={`band-pill ${info.cls}`}>{info.label}</span>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
      {data.target.totalReps > 0 && (
        <p className="muted comparison-note">
          {data.target.totalReps} repeticiones planificadas · las series no se contrastan una a una
          (el entreno guardado es un resumen, sin parciales por vuelta).
        </p>
      )}
    </div>
  )
}
