import { useState } from 'react'
import type { Recipe, RecipeRequest } from '../api'

interface Row {
  name: string
  quantity: string
  unit: string
}

function toRows(recipe: Recipe | null): Row[] {
  if (!recipe) return []
  return recipe.ingredients.map((i) => ({
    name: i.name,
    quantity: i.quantity != null ? String(i.quantity) : '',
    unit: i.unit ?? '',
  }))
}

function num(value: string): number | null {
  const n = Number(value)
  return value.trim() === '' || Number.isNaN(n) ? null : n
}

interface Props {
  initial: Recipe | null
  busy: boolean
  onSave: (req: RecipeRequest) => void
  onCancel: () => void
}

export default function RecipeForm({ initial, busy, onSave, onCancel }: Props) {
  const [name, setName] = useState(initial?.name ?? '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [servings, setServings] = useState(initial?.servings != null ? String(initial.servings) : '')
  const [calories, setCalories] = useState(initial?.calories != null ? String(initial.calories) : '')
  const [protein, setProtein] = useState(initial?.protein != null ? String(initial.protein) : '')
  const [carbs, setCarbs] = useState(initial?.carbs != null ? String(initial.carbs) : '')
  const [fat, setFat] = useState(initial?.fat != null ? String(initial.fat) : '')
  const [steps, setSteps] = useState(initial?.steps ?? '')
  const [rows, setRows] = useState<Row[]>(toRows(initial))

  function addRow() {
    setRows([...rows, { name: '', quantity: '', unit: '' }])
  }
  function updateRow(i: number, patch: Partial<Row>) {
    setRows(rows.map((r, idx) => (idx === i ? { ...r, ...patch } : r)))
  }
  function removeRow(i: number) {
    setRows(rows.filter((_, idx) => idx !== i))
  }

  function submit(e: React.FormEvent) {
    e.preventDefault()
    onSave({
      name,
      description: description.trim() === '' ? null : description.trim(),
      servings: num(servings),
      calories: num(calories),
      protein: num(protein),
      carbs: num(carbs),
      fat: num(fat),
      steps: steps.trim() === '' ? null : steps,
      ingredients: rows
        .filter((r) => r.name.trim() !== '')
        .map((r) => ({ name: r.name.trim(), quantity: num(r.quantity), unit: r.unit.trim() === '' ? null : r.unit.trim() })),
    })
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h3>{initial ? 'Editar receta' : 'Nueva receta'}</h3>
      <div className="grid">
        <label>
          Nombre
          <input value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label>
          Raciones
          <input type="number" min="1" value={servings} onChange={(e) => setServings(e.target.value)} />
        </label>
        <label>
          Calorías (total)
          <input type="number" min="0" value={calories} onChange={(e) => setCalories(e.target.value)} />
        </label>
        <label>
          Proteína (g)
          <input type="number" min="0" value={protein} onChange={(e) => setProtein(e.target.value)} />
        </label>
        <label>
          Carbohidratos (g)
          <input type="number" min="0" value={carbs} onChange={(e) => setCarbs(e.target.value)} />
        </label>
        <label>
          Grasa (g)
          <input type="number" min="0" value={fat} onChange={(e) => setFat(e.target.value)} />
        </label>
      </div>
      <label>
        Descripción
        <input value={description} onChange={(e) => setDescription(e.target.value)} />
      </label>

      <h4>Ingredientes</h4>
      {rows.length === 0 && <p className="muted">Sin ingredientes. Añade el primero.</p>}
      {rows.map((r, i) => (
        <div className="session-row" key={i}>
          <input placeholder="ingrediente" value={r.name} onChange={(e) => updateRow(i, { name: e.target.value })} />
          <input type="number" min="0" step="0.01" placeholder="cantidad" value={r.quantity} onChange={(e) => updateRow(i, { quantity: e.target.value })} />
          <input placeholder="unidad" value={r.unit} onChange={(e) => updateRow(i, { unit: e.target.value })} />
          <button type="button" className="link danger" onClick={() => removeRow(i)}>
            ✕
          </button>
        </div>
      ))}
      <button type="button" className="secondary" onClick={addRow}>
        + Añadir ingrediente
      </button>

      <label>
        Pasos (uno por línea)
        <textarea rows={4} value={steps} onChange={(e) => setSteps(e.target.value)} />
      </label>

      <div className="actions">
        <button type="submit" disabled={busy || name.trim() === ''}>
          Guardar receta
        </button>
        <button type="button" className="secondary" onClick={onCancel} disabled={busy}>
          Cancelar
        </button>
      </div>
    </form>
  )
}
