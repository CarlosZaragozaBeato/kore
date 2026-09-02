import { useEffect, useState } from 'react'
import { Empty, Loading } from '../components/state'
import {
  getAnalytics,
  getEnergy,
  getWeightGoal,
  listPlans,
  listStrengthSessions,
  listWeight,
  listWorkouts,
  type Dashboard as Analytics,
  type EnergySummary,
  type Plan,
  type PlannedSession,
  type StrengthSession,
  type WeightEntry,
  type WeightGoal,
  type Workout,
} from '../api'
import { metersToKm, formatDuration } from '../format'
import { disciplineColor, disciplineLabel } from '../discipline'
import { StateBadge } from '../energyState'
import type { Navigate } from '../nav'
import { WEEKDAYS, dayNum, todayIso, weekDates } from '../calendar'

function today(): string {
  return todayIso()
}

function Dot({ type }: { type: string }) {
  return <span className="dot" style={{ backgroundColor: disciplineColor(type), display: 'inline-block' }} />
}

export default function Dashboard({ onNavigate }: { onNavigate: Navigate }) {
  const [analytics, setAnalytics] = useState<Analytics | null>(null)
  const [energy, setEnergy] = useState<EnergySummary | null>(null)
  const [weight, setWeight] = useState<WeightEntry[]>([])
  const [goal, setGoal] = useState<WeightGoal | null>(null)
  const [plans, setPlans] = useState<Plan[]>([])
  const [workouts, setWorkouts] = useState<Workout[]>([])
  const [strength, setStrength] = useState<StrengthSession[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let alive = true
    Promise.all([
      getAnalytics(),
      getEnergy(),
      listWeight(),
      getWeightGoal(),
      listPlans(),
      listWorkouts(),
      listStrengthSessions(),
    ])
      .then(([a, e, w, g, p, wo, ss]) => {
        if (!alive) return
        setAnalytics(a)
        setEnergy(e)
        setWeight(w)
        setGoal(g)
        setPlans(p)
        setWorkouts(wo)
        setStrength(ss)
      })
      .catch((err) => alive && setError((err as Error).message))
      .finally(() => alive && setLoading(false))
    return () => {
      alive = false
    }
  }, [])

  if (loading) return <Loading />
  if (error) return <p className="error">{error}</p>

  const d = today()

  // --- Fila de la semana actual (calendario compacto navegable) ---
  const weekIsos = weekDates(d)
  const weekDays = weekIsos.map((iso, i) => {
    const dayWorkouts = workouts.filter((w) => w.date === iso)
    const disciplines = Array.from(new Set(dayWorkouts.map((w) => w.type)))
    const plannedPending = plans
      .flatMap((p) => p.sessions)
      .some((s) => s.date === iso && !s.done)
    const hasStrength = strength.some((s) => s.date === iso)
    return { iso, dow: WEEKDAYS[i], num: dayNum(iso), disciplines, plannedPending, hasStrength }
  })

  // --- Semana (carga y km) ---
  const weekly = analytics?.weekly ?? []
  const thisWeek = weekly[weekly.length - 1]
  const prevWeek = weekly[weekly.length - 2]
  const loadTrend =
    thisWeek && prevWeek ? thisWeek.load - prevWeek.load : null

  // --- Energía ---
  const todayEnergy = energy?.days.find((x) => x.date === d)
  const weekEnergy = energy?.weeks[energy.weeks.length - 1]

  // --- Peso ---
  const latest = weight[0]
  const previous = weight[1]
  const wDelta = latest && previous ? latest.weightKg - previous.weightKg : null
  const inRange =
    latest && goal?.minKg != null && goal?.maxKg != null
      ? latest.weightKg >= goal.minKg && latest.weightKg <= goal.maxKg
      : null

  // --- Plan activo, sesiones de hoy y próximas ---
  const activePlan =
    plans.find((p) => p.startDate <= d && (p.endDate == null || p.endDate >= d)) ??
    [...plans].sort((a, b) => b.startDate.localeCompare(a.startDate))[0]
  const todaySessions = activePlan?.sessions.filter((s) => s.date === d) ?? []
  const upcoming: PlannedSession[] = plans
    .flatMap((p) => p.sessions)
    .filter((s) => s.date > d && !s.done)
    .sort((a, b) => a.date.localeCompare(b.date))
    .slice(0, 5)

  // --- Entrenos ---
  const byDateDesc = [...workouts].sort((a, b) => b.date.localeCompare(a.date))
  const todayWorkouts = workouts.filter((w) => w.date === d)
  const recent = byDateDesc.slice(0, 6)
  const lastSuunto = byDateDesc.find((w) => w.source === 'SUUNTO')

  const noData = workouts.length === 0 && plans.length === 0 && weight.length === 0

  return (
    <section>
      <div className="section-head">
        <h2 style={{ margin: 0 }}>Inicio</h2>
        <button className="link" onClick={() => onNavigate('calendar')}>
          {d} · ver calendario
        </button>
      </div>

      {/* Fila de la semana actual: un vistazo rápido; cada día abre el Calendario */}
      <div className="week-row">
        {weekDays.map((wd) => (
          <button
            key={wd.iso}
            className={wd.iso === d ? 'week-day today' : 'week-day'}
            onClick={() => onNavigate('calendar', { date: wd.iso, view: 'week' })}
          >
            <span className="week-dow">{wd.dow}</span>
            <span className="week-num">{wd.num}</span>
            <span className="week-marks">
              {wd.disciplines.slice(0, 3).map((t) => (
                <Dot key={t} type={t} />
              ))}
              {wd.hasStrength && <Dot type="STRENGTH" />}
              {wd.plannedPending && <span className="week-planned" />}
            </span>
          </button>
        ))}
      </div>

      {noData && (
        <p className="muted">
          Aún no hay datos. Registra entrenos, crea un plan o añade tu peso para ver aquí tu resumen.
        </p>
      )}

      <div className="cards">
        <div className="stat clickable" onClick={() => onNavigate('analytics')}>
          <span className="muted">Carga esta semana</span>
          <span className="stat-value">{thisWeek ? Math.round(thisWeek.load) : '—'}</span>
          {loadTrend != null && (
            <span className={loadTrend === 0 ? 'muted' : loadTrend > 0 ? 'error' : 'ok'}>
              {loadTrend > 0 ? '▲' : loadTrend < 0 ? '▼' : '='} {Math.abs(Math.round(loadTrend))} vs semana previa
            </span>
          )}
        </div>
        <div className="stat clickable" onClick={() => onNavigate('calendar', { date: d, view: 'week' })}>
          <span className="muted">Km esta semana</span>
          <span className="stat-value">{thisWeek ? metersToKm(thisWeek.distanceMeters) : '—'}</span>
          <span className="muted">km · ver semana</span>
        </div>
        <div className="stat clickable" onClick={() => onNavigate('weight')}>
          <span className="muted">Balance hoy</span>
          <span className="stat-value">{todayEnergy ? todayEnergy.balanceKcal : '—'}</span>
          {todayEnergy && <StateBadge state={todayEnergy.state} />}
        </div>
        <div className="stat clickable" onClick={() => onNavigate('weight')}>
          <span className="muted">Peso</span>
          <span className="stat-value">{latest ? `${latest.weightKg}` : '—'}</span>
          {latest && (
            <span className={inRange == null ? 'muted' : inRange ? 'ok' : 'error'}>
              {wDelta != null ? `${wDelta > 0 ? '▲' : wDelta < 0 ? '▼' : '='} ${Math.abs(wDelta).toFixed(1)} kg` : 'kg'}
              {inRange != null ? (inRange ? ' · en rango' : ' · fuera') : ''}
            </span>
          )}
        </div>
      </div>

      <div
        className="card plan clickable"
        style={{ borderLeft: '4px solid var(--color-run)' }}
        onClick={() => onNavigate('calendar', { date: d, view: 'week' })}
      >
        <div className="section-head">
          <h3 style={{ margin: 0 }}>Hoy</h3>
          <span className="muted">
            {todayWorkouts.length} entreno{todayWorkouts.length === 1 ? '' : 's'} registrado
            {todayWorkouts.length === 1 ? '' : 's'}
          </span>
        </div>
        {todaySessions.length === 0 ? (
          <Empty icon="plans">Sin sesiones planificadas para hoy.</Empty>
        ) : (
          <ul className="timeline">
            {todaySessions.map((s, i) => (
              <li key={i} className={s.done ? 'done' : ''}>
                <span className="mark">{s.done ? '✓' : '○'}</span>
                <Dot type={s.type} />
                <span>{disciplineLabel(s.type)}</span>
                <span className="muted">
                  {s.targetDistanceMeters != null ? `${metersToKm(s.targetDistanceMeters)} km` : ''}
                  {s.description ? ` · ${s.description}` : ''}
                </span>
              </li>
            ))}
          </ul>
        )}
        {weekEnergy && (
          <p className="muted">
            Balance semana: {weekEnergy.balanceKcal} kcal (consumidas {weekEnergy.consumedKcal} · quemadas{' '}
            {weekEnergy.burnedKcal})
          </p>
        )}
      </div>

      {activePlan && (
        <div className="card plan clickable" onClick={() => onNavigate('plans')}>
          <div className="plan-head">
            <div>
              <strong>{activePlan.name}</strong>
              {activePlan.goal && <span className="muted"> · {activePlan.goal}</span>}
            </div>
            <span className="muted">
              {activePlan.completedCount}/{activePlan.plannedCount} · {activePlan.adherencePct}%
            </span>
          </div>
          <div className="adherence">
            <div className="adherence-bar">
              <div className="adherence-fill" style={{ width: `${activePlan.adherencePct}%` }} />
            </div>
          </div>
          {upcoming.length > 0 && (
            <>
              <h4 style={{ margin: '0.4rem 0' }}>Próximas sesiones</h4>
              <ul className="timeline">
                {upcoming.map((s, i) => (
                  <li key={i}>
                    <span className="muted">{s.date}</span>
                    <Dot type={s.type} />
                    <span>{disciplineLabel(s.type)}</span>
                    <span className="muted">
                      {s.targetDistanceMeters != null ? `${metersToKm(s.targetDistanceMeters)} km` : ''}
                    </span>
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}

      <div className="card plan clickable" onClick={() => onNavigate('workouts')}>
        <div className="section-head">
          <h3 style={{ margin: 0 }}>Últimos entrenos</h3>
          {lastSuunto && <span className="muted">Suunto: {lastSuunto.date}</span>}
        </div>
        {recent.length === 0 ? (
          <Empty icon="workouts">Sin entrenos todavía.</Empty>
        ) : (
          <ul className="timeline">
            {recent.map((w) => (
              <li key={w.id}>
                <span className="muted">{w.date}</span>
                <Dot type={w.type} />
                <span>{disciplineLabel(w.type)}</span>
                <span className="muted">
                  {w.distanceMeters != null ? `${metersToKm(w.distanceMeters)} km` : ''}
                  {w.durationSeconds != null ? ` · ${formatDuration(w.durationSeconds)}` : ''}
                  {w.source === 'SUUNTO' ? ' · Suunto' : ''}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  )
}
