import { useEffect, useState } from 'react'
import { apiGet } from './api'

interface Ping {
  service: string
  status: string
  timestamp: string
}

type Health = 'checking' | 'up' | 'down'

export default function App() {
  const [health, setHealth] = useState<Health>('checking')
  const [ping, setPing] = useState<Ping | null>(null)

  useEffect(() => {
    apiGet<Ping>('/ping')
      .then((res) => {
        if (res.success && res.data) {
          setPing(res.data)
          setHealth('up')
        } else {
          setHealth('down')
        }
      })
      .catch(() => setHealth('down'))
  }, [])

  return (
    <main style={{ fontFamily: 'system-ui, sans-serif', padding: '2rem', maxWidth: 640 }}>
      <h1>CCollector</h1>
      <p>Plataforma personal de entrenamiento. Fase 0 — fundaciones.</p>
      <p>
        Backend:{' '}
        <strong style={{ color: health === 'up' ? 'green' : health === 'down' ? 'crimson' : 'gray' }}>
          {health === 'checking' ? 'comprobando…' : health === 'up' ? 'conectado' : 'sin conexión'}
        </strong>
      </p>
      {ping && (
        <pre style={{ background: '#f4f4f4', padding: '1rem', borderRadius: 8 }}>
          {JSON.stringify(ping, null, 2)}
        </pre>
      )}
    </main>
  )
}
