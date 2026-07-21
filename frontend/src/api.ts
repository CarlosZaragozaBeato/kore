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

export interface Ingredient {
  name: string
  quantity: number | null
  unit: string | null
}

export interface Recipe {
  id: number
  name: string
  description: string | null
  servings: number | null
  calories: number | null
  protein: number | null
  carbs: number | null
  fat: number | null
  steps: string | null
  createdAt: string
  ingredients: Ingredient[]
}

export interface RecipeRequest {
  name: string
  description: string | null
  servings: number | null
  calories: number | null
  protein: number | null
  carbs: number | null
  fat: number | null
  steps: string | null
  ingredients: Ingredient[]
}

export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK'

export interface Meal {
  date: string
  mealType: MealType
  recipeName: string | null
  notes: string | null
}

export interface DietPlan {
  id: number
  name: string
  startDate: string
  endDate: string | null
  targetCalories: number | null
  targetProtein: number | null
  targetCarbs: number | null
  targetFat: number | null
  notes: string | null
  createdAt: string
  meals: Meal[]
}

export interface DietPlanRequest {
  name: string
  startDate: string
  endDate: string | null
  targetCalories: number | null
  targetProtein: number | null
  targetCarbs: number | null
  targetFat: number | null
  notes: string | null
  meals: Meal[]
}

export function listRecipes(): Promise<Recipe[]> {
  return fetch(`${BASE}/recipes`, { headers: headers(true) }).then((r) => unwrap<Recipe[]>(r))
}

export function createRecipe(req: RecipeRequest): Promise<Recipe> {
  return fetch(`${BASE}/recipes`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<Recipe>(r))
}

export function updateRecipe(id: number, req: RecipeRequest): Promise<Recipe> {
  return fetch(`${BASE}/recipes/${id}`, {
    method: 'PUT',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<Recipe>(r))
}

export function deleteRecipe(id: number): Promise<void> {
  return fetch(`${BASE}/recipes/${id}`, { method: 'DELETE', headers: headers(true) }).then((r) =>
    unwrap<void>(r),
  )
}

export function listDietPlans(): Promise<DietPlan[]> {
  return fetch(`${BASE}/diet-plans`, { headers: headers(true) }).then((r) => unwrap<DietPlan[]>(r))
}

export function createDietPlan(req: DietPlanRequest): Promise<DietPlan> {
  return fetch(`${BASE}/diet-plans`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<DietPlan>(r))
}

export function updateDietPlan(id: number, req: DietPlanRequest): Promise<DietPlan> {
  return fetch(`${BASE}/diet-plans/${id}`, {
    method: 'PUT',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<DietPlan>(r))
}

export function deleteDietPlan(id: number): Promise<void> {
  return fetch(`${BASE}/diet-plans/${id}`, { method: 'DELETE', headers: headers(true) }).then((r) =>
    unwrap<void>(r),
  )
}

export interface Exercise {
  id: number
  name: string
  muscleGroup: string | null
  equipment: string | null
  description: string | null
}

export interface ExerciseRequest {
  name: string
  muscleGroup: string | null
  equipment: string | null
  description: string | null
}

export interface RoutineItem {
  exerciseName: string
  sets: number | null
  reps: number | null
  restSeconds: number | null
  notes: string | null
}

export interface Routine {
  id: number
  name: string
  description: string | null
  createdAt: string
  items: RoutineItem[]
}

export interface RoutineRequest {
  name: string
  description: string | null
  items: RoutineItem[]
}

export interface StrengthSession {
  id: number
  date: string
  routineId: number | null
  routineName: string | null
  notes: string | null
}

export interface StrengthSessionRequest {
  date: string
  routineId: number | null
  notes: string | null
}

export function listExercises(): Promise<Exercise[]> {
  return fetch(`${BASE}/exercises`, { headers: headers(true) }).then((r) => unwrap<Exercise[]>(r))
}

export function createExercise(req: ExerciseRequest): Promise<Exercise> {
  return fetch(`${BASE}/exercises`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<Exercise>(r))
}

export function deleteExercise(id: number): Promise<void> {
  return fetch(`${BASE}/exercises/${id}`, { method: 'DELETE', headers: headers(true) }).then((r) =>
    unwrap<void>(r),
  )
}

export function listRoutines(): Promise<Routine[]> {
  return fetch(`${BASE}/routines`, { headers: headers(true) }).then((r) => unwrap<Routine[]>(r))
}

export function createRoutine(req: RoutineRequest): Promise<Routine> {
  return fetch(`${BASE}/routines`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<Routine>(r))
}

export function updateRoutine(id: number, req: RoutineRequest): Promise<Routine> {
  return fetch(`${BASE}/routines/${id}`, {
    method: 'PUT',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<Routine>(r))
}

export function deleteRoutine(id: number): Promise<void> {
  return fetch(`${BASE}/routines/${id}`, { method: 'DELETE', headers: headers(true) }).then((r) =>
    unwrap<void>(r),
  )
}

export function listStrengthSessions(): Promise<StrengthSession[]> {
  return fetch(`${BASE}/strength-sessions`, { headers: headers(true) }).then((r) =>
    unwrap<StrengthSession[]>(r),
  )
}

export function createStrengthSession(req: StrengthSessionRequest): Promise<StrengthSession> {
  return fetch(`${BASE}/strength-sessions`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<StrengthSession>(r))
}

export function deleteStrengthSession(id: number): Promise<void> {
  return fetch(`${BASE}/strength-sessions/${id}`, { method: 'DELETE', headers: headers(true) }).then(
    (r) => unwrap<void>(r),
  )
}

export interface PlannedSession {
  id: number | null
  date: string
  type: WorkoutType
  targetDistanceMeters: number | null
  targetDurationSeconds: number | null
  description: string | null
  done: boolean
}

export interface Plan {
  id: number
  name: string
  goal: string | null
  startDate: string
  endDate: string | null
  createdAt: string
  sessions: PlannedSession[]
  plannedCount: number
  completedCount: number
  adherencePct: number
}

export interface PlanSessionRequest {
  date: string
  type: WorkoutType
  targetDistanceMeters: number | null
  targetDurationSeconds: number | null
  description: string | null
}

export interface PlanRequest {
  name: string
  goal: string | null
  startDate: string
  endDate: string | null
  sessions: PlanSessionRequest[]
}

export function listPlans(): Promise<Plan[]> {
  return fetch(`${BASE}/plans`, { headers: headers(true) }).then((r) => unwrap<Plan[]>(r))
}

export function createPlan(req: PlanRequest): Promise<Plan> {
  return fetch(`${BASE}/plans`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<Plan>(r))
}

export function updatePlan(id: number, req: PlanRequest): Promise<Plan> {
  return fetch(`${BASE}/plans/${id}`, {
    method: 'PUT',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<Plan>(r))
}

export function deletePlan(id: number): Promise<void> {
  return fetch(`${BASE}/plans/${id}`, {
    method: 'DELETE',
    headers: headers(true),
  }).then((r) => unwrap<void>(r))
}

export interface PeriodSummary {
  label: string
  from: string
  to: string
  workouts: number
  distanceMeters: number
  durationSeconds: number
  avgPaceSecondsPerKm: number | null
  load: number
}

export interface Totals {
  workouts: number
  distanceMeters: number
  durationSeconds: number
  avgPaceSecondsPerKm: number | null
  load: number
}

export interface Dashboard {
  generatedAt: string
  totals: Totals
  weekly: PeriodSummary[]
  monthly: PeriodSummary[]
}

export function getAnalytics(): Promise<Dashboard> {
  return fetch(`${BASE}/analytics/summary`, { headers: headers(true) }).then((r) => {
    if (!r.ok) {
      throw new Error(`Error ${r.status}`)
    }
    return r.json() as Promise<Dashboard>
  })
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
