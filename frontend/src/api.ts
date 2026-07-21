import { getUsername } from './session'

// En dev, /api se proxya al backend (ver vite.config.ts).
const BASE = import.meta.env.VITE_API_BASE ?? '/api/v1'

export interface ResponseDTO<T> {
  success: boolean
  data: T | null
  error: string | null
}

export interface CollectorUser {
  id: number
  username: string
  createdAt: string
}

export type WorkoutType = 'RUNNING' | 'STRENGTH' | 'OTHER'
export type WorkoutSource = 'MANUAL' | 'SUUNTO'

export interface Workout {
  id: number
  date: string
  type: WorkoutType
  distanceMeters: number | null
  durationSeconds: number | null
  avgHeartRate: number | null
  perceivedEffort: number | null
  notes: string | null
  source: WorkoutSource
  createdAt: string
  paceSecondsPerKm: number | null
}

export interface WorkoutRequest {
  date: string
  type: WorkoutType
  distanceMeters: number | null
  durationSeconds: number | null
  avgHeartRate: number | null
  perceivedEffort: number | null
  notes: string | null
}

function headers(withUser: boolean): Record<string, string> {
  const h: Record<string, string> = { 'Content-Type': 'application/json' }
  const username = getUsername()
  if (withUser && username) {
    h['X-CCollector-Username'] = username
  }
  return h
}

async function unwrap<T>(res: Response): Promise<T> {
  const body = (await res.json().catch(() => null)) as ResponseDTO<T> | null
  if (!res.ok || !body || !body.success) {
    throw new Error(body?.error ?? `Error ${res.status}`)
  }
  return body.data as T
}

export function login(username: string): Promise<CollectorUser> {
  return fetch(`${BASE}/auth/login`, {
    method: 'POST',
    headers: headers(false),
    body: JSON.stringify({ username }),
  }).then((r) => unwrap<CollectorUser>(r))
}

export function listWorkouts(): Promise<Workout[]> {
  return fetch(`${BASE}/workouts`, { headers: headers(true) }).then((r) => unwrap<Workout[]>(r))
}

export function createWorkout(req: WorkoutRequest): Promise<Workout> {
  return fetch(`${BASE}/workouts`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<Workout>(r))
}

export function updateWorkout(id: number, req: WorkoutRequest): Promise<Workout> {
  return fetch(`${BASE}/workouts/${id}`, {
    method: 'PUT',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<Workout>(r))
}

export function deleteWorkout(id: number): Promise<void> {
  return fetch(`${BASE}/workouts/${id}`, {
    method: 'DELETE',
    headers: headers(true),
  }).then((r) => unwrap<void>(r))
}

// Export devuelve el documento portable crudo (no envuelto en ResponseDTO).
export function exportSession(): Promise<unknown> {
  return fetch(`${BASE}/session/export`, { headers: headers(true) }).then((r) => {
    if (!r.ok) {
      throw new Error(`Error ${r.status}`)
    }
    return r.json()
  })
}

export function importSession(doc: unknown): Promise<CollectorUser> {
  return fetch(`${BASE}/session/import`, {
    method: 'POST',
    headers: headers(false),
    body: JSON.stringify(doc),
  }).then((r) => unwrap<CollectorUser>(r))
}

export interface SuuntoSettings {
  enabled: boolean
  clientId: string | null
  hasClientSecret: boolean
  hasRefreshToken: boolean
  hasSubscriptionKey: boolean
  lastSyncAt: string | null
}

export interface SuuntoSettingsRequest {
  enabled: boolean
  clientId: string
  clientSecret: string | null
  refreshToken: string | null
  subscriptionKey: string | null
}

export interface SyncResult {
  imported: number
  skipped: number
  total: number
  syncedAt: string
}

export function getSuuntoSettings(): Promise<SuuntoSettings> {
  return fetch(`${BASE}/suunto/settings`, { headers: headers(true) }).then((r) =>
    unwrap<SuuntoSettings>(r),
  )
}

export function saveSuuntoSettings(req: SuuntoSettingsRequest): Promise<SuuntoSettings> {
  return fetch(`${BASE}/suunto/settings`, {
    method: 'PUT',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<SuuntoSettings>(r))
}

export function syncSuunto(): Promise<SyncResult> {
  // POST sin cuerpo: no enviamos Content-Type.
  const h: Record<string, string> = {}
  const username = getUsername()
  if (username) {
    h['X-CCollector-Username'] = username
  }
  return fetch(`${BASE}/suunto/sync`, { method: 'POST', headers: h }).then((r) =>
    unwrap<SyncResult>(r),
  )
}
