// Conversión y presentación de unidades. Se almacena en metros/segundos;
// aquí formateamos para el usuario.

export function metersToKm(meters: number | null): string {
  return meters == null ? '—' : (meters / 1000).toFixed(2)
}

export function formatDuration(seconds: number | null): string {
  if (seconds == null) return '—'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  const mm = String(m).padStart(2, '0')
  const ss = String(s).padStart(2, '0')
  return h > 0 ? `${h}:${mm}:${ss}` : `${m}:${ss}`
}

export function formatPace(secondsPerKm: number | null): string {
  if (secondsPerKm == null) return '—'
  const m = Math.floor(secondsPerKm / 60)
  const s = Math.floor(secondsPerKm % 60)
  return `${m}:${String(s).padStart(2, '0')}/km`
}
