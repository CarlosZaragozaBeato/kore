import { useEffect, useState } from 'react'
import {
  getSuuntoSettings,
  saveSuuntoSettings,
  syncSuunto,
  type SuuntoSettings,
} from '../api'

export default function Settings() {
  const [settings, setSettings] = useState<SuuntoSettings | null>(null)
  const [enabled, setEnabled] = useState(false)
  const [clientId, setClientId] = useState('')
  const [clientSecret, setClientSecret] = useState('')
  const [refreshToken, setRefreshToken] = useState('')
  const [subscriptionKey, setSubscriptionKey] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)

  async function load() {
    try {
      const s = await getSuuntoSettings()
      setSettings(s)
      setEnabled(s.enabled)
      setClientId(s.clientId ?? '')
    } catch (err) {
      setError((err as Error).message)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  async function save(e: React.FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setInfo(null)
    try {
      const s = await saveSuuntoSettings({
        enabled,
        clientId,
        // vacío = conservar el guardado
        clientSecret: clientSecret === '' ? null : clientSecret,
        refreshToken: refreshToken === '' ? null : refreshToken,
        subscriptionKey: subscriptionKey === '' ? null : subscriptionKey,
      })
      setSettings(s)
      setClientSecret('')
      setRefreshToken('')
      setSubscriptionKey('')
      setInfo('Configuración guardada.')
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  async function sync() {
    setBusy(true)
    setError(null)
    setInfo(null)
    try {
      const r = await syncSuunto()
      setInfo(`Sincronizado: ${r.imported} importados, ${r.skipped} ya existentes (${r.total} en Suunto).`)
      await load()
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  const placeholder = (saved: boolean) => (saved ? '•••••••• (guardado)' : '')

  return (
    <section>
      <h2>Integración con Suunto</h2>
      <p className="muted">
        Introduce las credenciales de tu app de Suunto. Se guardan cifradas en este
        dispositivo y nunca se muestran de vuelta.
      </p>

      <form className="card form" onSubmit={save}>
        <label className="checkbox">
          <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
          Habilitar la integración
        </label>

        <label>
          Client ID
          <input value={clientId} onChange={(e) => setClientId(e.target.value)} />
        </label>
        <label>
          Client Secret
          <input
            type="password"
            value={clientSecret}
            placeholder={placeholder(settings?.hasClientSecret ?? false)}
            onChange={(e) => setClientSecret(e.target.value)}
          />
        </label>
        <label>
          Refresh Token
          <input
            type="password"
            value={refreshToken}
            placeholder={placeholder(settings?.hasRefreshToken ?? false)}
            onChange={(e) => setRefreshToken(e.target.value)}
          />
        </label>
        <label>
          Subscription Key
          <input
            type="password"
            value={subscriptionKey}
            placeholder={placeholder(settings?.hasSubscriptionKey ?? false)}
            onChange={(e) => setSubscriptionKey(e.target.value)}
          />
        </label>

        <div className="actions">
          <button type="submit" disabled={busy}>
            Guardar
          </button>
          <button type="button" className="secondary" onClick={sync} disabled={busy || !settings?.enabled}>
            Sincronizar ahora
          </button>
        </div>
      </form>

      <p className="muted">
        Última sincronización:{' '}
        {settings?.lastSyncAt ? new Date(settings.lastSyncAt).toLocaleString() : 'nunca'}
      </p>

      {info && <p className="ok">{info}</p>}
      {error && <p className="error">{error}</p>}
    </section>
  )
}
