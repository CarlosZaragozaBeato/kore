import { useRef, useState } from 'react'
import { exportKdl, importKdl, type KdlImportResult } from '../api'

export default function KdlPanel() {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<KdlImportResult | null>(null)
  const fileRef = useRef<HTMLInputElement>(null)

  async function doExport() {
    setBusy(true)
    setError(null)
    setResult(null)
    try {
      const doc = await exportKdl()
      const blob = new Blob([JSON.stringify(doc, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `kore-recursos-${new Date().toISOString().slice(0, 10)}.kdl.json`
      a.click()
      URL.revokeObjectURL(url)
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  async function onFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    setBusy(true)
    setError(null)
    setResult(null)
    try {
      const doc = JSON.parse(await file.text())
      setResult(await importKdl(doc))
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
      if (fileRef.current) fileRef.current.value = ''
    }
  }

  return (
    <>
      <h2 style={{ marginTop: '2rem' }}>Recursos (KDL)</h2>
      <p className="muted">
        Exporta o importa recursos sueltos (entrenos, ingredientes, ejercicios, recetas, planes,
        rutinas, dietas, peso) en un archivo JSON portable entre versiones y dispositivos. Cada
        sección (Planes, Nutrición, Gimnasio) tiene además su propio importador para traer, p. ej.,
        un plan completo generado por una IA.
      </p>
      <div className="actions">
        <button className="secondary" onClick={doExport} disabled={busy}>
          Exportar recursos
        </button>
        <button className="secondary" onClick={() => fileRef.current?.click()} disabled={busy}>
          Importar recursos
        </button>
        <input ref={fileRef} type="file" accept=".json,.kdl,application/json" hidden onChange={onFile} />
      </div>

      {error && <p className="error">{error}</p>}
      {result && (
        <div className="card" style={{ marginTop: '0.8rem' }}>
          <p className="ok">
            Importados {result.imported} · omitidos {result.skipped}
          </p>
          {Object.keys(result.byType).length > 0 && (
            <p className="muted">
              {Object.entries(result.byType)
                .map(([k, v]) => `${k}: ${v}`)
                .join(' · ')}
            </p>
          )}
          {result.errors.length > 0 && (
            <ul className="timeline">
              {result.errors.map((msg, i) => (
                <li key={i} className="muted">
                  {msg}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </>
  )
}
