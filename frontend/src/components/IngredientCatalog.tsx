import { useEffect, useState } from 'react'
import { toast } from '../toast'
import {
  createIngredient,
  deleteIngredient,
  listIngredients,
  recipesByIngredient,
  seedIngredients,
  type BaseUnit,
  type CatalogIngredient,
  type Recipe,
} from '../api'
import KoreIcon from './icons'
import { Empty, Loading } from './state'

function num(value: string): number | null {
  const n = Number(value)
  return value.trim() === '' || Number.isNaN(n) ? null : n
}

const EMPTY = { name: '', baseUnit: 'GRAM' as BaseUnit, imageUrl: '', calories: '', protein: '', carbs: '', fat: '', fiber: '', sugars: '', sodium: '' }

export default function IngredientCatalog() {
  const [items, setItems] = useState<CatalogIngredient[]>([])
  const [form, setForm] = useState({ ...EMPTY })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [openId, setOpenId] = useState<number | null>(null)
  const [usedIn, setUsedIn] = useState<Recipe[]>([])
  const [loadingRecipes, setLoadingRecipes] = useState(false)

  async function refresh() {
    setError(null)
    try {
      setItems(await listIngredients())
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
      toast.success('Hecho')
    } catch (err) {
      toast.error((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  async function add(e: React.FormEvent) {
    e.preventDefault()
    if (form.name.trim() === '') return
    await run(async () => {
      await createIngredient({
        name: form.name.trim(),
        baseUnit: form.baseUnit,
        imageUrl: form.imageUrl.trim() === '' ? null : form.imageUrl.trim(),
        calories: num(form.calories),
        protein: num(form.protein),
        carbs: num(form.carbs),
        fat: num(form.fat),
        fiber: num(form.fiber),
        sugars: num(form.sugars),
        sodium: num(form.sodium),
      })
      setForm({ ...EMPTY })
    })
  }

  async function toggleRecipes(i: CatalogIngredient) {
    if (openId === i.id) {
      setOpenId(null)
      setUsedIn([])
      return
    }
    setOpenId(i.id)
    setUsedIn([])
    setLoadingRecipes(true)
    try {
      setUsedIn(await recipesByIngredient(i.id))
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setLoadingRecipes(false)
    }
  }

  const unit = (u: BaseUnit) => (u === 'GRAM' ? '100 g' : '100 ml')

  return (
    <>
      {error && <p className="error">{error}</p>}
      <form className="card form" onSubmit={add}>
        <h3>Nuevo ingrediente</h3>
        <p className="muted">Valores nutricionales por 100 g o 100 ml.</p>
        <div className="grid">
          <label>
            Nombre
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </label>
          <label>
            Base
            <select value={form.baseUnit} onChange={(e) => setForm({ ...form, baseUnit: e.target.value as BaseUnit })}>
              <option value="GRAM">por 100 g</option>
              <option value="MILLILITER">por 100 ml</option>
            </select>
          </label>
          <label>
            Imagen (URL)
            <input value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} placeholder="https://… .jpg" />
          </label>
          <label>
            kcal
            <input type="number" min="0" step="0.1" value={form.calories} onChange={(e) => setForm({ ...form, calories: e.target.value })} />
          </label>
          <label>
            Proteína (g)
            <input type="number" min="0" step="0.1" value={form.protein} onChange={(e) => setForm({ ...form, protein: e.target.value })} />
          </label>
          <label>
            Carbos (g)
            <input type="number" min="0" step="0.1" value={form.carbs} onChange={(e) => setForm({ ...form, carbs: e.target.value })} />
          </label>
          <label>
            Grasa (g)
            <input type="number" min="0" step="0.1" value={form.fat} onChange={(e) => setForm({ ...form, fat: e.target.value })} />
          </label>
          <label>
            Fibra (g)
            <input type="number" min="0" step="0.1" value={form.fiber} onChange={(e) => setForm({ ...form, fiber: e.target.value })} />
          </label>
          <label>
            Azúcares (g)
            <input type="number" min="0" step="0.1" value={form.sugars} onChange={(e) => setForm({ ...form, sugars: e.target.value })} />
          </label>
          <label>
            Sodio (mg)
            <input type="number" min="0" step="0.1" value={form.sodium} onChange={(e) => setForm({ ...form, sodium: e.target.value })} />
          </label>
        </div>
        <div className="actions">
          <button type="submit" disabled={busy || form.name.trim() === ''}>
            Añadir
          </button>
          <button type="button" className="secondary" disabled={busy} onClick={() => run(seedIngredients)}>
            Cargar ejemplos
          </button>
        </div>
      </form>

      <h3>Catálogo ({items.length})</h3>
      {items.length === 0 ? (
        <Empty icon="nutrition">Sin ingredientes todavía. Usa «Cargar ejemplos» para empezar.</Empty>
      ) : (
        <div className="ex-grid">
          {items.map((i) => (
            <article key={i.id} className="ex-card fade-up">
              <div className="ex-thumb">
                {i.imageUrl ? (
                  <img src={i.imageUrl} alt={i.name} loading="lazy" />
                ) : (
                  <span className="ex-thumb-empty">
                    <KoreIcon name="leaf" size={30} />
                  </span>
                )}
              </div>
              <div className="ex-body">
                <strong>{i.name}</strong>
                <span className="muted">por {unit(i.baseUnit)}</span>
                <div className="ex-meta">
                  {i.calories != null && <span className="chip">{i.calories} kcal</span>}
                  {i.protein != null && <span>P {i.protein}</span>}
                  {i.carbs != null && <span>C {i.carbs}</span>}
                  {i.fat != null && <span>G {i.fat}</span>}
                </div>
                <div className="ex-detail" style={{ borderTop: 'none', paddingTop: '0.4rem' }}>
                  <button className="link" onClick={() => toggleRecipes(i)}>
                    {openId === i.id ? 'Ocultar recetas' : 'Ver recetas'}
                  </button>
                  <button className="link danger" onClick={() => run(() => deleteIngredient(i.id))}>
                    Eliminar
                  </button>
                  {openId === i.id && (
                    <div style={{ marginTop: '0.4rem' }}>
                      {loadingRecipes ? (
                        <Loading />
                      ) : usedIn.length === 0 ? (
                        <p className="muted">Ninguna receta usa este ingrediente todavía.</p>
                      ) : (
                        <ul className="timeline">
                          {usedIn.map((r) => (
                            <li key={r.id}>
                              <span>{r.name}</span>
                              {r.calories != null && <span className="muted">{r.calories} kcal</span>}
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </>
  )
}
