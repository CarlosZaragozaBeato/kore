import type { Workout } from '../api'
import { formatDuration, formatPace, metersToKm } from '../format'
import { disciplineColor, disciplineLabel } from '../discipline'

/** Una casilla de métrica; solo se pinta si hay valor. */
function Tile({ label, value, accent }: { label: string; value: string | null; accent?: boolean }) {
  if (value == null) return null
  return (
    <div className={accent ? 'metric-tile accent' : 'metric-tile'}>
      <span className="label">{label}</span>
      <span className="value">{value}</span>
    </div>
  )
}

interface Props {
  workout: Workout
  weekLoad: number | null
  onBack: () => void
  onEdit: () => void
  onDelete: () => void
}

/**
 * Vista de detalle de un entreno: primero lo general y sus métricas (incluidas
 * las derivadas cadencia/zancada de la Fase 18); el desglose por tramos (splits)
 * llegará al importar el FIT del entreno.
 */
export default function WorkoutDetail({ workout: w, weekLoad, onBack, onEdit, onDelete }: Props) {
  return (
    <section className="fade-up">
      <div className="section-head">
        <button className="link" onClick={onBack}>
          ‹ Volver
        </button>
        <div className="actions">
          <button className="secondary" onClick={onEdit}>
            Editar
          </button>
          <button className="link danger" onClick={onDelete}>
            Eliminar
          </button>
        </div>
      </div>

      <div className="card plan" style={{ borderLeft: `4px solid ${disciplineColor(w.type)}` }}>
        <div className="plan-head">
          <div>
            <strong style={{ fontSize: '1.1rem' }}>{disciplineLabel(w.type)}</strong>
            <div className="muted">{w.date}</div>
          </div>
          <span className="muted">{w.source === 'SUUNTO' ? 'Suunto' : 'Manual'}</span>
        </div>

        <div className="metric-tiles">
          <Tile label="Distancia" value={w.distanceMeters != null ? `${metersToKm(w.distanceMeters)} km` : null} accent />
          <Tile label="Duración" value={w.durationSeconds != null ? formatDuration(w.durationSeconds) : null} />
          <Tile label="Ritmo" value={w.paceSecondsPerKm != null ? formatPace(w.paceSecondsPerKm) : null} accent />
          <Tile label="FC media" value={w.avgHeartRate != null ? `${w.avgHeartRate} ppm` : null} />
          <Tile label="FC máx" value={w.maxHeartRate != null ? `${w.maxHeartRate} ppm` : null} />
          <Tile label="Cadencia" value={w.avgCadenceSpm != null ? `${w.avgCadenceSpm} spm` : null} accent />
          <Tile label="Zancada" value={w.strideLengthMeters != null ? `${w.strideLengthMeters.toFixed(2)} m` : null} />
          <Tile label="Energía" value={w.energyKcal != null ? `${Math.round(w.energyKcal)} kcal` : null} />
          <Tile label="Pasos" value={w.stepCount != null ? w.stepCount.toLocaleString('es-ES') : null} />
          <Tile label="Esfuerzo" value={w.perceivedEffort != null ? `${w.perceivedEffort}/10` : null} />
          <Tile label="Carga semana" value={weekLoad != null ? String(Math.round(weekLoad)) : null} />
        </div>

        {w.notes && (
          <p className="muted" style={{ marginTop: '0.6rem' }}>
            {w.notes}
          </p>
        )}
      </div>

      <div className="card">
        <h4 style={{ margin: '0 0 0.4rem' }}>Splits por tramo</h4>
        <p className="muted">
          El desglose por tramos (ritmo, FC, cadencia y zancada por km/lap) llegará al importar el
          FIT del entreno desde Suunto. De momento se muestran las métricas medias del entreno.
        </p>
      </div>
    </section>
  )
}
