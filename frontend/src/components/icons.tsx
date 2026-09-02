// Iconografía Kore: set de iconos de trazo, en línea, que heredan el color
// (currentColor) y el tamaño del contexto. Sin dependencias ni fuentes de icono.
// Trazo consistente (1.8), remates redondeados: la "voz" gráfica de la marca.

export type IconName =
  | 'home'
  | 'calendar'
  | 'workouts'
  | 'plans'
  | 'gym'
  | 'nutrition'
  | 'weight'
  | 'analytics'
  | 'settings'
  | 'sun'
  | 'moon'
  | 'leaf'
  | 'download'
  | 'logout'

interface Props {
  name: IconName
  size?: number | string
  className?: string
}

// Cada entrada es el contenido interno del <svg> (viewBox 0 0 24 24).
const PATHS: Record<IconName, React.ReactNode> = {
  home: (
    <>
      <path d="M3 10.5 12 3l9 7.5" />
      <path d="M5 9.5V20h14V9.5" />
    </>
  ),
  calendar: (
    <>
      <rect x="3" y="4.5" width="18" height="16.5" rx="2" />
      <path d="M3 9.5h18M8 2.5v4M16 2.5v4" />
    </>
  ),
  workouts: <path d="M3 12h4l2.5-7 4 14 2.5-7H21" />,
  plans: (
    <>
      <rect x="4.5" y="4" width="15" height="17" rx="2" />
      <path d="M9 3v3h6V3M8.5 11h7M8.5 15h4.5" />
    </>
  ),
  gym: <path d="M4 9v6M7 7.5v9M17 7.5v9M20 9v6M7 12h10" />,
  nutrition: (
    <>
      <path d="M5 19c0-8 6-14 14-14 0 8-6 14-14 14z" />
      <path d="M6 18 13 11" />
    </>
  ),
  weight: (
    <>
      <path d="M4.5 16a7.5 7.5 0 0 1 15 0" />
      <path d="M12 16l3.5-4" />
    </>
  ),
  analytics: (
    <>
      <path d="M4 21h16" />
      <path d="M6.5 21V11M12 21V4M17.5 21v-6" />
    </>
  ),
  settings: (
    <>
      <path d="M4 7h8M17 7h3M4 12h3M12 12h8M4 17h6M15 17h5" />
      <circle cx="14" cy="7" r="2" />
      <circle cx="9" cy="12" r="2" />
      <circle cx="12" cy="17" r="2" />
    </>
  ),
  sun: (
    <>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2.5M12 19.5V22M2 12h2.5M19.5 12H22M4.9 4.9l1.8 1.8M17.3 17.3l1.8 1.8M19.1 4.9l-1.8 1.8M6.7 17.3l-1.8 1.8" />
    </>
  ),
  moon: <path d="M21 12.8A8.5 8.5 0 1 1 11.2 3 6.6 6.6 0 0 0 21 12.8z" />,
  leaf: (
    <>
      <path d="M5 19c0-8 6-14 14-14 0 8-6 14-14 14z" />
      <path d="M6 18 13 11" />
    </>
  ),
  download: (
    <>
      <path d="M12 3v12M7.5 10.5 12 15l4.5-4.5" />
      <path d="M4 20h16" />
    </>
  ),
  logout: (
    <>
      <path d="M14 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8" />
      <path d="M17 8l4 4-4 4M9 12h12" />
    </>
  ),
}

export default function KoreIcon({ name, size = '1.1em', className }: Props) {
  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {PATHS[name]}
    </svg>
  )
}
