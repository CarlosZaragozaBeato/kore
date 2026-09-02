// Enrutado ligero por hash (#/gym, #/calendar/2026-07-23/week). Sin
// dependencias y sin necesidad de reescrituras en el servidor: funciona
// servido como estático, offline y desde la PWA. Da URLs compartibles y
// habilita atrás/adelante del navegador.
import { useEffect, useState } from 'react'
import type { CalendarFocus, Navigate, View } from './nav'

const VIEWS: View[] = [
  'home',
  'calendar',
  'workouts',
  'plans',
  'gym',
  'nutrition',
  'weight',
  'analytics',
  'settings',
]

export interface Route {
  view: View
  calendar?: CalendarFocus
}

const ISO = /^\d{4}-\d{2}-\d{2}$/

export function parseHash(): Route {
  const parts = window.location.hash.replace(/^#\/?/, '').split('/').filter(Boolean)
  const view = (VIEWS as string[]).includes(parts[0]) ? (parts[0] as View) : 'home'
  if (view === 'calendar' && parts.length > 1) {
    const date = ISO.test(parts[1]) ? parts[1] : undefined
    const cview = parts[2] === 'week' || parts[2] === 'month' ? parts[2] : undefined
    if (date || cview) {
      return { view, calendar: { date, view: cview } }
    }
  }
  return { view }
}

export function toHash(view: View, calendar?: CalendarFocus): string {
  if (view === 'calendar' && calendar?.date) {
    const cview = calendar.view ? `/${calendar.view}` : ''
    return `#/calendar/${calendar.date}${cview}`
  }
  return `#/${view}`
}

/** Cambia de ruta escribiendo el hash (dispara `hashchange`). */
export const navigateTo: Navigate = (view, calendar) => {
  const next = toHash(view, calendar)
  if (window.location.hash !== next) {
    window.location.hash = next
  }
}

/** Ruta actual, reactiva a atrás/adelante y a `navigateTo`. */
export function useRoute(): Route {
  const [route, setRoute] = useState<Route>(parseHash)
  useEffect(() => {
    const onChange = () => setRoute(parseHash())
    window.addEventListener('hashchange', onChange)
    return () => window.removeEventListener('hashchange', onChange)
  }, [])
  return route
}
