// Presentación de pasos estructurados de una sesión planificada (Fase B).
import type { PlannedStep, StepKind } from './api'

export const STEP_KIND_LABEL: Record<StepKind, string> = {
  WARMUP: 'Calentamiento',
  INTERVAL: 'Serie',
  RECOVERY: 'Recuperación',
  STEADY: 'Rodaje',
  COOLDOWN: 'Vuelta a la calma',
}

function pace(secPerKm: number | null): string {
  if (secPerKm == null) return ''
  const m = Math.floor(secPerKm / 60)
  return `${m}:${String(secPerKm % 60).padStart(2, '0')}`
}

function dist(m: number | null): string {
  if (m == null) return ''
  return m >= 1000 ? `${(m / 1000).toFixed(m % 1000 === 0 ? 0 : 1)} km` : `${m} m`
}

function dur(s: number | null): string {
  if (s == null) return ''
  const m = Math.floor(s / 60)
  const sec = s % 60
  return m > 0 ? `${m}'${sec > 0 ? String(sec).padStart(2, '0') : ''}` : `${sec}''`
}

/** Resumen compacto de un paso: p. ej. "Serie ×5 · 200 m /1' @3:40–3:50/km". */
export function formatStep(s: PlannedStep): string {
  const parts: string[] = [STEP_KIND_LABEL[s.kind]]
  if (s.repeat > 1) parts[0] += ` ×${s.repeat}`

  const measure = dist(s.targetDistanceMeters) || dur(s.targetDurationSeconds)
  if (measure) parts.push(measure)

  if (s.recoverySeconds != null) parts.push(`/${dur(s.recoverySeconds)}`)

  const pMin = pace(s.targetPaceMinSecPerKm)
  const pMax = pace(s.targetPaceMaxSecPerKm)
  if (pMin && pMax) parts.push(`@${pMin}–${pMax}/km`)
  else if (pMin || pMax) parts.push(`@${pMin || pMax}/km`)

  if (s.targetHrMin != null || s.targetHrMax != null) {
    parts.push(`${s.targetHrMin ?? ''}–${s.targetHrMax ?? ''} ppm`)
  }
  if (s.note) parts.push(s.note)
  return parts.join(' · ')
}
