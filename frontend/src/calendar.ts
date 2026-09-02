// Utilidades de fechas para el Calendario. Todo se maneja en horario LOCAL y con
// cadenas 'YYYY-MM-DD' (el backend guarda fechas planas, sin zona). Para evitar
// desfases por zona horaria construimos los Date al mediodía local.

export const WEEKDAYS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

const MONTHS = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
]

/** 'YYYY-MM-DD' local de un Date. */
export function isoOf(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** Parsea 'YYYY-MM-DD' a Date local (mediodía, para esquivar DST). */
export function parseIso(s: string): Date {
  const [y, m, d] = s.split('-').map(Number)
  return new Date(y, m - 1, d, 12, 0, 0, 0)
}

export function todayIso(): string {
  return isoOf(new Date())
}

export function addDays(d: Date, n: number): Date {
  const r = new Date(d)
  r.setDate(r.getDate() + n)
  return r
}

export function addMonths(d: Date, n: number): Date {
  const r = new Date(d)
  r.setDate(1)
  r.setMonth(r.getMonth() + n)
  return r
}

/** Lunes de la semana (ISO) que contiene la fecha. */
export function mondayOf(d: Date): Date {
  const r = new Date(d)
  const dow = (r.getDay() + 6) % 7 // 0 = lunes
  r.setDate(r.getDate() - dow)
  r.setHours(12, 0, 0, 0)
  return r
}

/** Las 7 fechas ISO (lun→dom) de la semana que contiene `iso`. */
export function weekDates(iso: string): string[] {
  const monday = mondayOf(parseIso(iso))
  return Array.from({ length: 7 }, (_, i) => isoOf(addDays(monday, i)))
}

/**
 * Matriz de semanas (lun→dom) que cubren el mes de `anchor`, con relleno de los
 * días de meses colindantes para completar filas.
 */
export function monthMatrix(anchor: Date): string[][] {
  const first = new Date(anchor.getFullYear(), anchor.getMonth(), 1, 12)
  const start = mondayOf(first)
  const weeks: string[][] = []
  let cursor = start
  // Hasta 6 filas; paramos cuando ya cubrimos todo el mes.
  for (let w = 0; w < 6; w++) {
    const row = Array.from({ length: 7 }, (_, i) => isoOf(addDays(cursor, i)))
    weeks.push(row)
    cursor = addDays(cursor, 7)
    const lastOfRow = parseIso(row[6])
    if (lastOfRow.getMonth() !== anchor.getMonth() && lastOfRow > first) break
  }
  return weeks
}

export function monthLabel(d: Date): string {
  return `${MONTHS[d.getMonth()]} ${d.getFullYear()}`
}

/** Etiqueta de rango de una semana, p. ej. "14–20 jul". */
export function weekLabel(iso: string): string {
  const monday = mondayOf(parseIso(iso))
  const sunday = addDays(monday, 6)
  const m1 = MONTHS[monday.getMonth()].slice(0, 3)
  const m2 = MONTHS[sunday.getMonth()].slice(0, 3)
  return monday.getMonth() === sunday.getMonth()
    ? `${monday.getDate()}–${sunday.getDate()} ${m1}`
    : `${monday.getDate()} ${m1} – ${sunday.getDate()} ${m2}`
}

export function isSameMonth(iso: string, anchor: Date): boolean {
  return parseIso(iso).getMonth() === anchor.getMonth()
}

export function dayNum(iso: string): number {
  return parseIso(iso).getDate()
}
