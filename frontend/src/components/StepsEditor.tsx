import type { StepKind } from '../api'
import { STEP_KINDS, emptyStep, type StepDraft } from '../stepDraft'

interface Props {
  steps: StepDraft[]
  onChange: (steps: StepDraft[]) => void
}

/** Editor de pasos estructurados reutilizable (plan y calendario). */
export default function StepsEditor({ steps, onChange }: Props) {
  function update(j: number, patch: Partial<StepDraft>) {
    onChange(steps.map((s, sj) => (sj === j ? { ...s, ...patch } : s)))
  }
  function remove(j: number) {
    onChange(steps.filter((_, sj) => sj !== j))
  }

  return (
    <div className="steps">
      {steps.length === 0 && <p className="muted">Sin pasos. Divide la sesión en calentamiento, series, etc.</p>}
      {steps.map((s, j) => (
        <div className="step-row" key={j}>
          <select value={s.kind} onChange={(e) => update(j, { kind: e.target.value as StepKind })}>
            {STEP_KINDS.map((k) => (
              <option key={k.value} value={k.value}>
                {k.label}
              </option>
            ))}
          </select>
          <input className="w-xs" type="number" min="1" placeholder="×n" title="Repeticiones" value={s.repeat} onChange={(e) => update(j, { repeat: e.target.value })} />
          <input className="w-sm" type="number" min="0" placeholder="m" title="Distancia (metros)" value={s.dist} onChange={(e) => update(j, { dist: e.target.value })} />
          <input className="w-sm" type="number" min="0" placeholder="s" title="Duración (segundos)" value={s.dur} onChange={(e) => update(j, { dur: e.target.value })} />
          <input className="w-sm" placeholder="rit. min" title="Ritmo más rápido mm:ss/km" value={s.paceMin} onChange={(e) => update(j, { paceMin: e.target.value })} />
          <input className="w-sm" placeholder="rit. máx" title="Ritmo más lento mm:ss/km" value={s.paceMax} onChange={(e) => update(j, { paceMax: e.target.value })} />
          <input className="w-xs" type="number" min="0" placeholder="fc-" title="FC mínima" value={s.hrMin} onChange={(e) => update(j, { hrMin: e.target.value })} />
          <input className="w-xs" type="number" min="0" placeholder="fc+" title="FC máxima" value={s.hrMax} onChange={(e) => update(j, { hrMax: e.target.value })} />
          <input className="w-sm" type="number" min="0" placeholder="rec s" title="Descanso (segundos)" value={s.rec} onChange={(e) => update(j, { rec: e.target.value })} />
          <input placeholder="nota" value={s.note} onChange={(e) => update(j, { note: e.target.value })} />
          <button type="button" className="link danger" onClick={() => remove(j)}>
            ✕
          </button>
        </div>
      ))}
      <button type="button" className="link" onClick={() => onChange([...steps, emptyStep()])}>
        + Añadir paso
      </button>
    </div>
  )
}
