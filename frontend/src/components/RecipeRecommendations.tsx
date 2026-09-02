import { useEffect, useState } from 'react'
import { getRecipeRecommendations, type RecipeRecommendation } from '../api'
import { Empty, Loading } from './state'

const INTENSITY: Record<string, string> = {
  HIGH: 'Carga alta',
  MODERATE: 'Carga moderada',
  REST: 'Descanso',
}

const FOCUS: Record<string, string> = {
  CARB: 'Repostaje de carbohidratos',
  BALANCED: 'Equilibrado',
  PROTEIN: 'Recuperación / proteína',
}

export default function RecipeRecommendations() {
  const [data, setData] = useState<RecipeRecommendation | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getRecipeRecommendations()
      .then(setData)
      .catch((err) => setError((err as Error).message))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <Loading label="Calculando recomendaciones…" />
  if (error) return <p className="error">{error}</p>
  if (!data) return null

  const c = data.context

  return (
    <>
      <div className="card plan fade-up" style={{ borderLeft: '4px solid var(--color-run)' }}>
        <div className="plan-head">
          <div>
            <strong>{FOCUS[c.focus] ?? c.focus}</strong>
            <div className="muted">
              {INTENSITY[c.intensity] ?? c.intensity} · {c.sessionsLast7} entreno
              {c.sessionsLast7 === 1 ? '' : 's'} en 7 días
              {c.trainingKcalLast7 > 0 ? ` · ${Math.round(c.trainingKcalLast7)} kcal` : ''}
              {c.targetCalories != null ? ` · objetivo hoy ~${c.targetCalories} kcal` : ''}
            </div>
          </div>
        </div>
        <p className="muted">{c.rationale}</p>
      </div>

      <h3>Recetas sugeridas</h3>
      {data.recommendations.length === 0 ? (
        <Empty icon="nutrition">
          Aún no tienes recetas. Crea alguna con sus macros para recibir sugerencias.
        </Empty>
      ) : (
        data.recommendations.map((r, idx) => (
          <div className="card plan fade-up" key={r.recipeId}>
            <div className="plan-head">
              <div>
                <strong>
                  {idx === 0 ? '★ ' : ''}
                  {r.name}
                </strong>
                <div className="muted">
                  {r.calories != null ? `${r.calories} kcal` : ''}
                  {r.protein != null ? ` · P ${r.protein}g` : ''}
                  {r.carbs != null ? ` · C ${r.carbs}g` : ''}
                  {r.fat != null ? ` · G ${r.fat}g` : ''}
                </div>
              </div>
              <span className="chip">encaje {Math.round(r.score * 100)}%</span>
            </div>
            <p className="muted">{r.reason}</p>
          </div>
        ))
      )}
    </>
  )
}
