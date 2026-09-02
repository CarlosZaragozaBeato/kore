import { useEffect, useState } from 'react'
import {
  getSuuntoSettings,
  purgeSuunto,
  saveSuuntoSettings,
  syncSuunto,
  type SuuntoSettings,
} from '../api'
import KdlPanel from '../components/KdlPanel'
import { toast } from '../toast'

export default function Settings() {
  const [settings, setSettings] = useState<SuuntoSettings | null>(null)
  const [enabled, setEnabled] = useState(false)
  const [clientId, setClientId] = useState('')
  const [clientSecret, setClientSecret] = useState('')
  const [refreshToken, setRefreshToken] = useState('')
  const [subscriptionKey, setSubscriptionKey] = useState('')
  const [busy, setBusy] = useState(false)
  const [info, setInfo] = useState<string | null>(null)

  async function load() {
    try {
      const s = await getSuuntoSettings()
      setSettings(s)
      setEnabled(s.enabled)
      setClientId(s.clientId ?? '')
    } catch (err) {
      toast.error((err as Error).message)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  async function save(e: React.FormEvent) {
    e.preventDefault()
    setBusy(true)
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
      toast.success('Ajustes guardados')
    } catch (err) {
      toast.error((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  async function sync() {
    setBusy(true)
    setInfo(null)
    try {
      const r = await syncSuunto()
      setInfo(
        `Sincronizado: ${r.imported} nuevos, ${r.updated} actualizados, ${r.skipped} omitidos (${r.total} en Suunto).`,
      )
      toast.success('Suunto sincronizado')
      await load()
    } catch (err) {
      toast.error((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  async function purge() {
    if (!confirm('¿Borrar los entrenos de Suunto anteriores a la ventana de retención? Son recuperables con un re-sync.')) {
      return
    }
    setBusy(true)
    setInfo(null)
    try {
      const r = await purgeSuunto()
      setInfo(`Retención aplicada: ${r.purged} entrenos de Suunto borrados (se conserva desde ${r.cutoff}).`)
      toast.success('Retención aplicada')
      await load()
    } catch (err) {
      toast.error((err as Error).message)
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
          <button type="button" className="link danger" onClick={purge} disabled={busy}>
            Purgar antiguos
          </button>
        </div>
      </form>

      <p className="muted">
        Última sincronización:{' '}
        {settings?.lastSyncAt ? new Date(settings.lastSyncAt).toLocaleString() : 'nunca'}
      </p>
      <p className="muted">
        Retención mínima: los entrenos de Suunto son recuperables desde su nube, así que solo se
        conservan el mes actual y el anterior. «Purgar antiguos» libera el resto (un re-sync los
        recupera).
      </p>

      {info && <p className="ok">{info}</p>}

      <KdlPanel />

      <h2 style={{ marginTop: '2rem' }}>Integración con agentes</h2>
      <p className="muted">
        Tus datos son accesibles por API para que un agente los lea y escriba.
        Envía el header <code>X-CCollector-Username</code> en cada petición.
      </p>
      <ul className="timeline">
        <li>
          <code>GET /api/v1/agent/manifest</code>
          <span className="muted">mapa de recursos (sin sesión)</span>
        </li>
        <li>
          <code>GET /api/v1/agent/context</code>
          <span className="muted">toda tu sesión + analítica en una llamada</span>
        </li>
        <li>
          <code>GET /q/openapi</code>
          <span className="muted">contrato OpenAPI · UI en /q/swagger-ui</span>
        </li>
      </ul>
      <p className="muted">Detalles y flujos en docs/AGENTS.md.</p>
    </section>
  )
}
