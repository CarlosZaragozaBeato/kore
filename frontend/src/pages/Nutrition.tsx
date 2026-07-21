import { useEffect, useState } from 'react'
import {
  createDietPlan,
  createRecipe,
  deleteDietPlan,
  deleteRecipe,
  listDietPlans,
  listRecipes,
  updateDietPlan,
  updateRecipe,
  type DietPlan,
  type DietPlanRequest,
  type Recipe,
  type RecipeRequest,
} from '../api'
import RecipeForm from '../components/RecipeForm'
import DietPlanForm from '../components/DietPlanForm'

type Sub = 'recipes' | 'diets'
type RecipeMode = 'list' | 'new' | Recipe
type DietMode = 'list' | 'new' | DietPlan

export default function Nutrition() {
  const [sub, setSub] = useState<Sub>('recipes')
  const [recipes, setRecipes] = useState<Recipe[]>([])
  const [diets, setDiets] = useState<DietPlan[]>([])
  const [recipeMode, setRecipeMode] = useState<RecipeMode>('list')
  const [dietMode, setDietMode] = useState<DietMode>('list')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function refresh() {
    setError(null)
    try {
      const [r, d] = await Promise.all([listRecipes(), listDietPlans()])
      setRecipes(r)
      setDiets(d)
    } catch (err) {
      setError((err as Error).message)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  async function run(fn: () => Promise<unknown>) {
    setBusy(true)
    setError(null)
    try {
      await fn()
      await refresh()
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  async function saveRecipe(req: RecipeRequest) {
    await run(async () => {
      if (recipeMode !== 'list' && recipeMode !== 'new') {
        await updateRecipe(recipeMode.id, req)
      } else {
        await createRecipe(req)
      }
      setRecipeMode('list')
    })
  }

  async function saveDiet(req: DietPlanRequest) {
    await run(async () => {
      if (dietMode !== 'list' && dietMode !== 'new') {
        await updateDietPlan(dietMode.id, req)
      } else {
        await createDietPlan(req)
      }
      setDietMode('list')
    })
  }

  return (
    <section>
      <div className="nav" style={{ marginBottom: '1rem' }}>
        <button className={sub === 'recipes' ? 'tab active' : 'tab'} onClick={() => setSub('recipes')}>
          Recetas
        </button>
        <button className={sub === 'diets' ? 'tab active' : 'tab'} onClick={() => setSub('diets')}>
          Dietas
        </button>
      </div>

      {error && <p className="error">{error}</p>}

      {sub === 'recipes' &&
        (recipeMode !== 'list' ? (
          <RecipeForm
            initial={recipeMode === 'new' ? null : recipeMode}
            busy={busy}
            onSave={saveRecipe}
            onCancel={() => setRecipeMode('list')}
          />
        ) : (
          <>
            <div className="section-head">
              <h3>Recetas ({recipes.length})</h3>
              <button onClick={() => setRecipeMode('new')}>Nueva receta</button>
            </div>
            {recipes.length === 0 ? (
              <p className="muted">Sin recetas todavía.</p>
            ) : (
              recipes.map((r) => (
                <div className="card plan" key={r.id}>
                  <div className="plan-head">
                    <div>
                      <strong>{r.name}</strong>
                      <div className="muted">
                        {r.calories != null ? `${r.calories} kcal` : ''}
                        {r.protein != null ? ` · P ${r.protein}g` : ''}
                        {r.carbs != null ? ` · C ${r.carbs}g` : ''}
                        {r.fat != null ? ` · G ${r.fat}g` : ''}
                        {r.servings != null ? ` · ${r.servings} rac.` : ''}
                      </div>
                    </div>
                    <div className="actions">
                      <button className="secondary" onClick={() => setRecipeMode(r)}>
                        Editar
                      </button>
                      <button className="link danger" onClick={() => run(() => deleteRecipe(r.id))}>
                        Eliminar
                      </button>
                    </div>
                  </div>
                  <ul className="timeline">
                    {r.ingredients.map((ing, idx) => (
                      <li key={idx}>
                        <span>{ing.name}</span>
                        <span className="muted">
                          {ing.quantity != null ? ing.quantity : ''} {ing.unit ?? ''}
                        </span>
                      </li>
                    ))}
                  </ul>
                  {r.steps && <p className="muted" style={{ whiteSpace: 'pre-line' }}>{r.steps}</p>}
                </div>
              ))
            )}
          </>
        ))}

      {sub === 'diets' &&
        (dietMode !== 'list' ? (
          <DietPlanForm
            initial={dietMode === 'new' ? null : dietMode}
            recipeNames={recipes.map((r) => r.name)}
            busy={busy}
            onSave={saveDiet}
            onCancel={() => setDietMode('list')}
          />
        ) : (
          <>
            <div className="section-head">
              <h3>Dietas ({diets.length})</h3>
              <button onClick={() => setDietMode('new')}>Nueva dieta</button>
            </div>
            {diets.length === 0 ? (
              <p className="muted">Sin dietas todavía.</p>
            ) : (
              diets.map((d) => (
                <div className="card plan" key={d.id}>
                  <div className="plan-head">
                    <div>
                      <strong>{d.name}</strong>
                      <div className="muted">
                        {d.startDate}
                        {d.endDate ? ` → ${d.endDate}` : ''}
                        {d.targetCalories != null ? ` · objetivo ${d.targetCalories} kcal` : ''}
                      </div>
                    </div>
                    <div className="actions">
                      <button className="secondary" onClick={() => setDietMode(d)}>
                        Editar
                      </button>
                      <button className="link danger" onClick={() => run(() => deleteDietPlan(d.id))}>
                        Eliminar
                      </button>
                    </div>
                  </div>
                  <ul className="timeline">
                    {d.meals.map((m, idx) => (
                      <li key={idx}>
                        <span>{m.date}</span>
                        <span className="muted">{m.mealType}</span>
                        <span>{m.recipeName ?? ''}</span>
                        {m.notes && <span className="muted">{m.notes}</span>}
                      </li>
                    ))}
                  </ul>
                </div>
              ))
            )}
          </>
        ))}
    </section>
  )
}
