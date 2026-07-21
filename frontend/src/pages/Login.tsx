import { useState } from 'react'
import { importSession, login } from '../api'
import { setUsername } from '../session'

interface Props {
  onLogin: (username: string) => void
}

export default function Login({ onLogin }: Props) {
  const [value, setValue] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    const username = value.trim()
    if (!username) return
    setBusy(true)
    setError(null)
    try {
      const user = await login(username)
      setUsername(user.username)
      onLogin(user.username)
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  async function importFile(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    setBusy(true)
    setError(null)
    setInfo(null)
    try {
      const doc = JSON.parse(await file.text())
      const user = await importSession(doc)
      setInfo(`Sesión "${user.username}" importada. Entra con ese nombre.`)
      setValue(user.username)
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="center">
      <div className="card">
        <h1>CCollector</h1>
        <p className="muted">Crea o recupera tu sesión con solo un nombre.</p>
        <form onSubmit={submit}>
          <label>
            Nombre de usuario
            <input
              autoFocus
              value={value}
              onChange={(e) => setValue(e.target.value)}
              placeholder="p. ej. carlos"
            />
          </label>
          <button type="submit" disabled={busy || value.trim() === ''}>
            Entrar
          </button>
        </form>

        <hr />
        <label className="muted">
          ¿Vienes de otro dispositivo? Importa tu sesión:
          <input type="file" accept="application/json" onChange={importFile} disabled={busy} />
        </label>

        {info && <p className="ok">{info}</p>}
        {error && <p className="error">{error}</p>}
      </div>
    </main>
  )
}
