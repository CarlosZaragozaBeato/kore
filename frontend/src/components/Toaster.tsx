import { useEffect, useState } from 'react'
import { dismiss, subscribe, type Toast } from '../toast'

export default function Toaster() {
  const [items, setItems] = useState<Toast[]>([])

  useEffect(() => subscribe(setItems), [])

  if (items.length === 0) return null

  return (
    <div className="toaster" role="status" aria-live="polite">
      {items.map((t) => (
        <button
          key={t.id}
          type="button"
          className={`toast toast-${t.type}`}
          onClick={() => dismiss(t.id)}
          title="Descartar"
        >
          {t.message}
        </button>
      ))}
    </div>
  )
}
