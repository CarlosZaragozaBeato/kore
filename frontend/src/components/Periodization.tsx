import { useState } from 'react'
import {
  createBlock,
  deleteBlock,
  getBlockSummary,
  type Block,
  type BlockFocus,
  type BlockLevel,
  type BlockSummary,
  type LoadStance,
} from '../api'
import { toast } from '../toast'
import { metersToKm } from '../format'

const LEVELS: BlockLevel[] = ['MACRO', 'MESO', 'MICRO']
const FOCUSES: BlockFocus[] = ['BASE', 'BUILD', 'PEAK', 'TAPER', 'RECOVERY', 'RACE', 'GENERAL']

const LEVEL_LABEL: Record<BlockLevel, string> = {
  MACRO: 'Macro',
  MESO: 'Meso',
  MICRO: 'Micro',
}

const FOCUS_LABEL: Record<BlockFocus, string> = {
  BASE: 'Base',
  BUILD: 'Construcción',
  PEAK: 'Pico',
  TAPER: 'Afinamiento',
  RECOVERY: 'Recuperación',
  RACE: 'Competición',
  GENERAL: 'General',
}

const FOCUS_COLOR: Record<BlockFocus, string> = {
  BASE: '#3b82f6',
  BUILD: '#f59e0b',
  PEAK: '#ef4444',
  TAPER: '#8b5cf6',
  RECOVERY: '#10b981',
  RACE: '#ec4899',
  GENERAL: '#64748b',
}

const STANCE_LABEL: Record<LoadStance, string> = {
  DELOAD: 'Descarga',
  MAINTAIN: 'Mantener',
  BUILD: 'Subir',
}

/** Aplana el árbol para el selector de padre y para localizar el bloque activo. */
function flatten(blocks: Block[]): Block[] {
  return blocks.flatMap((b) => [b, ...flatten(b.children)])
}

/** Bloques que cubren una fecha, del más amplio (macro) al más fino (micro). */
function activeChain(blocks: Block[], iso: string): Block[] {
  return flatten(blocks)
    .filter((b) => b.startDate <= iso && iso <= b.endDate)
    .sort((a, b) => LEVELS.indexOf(a.level) - LEVELS.indexOf(b.level))
}

function FocusChip({ focus }: { focus: BlockFocus }) {
  return (
    <span className="block-focus" style={{ backgroundColor: FOCUS_COLOR[focus] }}>
      {FOCUS_LABEL[focus]}
    </span>
  )
}

export default function Periodization({
  blocks,
  selectedDate,
  onChanged,
}: {
  blocks: Block[]
  selectedDate: string
  onChanged: () => void
}) {
  const [adding, setAdding] = useState(false)
  const [summaries, setSummaries] = useState<Record<number, BlockSummary>>({})

  const chain = activeChain(blocks, selectedDate)

  async function remove(b: Block) {
    if (!confirm(`¿Eliminar el bloque "${b.name}" y todo lo que cuelga de él?`)) return
    try {
      await deleteBlock(b.id)
      toast.success('Bloque eliminado')
      onChanged()
    } catch (err) {
      toast.error((err as Error).message)
    }
  }

  async function loadSummary(b: Block) {
    if (summaries[b.id]) {
      setSummaries((s) => {
        const next = { ...s }
        delete next[b.id]
        return next
      })
      return
    }
    try {
      const sum = await getBlockSummary(b.id)
      setSummaries((s) => ({ ...s, [b.id]: sum }))
    } catch (err) {
      toast.error((err as Error).message)
    }
  }

  function renderBlock(b: Block) {
    const sum = summaries[b.id]
    return (
      <li key={b.id} className={`block-node level-${b.level.toLowerCase()}`}>
        <div className="block-row">
          <span className="block-level">{LEVEL_LABEL[b.level]}</span>
          <strong>{b.name}</strong>
          <FocusChip focus={b.focus} />
          {b.loadStance && <span className="block-stance">{STANCE_LABEL[b.loadStance]}</span>}
          <span className="muted">
            {b.startDate} → {b.endDate}
          </span>
          <span className="block-actions">
            <button className="link" onClick={() => void loadSummary(b)}>
              {sum ? 'Ocultar' : 'Resumen'}
            </button>
            <button className="link danger" onClick={() => void remove(b)}>
              Eliminar
            </button>
          </span>
        </div>
        {b.note && <p className="muted block-note">{b.note}</p>}
        {sum && (
          <div className="block-summary">
            <span>{sum.weeks} sem</span>
            <span>
              {sum.completedWorkouts}/{sum.plannedSessions} sesiones
            </span>
            <span>
              {metersToKm(sum.completedDistanceMeters)}/{metersToKm(sum.plannedDistanceMeters)} km
            </span>
            <span>{sum.adherencePct != null ? `${sum.adherencePct}% adherencia` : 'sin plan'}</span>
          </div>
        )}
        {b.children.length > 0 && <ul className="block-children">{b.children.map(renderBlock)}</ul>}
      </li>
    )
  }

  return (
    <div className="card periodization" style={{ borderLeft: '4px solid var(--color-swim)', marginTop: '1rem' }}>
      <div className="section-head">
        <h3 style={{ margin: 0 }}>Periodización</h3>
        <button className="secondary" onClick={() => setAdding((a) => !a)}>
          {adding ? 'Cerrar' : '+ Bloque'}
        </button>
      </div>

      {chain.length > 0 ? (
        <div className="block-breadcrumb">
          <span className="muted">En {selectedDate}:</span>
          {chain.map((b, i) => (
            <span key={b.id} className="crumb">
              {i > 0 && <span className="sep">›</span>}
              <span className="block-level">{LEVEL_LABEL[b.level]}</span> {b.name}
              <FocusChip focus={b.focus} />
            </span>
          ))}
        </div>
      ) : (
        <p className="muted">Ningún bloque cubre el día seleccionado.</p>
      )}

      {adding && (
        <BlockForm
          blocks={blocks}
          defaultStart={selectedDate}
          onSaved={() => {
            setAdding(false)
            onChanged()
          }}
          onCancel={() => setAdding(false)}
        />
      )}

      {blocks.length > 0 && <ul className="block-tree">{blocks.map(renderBlock)}</ul>}
    </div>
  )
}

function BlockForm({
  blocks,
  defaultStart,
  onSaved,
  onCancel,
}: {
  blocks: Block[]
  defaultStart: string
  onSaved: () => void
  onCancel: () => void
}) {
  const [level, setLevel] = useState<BlockLevel>('MESO')
  const [focus, setFocus] = useState<BlockFocus>('BASE')
  const [name, setName] = useState('')
  const [startDate, setStartDate] = useState(defaultStart)
  const [endDate, setEndDate] = useState(defaultStart)
  const [loadStance, setLoadStance] = useState<LoadStance | ''>('')
  const [parentId, setParentId] = useState<number | ''>('')
  const [note, setNote] = useState('')
  const [busy, setBusy] = useState(false)

  const parents = flatten(blocks).filter((b) => LEVELS.indexOf(b.level) < LEVELS.indexOf(level))

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    if (name.trim() === '') {
      toast.error('El bloque necesita un nombre')
      return
    }
    setBusy(true)
    try {
      await createBlock({
        level,
        focus,
        name: name.trim(),
        startDate,
        endDate,
        loadStance: loadStance === '' ? null : loadStance,
        parentId: parentId === '' ? null : parentId,
        note: note.trim() === '' ? null : note.trim(),
      })
      toast.success('Bloque creado')
      onSaved()
    } catch (err) {
      toast.error((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card form" onSubmit={submit} style={{ marginTop: '0.6rem' }}>
      <div className="session-row">
        <select value={level} onChange={(e) => setLevel(e.target.value as BlockLevel)} title="Nivel">
          {LEVELS.map((l) => (
            <option key={l} value={l}>
              {LEVEL_LABEL[l]}
            </option>
          ))}
        </select>
        <select value={focus} onChange={(e) => setFocus(e.target.value as BlockFocus)} title="Foco">
          {FOCUSES.map((f) => (
            <option key={f} value={f}>
              {FOCUS_LABEL[f]}
            </option>
          ))}
        </select>
        <select
          value={loadStance}
          onChange={(e) => setLoadStance(e.target.value as LoadStance | '')}
          title="Postura de carga"
        >
          <option value="">Carga: —</option>
          <option value="DELOAD">Descarga</option>
          <option value="MAINTAIN">Mantener</option>
          <option value="BUILD">Subir</option>
        </select>
        <select
          value={parentId}
          onChange={(e) => setParentId(e.target.value === '' ? '' : Number(e.target.value))}
          title="Bloque contenedor"
        >
          <option value="">Sin contenedor</option>
          {parents.map((p) => (
            <option key={p.id} value={p.id}>
              {LEVEL_LABEL[p.level]} · {p.name}
            </option>
          ))}
        </select>
      </div>
      <input placeholder="nombre (p. ej. Base 1, Temporada Ironman)" value={name} onChange={(e) => setName(e.target.value)} />
      <div className="session-row">
        <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} title="Inicio" />
        <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} title="Fin" />
      </div>
      <input placeholder="nota (opcional)" value={note} onChange={(e) => setNote(e.target.value)} />
      <div className="actions">
        <button type="submit" disabled={busy}>
          Crear bloque
        </button>
        <button type="button" className="secondary" onClick={onCancel} disabled={busy}>
          Cancelar
        </button>
      </div>
    </form>
  )
}
