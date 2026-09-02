import { useRef, useState } from 'react'
import { importKdlAs, type KdlImportResult } from '../api'

export interface ImportType {
  key: string
  label: string
}

/**
 * Import por sección (Fase 21): pega o sube un JSON — un recurso suelto, un array
 * o un documento KDL — y se importa como el tipo elegido en esta sección. Ideal
 * para traer lo que te genere una IA (p. ej. un plan de 8 semanas).
 */
export default function SectionImport({
  types,
  onImported,
}: {
  types: ImportType[]
  onImported: () => void
}) {
  const [type, setType] = useState(types[0].key)
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<KdlImportResult | null>(null)
  const fileRef = useRef<HTMLInputElement>(null)

  async function run(raw: string) {
    setBusy(true)
    setError(null)
    setResult(null)
    try {
      const parsed = JSON.parse(raw)
      const r = await importKdlAs(type, parsed)
      setResult(r)
      onImported()
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  async function onFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    await run(await file.text())
    if (fileRef.current) fileRef.current.value = ''
  }

  return (
    <details className="import-panel">
      <summary>Importar {types.length === 1 ? types[0].label.toLowerCase() : 'en esta sección'}</summary>
      <div className="card">
        {types.length > 1 && (
          <label>
            Tipo
            <select value={type} onChange={(e) => setType(e.target.value)}>
              {types.map((t) => (
                <option key={t.key} value={t.key}>
                  {t.label}
                </option>
              ))}
            </select>
          </label>
        )}
        <p className="muted">
          Pega un JSON (un recurso, un array o un documento KDL) o sube un archivo. Puedes pedirle a
          una IA que lo genere en el formato de Kore (ver docs/AGENTS.md).
        </p>
        <textarea
          rows={5}
          value={text}
          placeholder={'{ "name": "…", … }'}
          onChange={(e) => setText(e.target.value)}
        />
        <div className="actions">
          <button type="button" disabled={busy || text.trim() === ''} onClick={() => run(text)}>
            Importar pegado
          </button>
          <button type="button" className="secondary" disabled={busy} onClick={() => fileRef.current?.click()}>
            Subir archivo
          </button>
          <input ref={fileRef} type="file" accept=".json,.kdl,application/json" hidden onChange={onFile} />
        </div>

        {error && <p className="error">{error}</p>}
        {result && (
          <div className="card" style={{ marginTop: '0.6rem' }}>
            <p className="ok">
              Importados {result.imported} · omitidos {result.skipped}
            </p>
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
      </div>
    </details>
  )
}
