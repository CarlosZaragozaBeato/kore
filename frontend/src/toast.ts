// Avisos efímeros (toasts), sin dependencias. Un store minúsculo con
// suscripción: cualquier módulo llama a `toast.success('…')` y el <Toaster>
// montado en App los pinta. Feedback uniforme de acciones en toda la app.

export type ToastType = 'success' | 'error' | 'info'

export interface Toast {
  id: number
  type: ToastType
  message: string
}

type Listener = (toasts: Toast[]) => void

let toasts: Toast[] = []
let seq = 0
const listeners = new Set<Listener>()

function emit() {
  for (const l of listeners) l(toasts)
}

export function dismiss(id: number): void {
  toasts = toasts.filter((t) => t.id !== id)
  emit()
}

function push(type: ToastType, message: string, ttlMs: number) {
  const id = ++seq
  toasts = [...toasts, { id, type, message }]
  emit()
  if (ttlMs > 0) {
    setTimeout(() => dismiss(id), ttlMs)
  }
}

export function subscribe(listener: Listener): () => void {
  listeners.add(listener)
  listener(toasts)
  return () => {
    listeners.delete(listener)
  }
}

export const toast = {
  success: (message: string) => push('success', message, 3500),
  error: (message: string) => push('error', message, 6000),
  info: (message: string) => push('info', message, 3500),
}
