import { useState } from 'react'
import { getTrainingContext, type TrainingContext } from '../api'
import { toast } from '../toast'
import { metersToKm } from '../format'

/** Fecha ISO de hoy y de hace N días (rango por defecto: últimas 4 semanas). */
function isoDaysAgo(days: number): string {
  const d = new Date()
  d.setDate(d.getDate() - days)
  return d.toISOString().slice(0, 10)
}

export default function TrainingContextExport() {
  const [from, setFrom] = useState(() => isoDaysAgo(27))
  const [to, setTo] = useState(() => isoDaysAgo(0))
  const [busy, setBusy] = useState(false)
  const [ctx, setCtx] = useState<TrainingContext | null>(null)

  async function prepare(): Promise<TrainingContext | null> {
    setBusy(true)
    try {
      const c = await getTrainingContext(from, to)
      setCtx(c)
      return c
    } catch (err) {
      toast.error((err as Error).message)
      return null
    } finally {
      setBusy(false)
    }
  }

  async function download() {
    const c = ctx ?? (await prepare())
    if (!c) return
    const blob = new Blob([JSON.stringify(c, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `kore-contexto-${from}_${to}.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  async function copy() {
    const c = ctx ?? (await prepare())
    if (!c) return
    try {
      await navigator.clipboard.writeText(JSON.stringify(c, null, 2))
      toast.success('Contexto copiado al portapapeles')
    } catch {
      toast.error('No se pudo copiar; usa «Descargar JSON»')
    }
  }

  return (
    <details className="import-panel">
      <summary>Descargar contexto para un agente</summary>
      <div className="card">
        <p className="muted">
          Genera un JSON autodescriptivo con lo entrenado y planificado en el rango elegido (más
          señales de carga y bloques de periodización). Pásaselo a un agente y pídele las sesiones de
          la semana; te las devolverá en el formato listo para importar aquí abajo.
        </p>
        <div className="session-row">
          <label>
            Desde
            <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
          </label>
          <label>
            Hasta
            <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
          </label>
        </div>
        <div className="actions">
          <button type="button" disabled={busy} onClick={download}>
            Descargar JSON
          </button>
          <button type="button" className="secondary" disabled={busy} onClick={copy}>
            Copiar
          </button>
          <button type="button" className="secondary" disabled={busy} onClick={() => void prepare()}>
            Previsualizar
          </button>
        </div>

        {ctx && (
          <div className="card" style={{ marginTop: '0.6rem' }}>
            <p className="ok">
              {ctx.summary.workoutCount} entreno{ctx.summary.workoutCount === 1 ? '' : 's'} ·{' '}
              {metersToKm(ctx.summary.totalDistanceMeters)} km · {ctx.summary.plannedCount} planificada
              {ctx.summary.plannedCount === 1 ? '' : 's'} · {ctx.blocks.length} bloque
              {ctx.blocks.length === 1 ? '' : 's'}
            </p>
            <p className="muted">
              Carga (ACWR): {ctx.summary.load.acwr != null ? ctx.summary.load.acwr.toFixed(2) : '—'} ·{' '}
              {ctx.summary.load.acwrState}
            </p>
          </div>
        )}
      </div>
    </details>
  )
}
