import { useState } from 'react'
import type { DietPlan, DietPlanRequest, MealType } from '../api'

const MEAL_TYPES: MealType[] = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK']

interface Row {
  date: string
  mealType: MealType
  recipeName: string
  notes: string
}

function toRows(plan: DietPlan | null): Row[] {
  if (!plan) return []
  return plan.meals.map((m) => ({
    date: m.date,
    mealType: m.mealType,
    recipeName: m.recipeName ?? '',
    notes: m.notes ?? '',
  }))
}

function num(value: string): number | null {
  const n = Number(value)
  return value.trim() === '' || Number.isNaN(n) ? null : n
}

interface Props {
  initial: DietPlan | null
  recipeNames: string[]
  busy: boolean
  onSave: (req: DietPlanRequest) => void
  onCancel: () => void
}

export default function DietPlanForm({ initial, recipeNames, busy, onSave, onCancel }: Props) {
  const today = new Date().toISOString().slice(0, 10)
  const [name, setName] = useState(initial?.name ?? '')
  const [startDate, setStartDate] = useState(initial?.startDate ?? today)
  const [endDate, setEndDate] = useState(initial?.endDate ?? '')
  const [calories, setCalories] = useState(initial?.targetCalories != null ? String(initial.targetCalories) : '')
  const [protein, setProtein] = useState(initial?.targetProtein != null ? String(initial.targetProtein) : '')
  const [carbs, setCarbs] = useState(initial?.targetCarbs != null ? String(initial.targetCarbs) : '')
  const [fat, setFat] = useState(initial?.targetFat != null ? String(initial.targetFat) : '')
  const [notes, setNotes] = useState(initial?.notes ?? '')
  const [rows, setRows] = useState<Row[]>(toRows(initial))

  function addRow() {
    setRows([...rows, { date: startDate, mealType: 'BREAKFAST', recipeName: '', notes: '' }])
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
      startDate,
      endDate: endDate === '' ? null : endDate,
      targetCalories: num(calories),
      targetProtein: num(protein),
      targetCarbs: num(carbs),
      targetFat: num(fat),
      notes: notes.trim() === '' ? null : notes.trim(),
      meals: rows.map((r) => ({
        date: r.date,
        mealType: r.mealType,
        recipeName: r.recipeName.trim() === '' ? null : r.recipeName.trim(),
        notes: r.notes.trim() === '' ? null : r.notes.trim(),
      })),
    })
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h3>{initial ? 'Editar dieta' : 'Nueva dieta'}</h3>
      <div className="grid">
        <label>
          Nombre
          <input value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label>
          Inicio
          <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} required />
        </label>
        <label>
          Fin (opcional)
          <input type="date" value={endDate} min={startDate} onChange={(e) => setEndDate(e.target.value)} />
        </label>
        <label>
          Objetivo kcal
          <input type="number" min="0" value={calories} onChange={(e) => setCalories(e.target.value)} />
        </label>
        <label>
          Objetivo proteína (g)
          <input type="number" min="0" value={protein} onChange={(e) => setProtein(e.target.value)} />
        </label>
        <label>
          Objetivo carbos (g)
          <input type="number" min="0" value={carbs} onChange={(e) => setCarbs(e.target.value)} />
        </label>
        <label>
          Objetivo grasa (g)
          <input type="number" min="0" value={fat} onChange={(e) => setFat(e.target.value)} />
        </label>
      </div>
      <label>
        Notas
        <input value={notes} onChange={(e) => setNotes(e.target.value)} />
      </label>

      <datalist id="recipe-names">
        {recipeNames.map((n) => (
          <option key={n} value={n} />
        ))}
      </datalist>

      <h4>Comidas</h4>
      {rows.length === 0 && <p className="muted">Sin comidas. Añade la primera.</p>}
      {rows.map((r, i) => (
        <div className="session-row" key={i}>
          <input type="date" value={r.date} onChange={(e) => updateRow(i, { date: e.target.value })} required />
          <select value={r.mealType} onChange={(e) => updateRow(i, { mealType: e.target.value as MealType })}>
            {MEAL_TYPES.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
          <input list="recipe-names" placeholder="receta" value={r.recipeName} onChange={(e) => updateRow(i, { recipeName: e.target.value })} />
          <input placeholder="notas" value={r.notes} onChange={(e) => updateRow(i, { notes: e.target.value })} />
          <button type="button" className="link danger" onClick={() => removeRow(i)}>
            ✕
          </button>
        </div>
      ))}
      <button type="button" className="secondary" onClick={addRow}>
        + Añadir comida
      </button>

      <div className="actions">
        <button type="submit" disabled={busy || name.trim() === ''}>
          Guardar dieta
        </button>
        <button type="button" className="secondary" onClick={onCancel} disabled={busy}>
          Cancelar
        </button>
      </div>
    </form>
  )
}
