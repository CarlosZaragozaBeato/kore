import { useState } from 'react'
import { exportSession } from './api'
import { clearUsername, getUsername } from './session'
import Login from './pages/Login'
import Home from './pages/Home'
import Settings from './pages/Settings'
import Analytics from './pages/Analytics'

type View = 'workouts' | 'analytics' | 'settings'

export default function App() {
  const [username, setUsername] = useState<string | null>(getUsername())
  const [view, setView] = useState<View>('workouts')
  const [error, setError] = useState<string | null>(null)

  if (!username) {
    return <Login onLogin={setUsername} />
  }

  async function doExport() {
    setError(null)
    try {
      const doc = await exportSession()
      const blob = new Blob([JSON.stringify(doc, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `ccollector-${username}-${new Date().toISOString().slice(0, 10)}.json`
      a.click()
      URL.revokeObjectURL(url)
    } catch (err) {
      setError((err as Error).message)
    }
  }

  return (
    <main className="app">
      <header className="topbar">
        <div className="nav">
          <strong>CCollector</strong>
          <button className={view === 'workouts' ? 'tab active' : 'tab'} onClick={() => setView('workouts')}>
            Entrenos
          </button>
          <button className={view === 'analytics' ? 'tab active' : 'tab'} onClick={() => setView('analytics')}>
            Análisis
          </button>
          <button className={view === 'settings' ? 'tab active' : 'tab'} onClick={() => setView('settings')}>
            Ajustes
          </button>
        </div>
        <div className="actions">
          <span className="muted">{username}</span>
          <button className="secondary" onClick={doExport}>
            Exportar sesión
          </button>
          <button
            className="secondary"
            onClick={() => {
              clearUsername()
              setUsername(null)
            }}
          >
            Salir
          </button>
        </div>
      </header>

      {error && <p className="error">{error}</p>}

      {view === 'workouts' && <Home />}
      {view === 'analytics' && <Analytics />}
      {view === 'settings' && <Settings />}
    </main>
  )
}
