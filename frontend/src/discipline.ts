import type { WorkoutType } from './api'

// Mapeo ÚNICO tipo de sesión → color de disciplina Kore. Centralizado aquí para
// que listas, gráficos y tarjetas usen el mismo código de color de forma
// transversal (una sesión de natación siempre turquesa, etc.).
//
// Preparado ya para las disciplinas de triatlón que llegarán al mapear bien la
// extracción de Suunto (Fase 10): natación (turquesa), ciclismo (azul),
// carrera (naranja). Hoy el backend solo emite RUNNING/STRENGTH/OTHER.
export type Discipline = WorkoutType | 'SWIMMING' | 'CYCLING'

const MAP: Record<string, { label: string; color: string }> = {
  RUNNING: { label: 'Carrera', color: 'var(--color-run)' },
  SWIMMING: { label: 'Natación', color: 'var(--color-swim)' },
  CYCLING: { label: 'Ciclismo', color: 'var(--color-bike)' },
  STRENGTH: { label: 'Fuerza', color: 'var(--color-core-raised)' },
  OTHER: { label: 'Otro', color: 'var(--color-text-muted)' },
}

export function disciplineColor(type: string): string {
  return MAP[type]?.color ?? 'var(--color-text-muted)'
}

export function disciplineLabel(type: string): string {
  return MAP[type]?.label ?? type
}
