import { useState } from 'react'
import { exportSession } from './api'
import type { View } from './nav'
import { navigateTo, useRoute } from './router'
import { clearUsername, getUsername } from './session'
import { getTheme, setTheme, type Theme } from './theme'
import KoreIcon, { type IconName } from './components/icons'
import { toast } from './toast'

// ROADMAP v4: el ciclo solo trabaja 4 secciones foco (Inicio/Calendario/Entrenos/
// Planes) + Ajustes. Gimnasio/Nutrición/Peso/Análisis quedan CONGELADOS: sus
// páginas y rutas siguen existiendo (accesibles por hash directo), pero se
// retiran del nav. Para reactivarlas, vuelve a añadir sus entradas aquí.
const TABS: { v: View; label: string; icon: IconName }[] = [
  { v: 'home', label: 'Inicio', icon: 'home' },
  { v: 'calendar', label: 'Calendario', icon: 'calendar' },
  { v: 'workouts', label: 'Entrenos', icon: 'workouts' },
  { v: 'plans', label: 'Planes', icon: 'plans' },
  { v: 'settings', label: 'Ajustes', icon: 'settings' },
]
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import CalendarView from './pages/Calendar'
import Home from './pages/Home'
import Settings from './pages/Settings'
import Analytics from './pages/Analytics'
import Plans from './pages/Plans'
import Gym from './pages/Gym'
import Nutrition from './pages/Nutrition'
import Weight from './pages/Weight'

export default function App() {
  const [username, setUsername] = useState<string | null>(getUsername())
  const route = useRoute()
  const view = route.view
  const [theme, setThemeState] = useState<Theme>(getTheme())

  function toggleTheme() {
    const next: Theme = theme === 'dark' ? 'light' : 'dark'
    setTheme(next)
    setThemeState(next)
  }

  if (!username) {
    return <Login onLogin={setUsername} />
  }

  async function doExport() {
    try {
      const doc = await exportSession()
      const blob = new Blob([JSON.stringify(doc, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `kore-${username}-${new Date().toISOString().slice(0, 10)}.json`
      a.click()
      URL.revokeObjectURL(url)
      toast.success('Sesión exportada')
    } catch (err) {
      toast.error((err as Error).message)
    }
  }

  return (
    <main className="app">
      <a href="#main-content" className="skip-link">
        Saltar al contenido
      </a>
      <header className="topbar">
        <div className="topbar-row">
          <img
            className="brand-logo"
            src={theme === 'dark' ? '/brand/kore-logotipo-oscuro.svg' : '/brand/kore-logotipo.svg'}
            alt="Kore"
          />
          <div className="actions">
            <span className="muted">{username}</span>
            <button
              className="secondary icon-btn"
              onClick={toggleTheme}
              title={theme === 'dark' ? 'Cambiar a claro' : 'Cambiar a oscuro'}
              aria-label="Cambiar tema"
            >
              <KoreIcon name={theme === 'dark' ? 'sun' : 'moon'} />
            </button>
            <button className="secondary icon-btn" onClick={doExport} title="Exportar sesión" aria-label="Exportar sesión">
              <KoreIcon name="download" />
            </button>
            <button
              className="secondary icon-btn"
              onClick={() => {
                clearUsername()
                setUsername(null)
              }}
              title="Salir"
              aria-label="Salir"
            >
              <KoreIcon name="logout" />
            </button>
          </div>
        </div>
        <nav className="nav" aria-label="Secciones">
          {TABS.map(({ v, label, icon }) => (
            <button
              key={v}
              className={view === v ? 'tab active' : 'tab'}
              aria-current={view === v ? 'page' : undefined}
              onClick={() => navigateTo(v)}
            >
              <KoreIcon name={icon} />
              <span className="tab-label">{label}</span>
            </button>
          ))}
        </nav>
      </header>

      <div id="main-content" tabIndex={-1}>
        {view === 'home' && <Dashboard onNavigate={navigateTo} />}
        {view === 'calendar' && (
          <CalendarView
            key={`${route.calendar?.date ?? ''}-${route.calendar?.view ?? ''}`}
            initialDate={route.calendar?.date}
            initialView={route.calendar?.view}
          />
        )}
        {view === 'workouts' && <Home />}
        {view === 'plans' && <Plans />}
        {view === 'gym' && <Gym />}
        {view === 'nutrition' && <Nutrition />}
        {view === 'weight' && <Weight />}
        {view === 'analytics' && <Analytics />}
        {view === 'settings' && <Settings />}
      </div>
    </main>
  )
}
