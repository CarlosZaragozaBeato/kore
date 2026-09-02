import { useEffect, useMemo, useState } from 'react'
import { Empty, Loading } from '../components/state'
import {
  acceptPlannedSession,
  deletePlannedSession,
  generateVariants,
  getAnalytics,
  getRecommendation,
  listBlocks,
  listPlannedSessions,
  listPlans,
  listStrengthSessions,
  listWorkouts,
  rejectPlannedSession,
  type Block,
  type Dashboard,
  type PlanRecommendation,
  type PlannedSession,
  type Plan,
  type StrengthSession,
  type Workout,
} from '../api'
import DayPlanner from '../components/DayPlanner'
import Periodization from '../components/Periodization'
import SessionComparison from '../components/SessionComparison'
import { toast } from '../toast'
import { disciplineColor, disciplineLabel } from '../discipline'
import { formatStep } from '../steps'
import { formatDuration, formatPace, metersToKm } from '../format'
import {
  WEEKDAYS,
  addMonths,
  dayNum,
  isSameMonth,
  isoOf,
  monthLabel,
  monthMatrix,
  parseIso,
  todayIso,
  weekDates,
  weekLabel,
} from '../calendar'

type CalView = 'month' | 'week'

interface DayData {
  workouts: Workout[]
  planned: PlannedSession[]
  calendar: PlannedSession[]
  strength: StrengthSession[]
}

function emptyDay(): DayData {
  return { workouts: [], planned: [], calendar: [], strength: [] }
}

const STATUS_LABEL: Record<string, string> = {
  PROPOSED: 'Propuesta',
  ACCEPTED: 'Aceptada',
  REJECTED: 'Descartada',
}

/** Agrupa las sesiones sueltas de un día por variante (mismo grupo = alternativas). */
function groupCalendar(sessions: PlannedSession[]): PlannedSession[][] {
  const groups = new Map<string, PlannedSession[]>()
  for (const s of sessions) {
    const key = s.variantGroup ?? `solo-${s.id}`
    const g = groups.get(key) ?? []
    g.push(s)
    groups.set(key, g)
  }
  return [...groups.values()]
}

/** Punto de color de disciplina. */
function Dot({ type }: { type: string }) {
  return <span className="cal-dot" style={{ backgroundColor: disciplineColor(type) }} title={disciplineLabel(type)} />
}

export default function Calendar({
  initialDate,
  initialView,
}: {
  initialDate?: string
  initialView?: CalView
}) {
  const [anchor, setAnchor] = useState(() => parseIso(initialDate ?? todayIso()))
  const [view, setView] = useState<CalView>(initialView ?? 'month')
  const [selected, setSelected] = useState<string>(initialDate ?? todayIso())

  const [workouts, setWorkouts] = useState<Workout[]>([])
  const [plans, setPlans] = useState<Plan[]>([])
  const [calendarSessions, setCalendarSessions] = useState<PlannedSession[]>([])
  const [strength, setStrength] = useState<StrengthSession[]>([])
  const [blocks, setBlocks] = useState<Block[]>([])
  const [recommendation, setRecommendation] = useState<PlanRecommendation | null>(null)
  const [analytics, setAnalytics] = useState<Dashboard | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  // Formulario de planificación abierto: fecha, grupo de variantes y sesión en edición.
  const [planner, setPlanner] = useState<{ date: string; group: string | null; editing: PlannedSession | null } | null>(null)
  // Id de la sesión cuya comparación (planificado vs realizado) está desplegada.
  const [compareFor, setCompareFor] = useState<number | null>(null)

  useEffect(() => {
    let alive = true
    Promise.all([
      listWorkouts(),
      listPlans(),
      listPlannedSessions(),
      listStrengthSessions(),
      getAnalytics(),
      getRecommendation(),
      listBlocks(),
    ])
      .then(([wo, p, cs, ss, a, rec, bl]) => {
        if (!alive) return
        setWorkouts(wo)
        setPlans(p)
        setCalendarSessions(cs)
        setStrength(ss)
        setAnalytics(a)
        setRecommendation(rec)
        setBlocks(bl)
      })
      .catch((err) => alive && setError((err as Error).message))
      .finally(() => alive && setLoading(false))
    return () => {
      alive = false
    }
  }, [])

  async function reloadCalendar() {
    setCalendarSessions(await listPlannedSessions())
  }

  async function reloadBlocks() {
    setBlocks(await listBlocks())
  }

  async function act(fn: () => Promise<unknown>) {
    try {
      await fn()
      await reloadCalendar()
    } catch (err) {
      toast.error((err as Error).message)
    }
  }

  // Genera variantes de descarga/subida de una sesión y refresca la recomendación.
  async function suggestVariants(id: number) {
    try {
      const r = await generateVariants(id)
      setRecommendation(r.recommendation)
      toast.success(`Variantes creadas · recomendación: ${r.recommendation.label}`)
      await reloadCalendar()
    } catch (err) {
      toast.error((err as Error).message)
    }
  }

  // Índice por día: entrenos + sesiones planificadas + calendario + gimnasio.
  const byDay = useMemo(() => {
    const map = new Map<string, DayData>()
    const at = (iso: string) => {
      let d = map.get(iso)
      if (!d) {
        d = emptyDay()
        map.set(iso, d)
      }
      return d
    }
    for (const w of workouts) at(w.date).workouts.push(w)
    for (const p of plans) for (const s of p.sessions) at(s.date).planned.push(s)
    for (const s of calendarSessions) at(s.date).calendar.push(s)
    for (const s of strength) at(s.date).strength.push(s)
    return map
  }, [workouts, plans, calendarSessions, strength])

  const today = todayIso()
  const focusWeek = weekDates(selected)

  // Métricas de la semana en foco (a partir de los entrenos del rango).
  const weekMetrics = useMemo(() => {
    const set = new Set(focusWeek)
    const inWeek = workouts.filter((w) => set.has(w.date))
    const distance = inWeek.reduce((a, w) => a + (w.distanceMeters ?? 0), 0)
    const kcal = inWeek.reduce((a, w) => a + (w.energyKcal ?? 0), 0)
    const monday = focusWeek[0]
    const sunday = focusWeek[6]
    const period = analytics?.weekly.find((p) => p.from <= monday && sunday <= p.to)
      ?? analytics?.weekly.find((p) => p.from <= monday && monday <= p.to)
    return { count: inWeek.length, distance, kcal, load: period ? Math.round(period.load) : null }
  }, [focusWeek, workouts, analytics])

  if (loading) return <Loading />
  if (error) return <p className="error">{error}</p>

  const rows = view === 'month' ? monthMatrix(anchor) : [focusWeek]

  function go(dir: -1 | 1) {
    if (view === 'month') {
      setAnchor((a) => addMonths(a, dir))
    } else {
      // Semana: mover el día seleccionado ±7.
      const d = parseIso(selected)
      d.setDate(d.getDate() + dir * 7)
      setSelected(isoOf(d))
      setAnchor(d)
    }
  }

  const day = byDay.get(selected) ?? emptyDay()

  return (
    <section>
      <div className="section-head">
        <h2 style={{ margin: 0 }}>Calendario</h2>
        <div className="nav">
          <button className={view === 'month' ? 'tab active' : 'tab'} onClick={() => setView('month')}>
            Mes
          </button>
          <button className={view === 'week' ? 'tab active' : 'tab'} onClick={() => setView('week')}>
            Semana
          </button>
        </div>
      </div>

      <div className="cal-toolbar">
        <button className="secondary" onClick={() => go(-1)} aria-label="Anterior">
          ‹
        </button>
        <strong className="cal-title">
          {view === 'month' ? monthLabel(anchor) : weekLabel(selected)}
        </strong>
        <button className="secondary" onClick={() => go(1)} aria-label="Siguiente">
          ›
        </button>
        <button
          className="secondary"
          onClick={() => {
            setAnchor(parseIso(today))
            setSelected(today)
          }}
        >
          Hoy
        </button>
      </div>

      {/* Métricas de la semana en foco */}
      <div className="cards" style={{ marginTop: '0.4rem' }}>
        <div className="stat">
          <span className="muted">Semana {weekLabel(selected)}</span>
          <span className="stat-value">{weekMetrics.count}</span>
          <span className="muted">entreno{weekMetrics.count === 1 ? '' : 's'}</span>
        </div>
        <div className="stat">
          <span className="muted">Km</span>
          <span className="stat-value">{metersToKm(weekMetrics.distance)}</span>
          <span className="muted">km</span>
        </div>
        <div className="stat">
          <span className="muted">Kcal</span>
          <span className="stat-value">{weekMetrics.kcal ? Math.round(weekMetrics.kcal) : '—'}</span>
          <span className="muted">quemadas</span>
        </div>
        <div className="stat">
          <span className="muted">Carga</span>
          <span className="stat-value">{weekMetrics.load ?? '—'}</span>
          <span className="muted">semana</span>
        </div>
      </div>

      {recommendation && (
        <div className={`load-rec load-rec-${recommendation.stance.toLowerCase()}`} role="status">
          <span className="load-rec-label">🎯 Carga: {recommendation.label}</span>
          <span className="muted">{recommendation.reason}</span>
        </div>
      )}

      <div className="cal-grid">
        {WEEKDAYS.map((w) => (
          <div key={w} className="cal-head">
            {w}
          </div>
        ))}
        {rows.flat().map((iso) => {
          const d = byDay.get(iso) ?? emptyDay()
          const disciplines = Array.from(new Set(d.workouts.map((w) => w.type)))
          const pending =
            d.planned.filter((s) => !s.done).length +
            d.calendar.filter((s) => s.status !== 'REJECTED').length +
            d.strength.filter((s) => s.status === 'PLANNED').length
          const classes = ['cal-cell']
          if (view === 'month' && !isSameMonth(iso, anchor)) classes.push('cal-out')
          if (iso === today) classes.push('cal-today')
          if (iso === selected) classes.push('cal-selected')
          return (
            <button
              key={iso}
              className={classes.join(' ')}
              aria-label={iso}
              aria-current={iso === selected ? 'date' : undefined}
              onClick={() => setSelected(iso)}
            >
              <span className="cal-num">{dayNum(iso)}</span>
              <span className="cal-marks">
                {disciplines.slice(0, 3).map((t) => (
                  <Dot key={t} type={t} />
                ))}
                {d.strength.length > 0 && <Dot type="STRENGTH" />}
                {pending > 0 && <span className="cal-planned" title={`${pending} planificada(s)`} />}
              </span>
            </button>
          )
        })}
      </div>

      {/* Detalle del día seleccionado */}
      <div className="card plan" style={{ borderLeft: '4px solid var(--color-run)', marginTop: '1rem' }}>
        <div className="section-head">
          <h3 style={{ margin: 0 }}>{selected === today ? 'Hoy' : selected}</h3>
          <button
            className="secondary"
            onClick={() => setPlanner({ date: selected, group: null, editing: null })}
          >
            + Planificar sesión
          </button>
        </div>

        {planner && planner.date === selected && (
          <DayPlanner
            key={planner.editing?.id ?? planner.group ?? 'new'}
            date={planner.date}
            initial={planner.editing}
            variantGroup={planner.group}
            onSaved={() => {
              setPlanner(null)
              void reloadCalendar()
            }}
            onCancel={() => setPlanner(null)}
          />
        )}

        {day.workouts.length === 0 && day.planned.length === 0 && day.calendar.length === 0 && day.strength.length === 0 ? (
          <Empty icon="calendar">Nada registrado ni planificado este día.</Empty>
        ) : (
          <>
            {day.workouts.length > 0 && (
              <>
                <h4 style={{ margin: '0.6rem 0 0.2rem' }}>Entrenos</h4>
                <ul className="timeline">
                  {day.workouts.map((w) => (
                    <li key={w.id}>
                      <Dot type={w.type} />
                      <span>{disciplineLabel(w.type)}</span>
                      <span className="muted">
                        {w.distanceMeters != null ? `${metersToKm(w.distanceMeters)} km` : ''}
                        {w.durationSeconds != null ? ` · ${formatDuration(w.durationSeconds)}` : ''}
                        {w.paceSecondsPerKm != null ? ` · ${formatPace(w.paceSecondsPerKm)}` : ''}
                        {w.avgCadenceSpm != null ? ` · ${w.avgCadenceSpm} spm` : ''}
                        {w.strideLengthMeters != null ? ` · ${w.strideLengthMeters.toFixed(2)} m/paso` : ''}
                        {w.energyKcal != null ? ` · ${w.energyKcal} kcal` : ''}
                        {w.source === 'SUUNTO' ? ' · Suunto' : ''}
                      </span>
                    </li>
                  ))}
                </ul>
              </>
            )}

            {day.planned.length > 0 && (
              <>
                <h4 style={{ margin: '0.6rem 0 0.2rem' }}>Planificado</h4>
                <ul className="timeline">
                  {day.planned.map((s, i) => (
                    <li key={i} className={s.done ? 'done' : ''}>
                      <span className="mark">{s.done ? '✓' : '○'}</span>
                      <Dot type={s.type} />
                      <span>{disciplineLabel(s.type)}</span>
                      <span className="muted">
                        {s.targetDistanceMeters != null ? `${metersToKm(s.targetDistanceMeters)} km` : ''}
                        {s.description ? ` · ${s.description}` : ''}
                      </span>
                      {s.steps && s.steps.length > 0 && (
                        <ol className="step-summary">
                          {s.steps.map((st, si) => (
                            <li key={si}>{formatStep(st)}</li>
                          ))}
                        </ol>
                      )}
                    </li>
                  ))}
                </ul>
              </>
            )}

            {day.calendar.length > 0 && (
              <>
                <h4 style={{ margin: '0.6rem 0 0.2rem' }}>Planificado en calendario</h4>
                {groupCalendar(day.calendar).map((group, gi) => (
                  <div className={group.length > 1 ? 'variant-group' : ''} key={gi}>
                    {group.length > 1 && <span className="eyebrow">Variantes — elige una</span>}
                    {group.map((s) => (
                      <div key={s.id} className={`calendar-session status-${s.status.toLowerCase()}`}>
                        <div className="cs-head">
                          <Dot type={s.type} />
                          <span>{s.variantLabel ?? disciplineLabel(s.type)}</span>
                          <span className={`status-badge status-${s.status.toLowerCase()}`}>{STATUS_LABEL[s.status]}</span>
                          <span className="muted">
                            {s.targetDistanceMeters != null ? `${metersToKm(s.targetDistanceMeters)} km` : ''}
                            {s.description ? ` · ${s.description}` : ''}
                          </span>
                        </div>
                        {s.steps && s.steps.length > 0 && (
                          <ol className="step-summary">
                            {s.steps.map((st, si) => (
                              <li key={si}>{formatStep(st)}</li>
                            ))}
                          </ol>
                        )}
                        <div className="cs-actions">
                          {s.status !== 'ACCEPTED' && s.id != null && (
                            <button className="link" onClick={() => void act(() => acceptPlannedSession(s.id as number))}>
                              Aceptar
                            </button>
                          )}
                          {s.status !== 'REJECTED' && s.id != null && (
                            <button className="link" onClick={() => void act(() => rejectPlannedSession(s.id as number))}>
                              Descartar
                            </button>
                          )}
                          {day.workouts.length > 0 && s.id != null && (
                            <button
                              className="link"
                              onClick={() => setCompareFor((cur) => (cur === s.id ? null : (s.id as number)))}
                            >
                              {compareFor === s.id ? 'Ocultar comparación' : 'Comparar'}
                            </button>
                          )}
                          <button
                            className="link"
                            onClick={() => setPlanner({ date: selected, group: s.variantGroup, editing: null })}
                          >
                            Variante
                          </button>
                          {s.id != null && (
                            <button className="link" onClick={() => void suggestVariants(s.id as number)}>
                              Sugerir variantes
                            </button>
                          )}
                          <button
                            className="link"
                            onClick={() => setPlanner({ date: selected, group: s.variantGroup, editing: s })}
                          >
                            Editar
                          </button>
                          {s.id != null && (
                            <button
                              className="link danger"
                              onClick={() => {
                                if (confirm('¿Eliminar esta sesión planificada?')) {
                                  void act(() => deletePlannedSession(s.id as number))
                                }
                              }}
                            >
                              Eliminar
                            </button>
                          )}
                        </div>
                        {compareFor === s.id && s.id != null && <SessionComparison sessionId={s.id} />}
                      </div>
                    ))}
                  </div>
                ))}
              </>
            )}

            {day.strength.length > 0 && (
              <>
                <h4 style={{ margin: '0.6rem 0 0.2rem' }}>Gimnasio</h4>
                <ul className="timeline">
                  {day.strength.map((s) => (
                    <li key={s.id} className={s.status === 'DONE' ? 'done' : ''}>
                      <span className="mark">{s.status === 'DONE' ? '✓' : '○'}</span>
                      <Dot type="STRENGTH" />
                      <span>{s.routineName ?? 'Sesión de fuerza'}</span>
                      <span className="muted">
                        {s.status === 'PLANNED' ? 'Planificada' : 'Hecha'}
                        {s.notes ? ` · ${s.notes}` : ''}
                      </span>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </>
        )}
      </div>

      <Periodization blocks={blocks} selectedDate={selected} onChanged={() => void reloadBlocks()} />
    </section>
  )
}
