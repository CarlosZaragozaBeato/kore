import KoreIcon, { type IconName } from './icons'

/** Estado de carga uniforme: spinner Kore + mensaje. */
export function Loading({ label = 'Cargando…' }: { label?: string }) {
  return (
    <div className="state-loading" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      <span className="muted">{label}</span>
    </div>
  )
}

/** Estado vacío uniforme: icono opcional + texto (y una acción opcional). */
export function Empty({
  children,
  icon,
}: {
  children: React.ReactNode
  icon?: IconName
}) {
  return (
    <div className="state-empty">
      {icon && <KoreIcon name={icon} size={28} />}
      <p className="muted">{children}</p>
    </div>
  )
}
