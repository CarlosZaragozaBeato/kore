// Navegación entre secciones sin router: el estado de vista vive en App.tsx y se
// pasa un `Navigate` hacia abajo para que un widget lleve a su sección (Fase 19).

export type View =
  | 'home'
  | 'calendar'
  | 'workouts'
  | 'plans'
  | 'gym'
  | 'nutrition'
  | 'weight'
  | 'analytics'
  | 'settings'

/** Foco inicial del Calendario al navegar hacia él (día y vista). */
export interface CalendarFocus {
  date?: string
  view?: 'month' | 'week'
}

export type Navigate = (view: View, calendar?: CalendarFocus) => void
