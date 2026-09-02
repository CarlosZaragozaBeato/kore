import type { EnergyState } from './api'

// Metadatos de estado del balance energético, con los colores de estado Kore.
// Para mantenimiento, BALANCED (equilibrio) es lo deseado → verde.
export const ENERGY_STATE_META: Record<EnergyState, { label: string; color: string }> = {
  BALANCED: { label: 'Equilibrio', color: 'var(--color-success)' },
  DEFICIT: { label: 'Déficit', color: 'var(--color-warning)' },
  SURPLUS: { label: 'Superávit', color: 'var(--color-danger)' },
  UNKNOWN: { label: '—', color: 'var(--color-text-muted)' },
}

export function StateBadge({ state }: { state: EnergyState }) {
  const m = ENERGY_STATE_META[state]
  return (
    <span className="disc-badge">
      <span className="dot" style={{ backgroundColor: m.color }} />
      {m.label}
    </span>
  )
}
