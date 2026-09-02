// Borrador editable de un paso estructurado: los campos numéricos se manejan
// como texto y se parsean al enviar. Compartido por el editor de plan y el
// planificador de calendario.
import type { PlannedStep, PlanStepRequest, StepKind } from './api'

export interface StepDraft {
  kind: StepKind
  repeat: string
  dist: string // metros
  dur: string // segundos
  paceMin: string // mm:ss/km (más rápido)
  paceMax: string // mm:ss/km (más lento)
  hrMin: string
  hrMax: string
  rec: string // descanso en segundos
  note: string
}

export const STEP_KINDS: { value: StepKind; label: string }[] = [
  { value: 'WARMUP', label: 'Calentamiento' },
  { value: 'INTERVAL', label: 'Serie' },
  { value: 'RECOVERY', label: 'Recuperación' },
  { value: 'STEADY', label: 'Rodaje' },
  { value: 'COOLDOWN', label: 'Vuelta a la calma' },
]

export function secToPace(s: number | null): string {
  if (s == null) return ''
  const m = Math.floor(s / 60)
  return `${m}:${String(s % 60).padStart(2, '0')}`
}

export function paceToSec(v: string): number | null {
  const t = v.trim()
  if (t === '') return null
  const m = t.match(/^(\d+):(\d{1,2})$/)
  if (m) return Number(m[1]) * 60 + Number(m[2])
  const n = Number(t)
  return Number.isNaN(n) ? null : Math.round(n)
}

function num(value: string): number | null {
  const n = Number(value)
  return value.trim() === '' || Number.isNaN(n) ? null : n
}

export function emptyStep(): StepDraft {
  return { kind: 'INTERVAL', repeat: '1', dist: '', dur: '', paceMin: '', paceMax: '', hrMin: '', hrMax: '', rec: '', note: '' }
}

export function toStepDraft(s: PlannedStep): StepDraft {
  return {
    kind: s.kind,
    repeat: String(s.repeat ?? 1),
    dist: s.targetDistanceMeters != null ? String(s.targetDistanceMeters) : '',
    dur: s.targetDurationSeconds != null ? String(s.targetDurationSeconds) : '',
    paceMin: secToPace(s.targetPaceMinSecPerKm),
    paceMax: secToPace(s.targetPaceMaxSecPerKm),
    hrMin: s.targetHrMin != null ? String(s.targetHrMin) : '',
    hrMax: s.targetHrMax != null ? String(s.targetHrMax) : '',
    rec: s.recoverySeconds != null ? String(s.recoverySeconds) : '',
    note: s.note ?? '',
  }
}

export function toStepRequest(d: StepDraft): PlanStepRequest {
  return {
    kind: d.kind,
    repeat: num(d.repeat) == null ? 1 : Math.max(1, Math.round(num(d.repeat) as number)),
    targetDistanceMeters: num(d.dist),
    targetDurationSeconds: num(d.dur) == null ? null : Math.round(num(d.dur) as number),
    targetPaceMinSecPerKm: paceToSec(d.paceMin),
    targetPaceMaxSecPerKm: paceToSec(d.paceMax),
    targetHrMin: num(d.hrMin) == null ? null : Math.round(num(d.hrMin) as number),
    targetHrMax: num(d.hrMax) == null ? null : Math.round(num(d.hrMax) as number),
    recoverySeconds: num(d.rec) == null ? null : Math.round(num(d.rec) as number),
    note: d.note.trim() === '' ? null : d.note.trim(),
  }
}
