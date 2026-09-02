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

export type WorkoutType = 'RUNNING' | 'CYCLING' | 'SWIMMING' | 'STRENGTH' | 'OTHER'
export type WorkoutSource = 'MANUAL' | 'SUUNTO'

export interface Workout {
  id: number
  date: string
  type: WorkoutType
  distanceMeters: number | null
  durationSeconds: number | null
  avgHeartRate: number | null
  maxHeartRate: number | null
  energyKcal: number | null
  stepCount: number | null
  perceivedEffort: number | null
  notes: string | null
  source: WorkoutSource
  createdAt: string
  paceSecondsPerKm: number | null
  /** Cadencia media (pasos/min), derivada de pasos y duración. */
  avgCadenceSpm: number | null
  /** Longitud de zancada (m/paso), derivada de distancia y pasos. */
  strideLengthMeters: number | null
}

export interface WorkoutRequest {
  date: string
  type: WorkoutType
  distanceMeters: number | null
  durationSeconds: number | null
  avgHeartRate: number | null
  maxHeartRate: number | null
  energyKcal: number | null
  stepCount: number | null
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
  updated: number
  skipped: number
  total: number
  syncedAt: string
}

export interface PurgeResult {
  purged: number
  cutoff: string
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
  ingredientId?: number | null
}

export type BaseUnit = 'GRAM' | 'MILLILITER'

/** Ingrediente del catálogo: valores nutricionales por 100 g/ml. */
export interface CatalogIngredient {
  id: number
  name: string
  baseUnit: BaseUnit
  imageUrl: string | null
  calories: number | null
  protein: number | null
  carbs: number | null
  fat: number | null
  fiber: number | null
  sugars: number | null
  sodium: number | null
}

export interface CatalogIngredientRequest {
  name: string
  baseUnit: BaseUnit
  imageUrl: string | null
  calories: number | null
  protein: number | null
  carbs: number | null
  fat: number | null
  fiber: number | null
  sugars: number | null
  sodium: number | null
}

export interface SeedResult {
  added: number
  skipped: number
  total: number
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

export interface RecommendationContext {
  sessionsLast7: number
  trainingKcalLast7: number
  activityKcalLast7: number | null
  intensity: 'HIGH' | 'MODERATE' | 'REST'
  focus: 'CARB' | 'BALANCED' | 'PROTEIN'
  targetCalories: number | null
  rationale: string
}

export interface RankedRecipe {
  recipeId: number
  name: string
  calories: number | null
  protein: number | null
  carbs: number | null
  fat: number | null
  score: number
  reason: string
}

export interface RecipeRecommendation {
  context: RecommendationContext
  recommendations: RankedRecipe[]
}

export function listRecipes(): Promise<Recipe[]> {
  return fetch(`${BASE}/recipes`, { headers: headers(true) }).then((r) => unwrap<Recipe[]>(r))
}

export function recipesByIngredient(ingredientId: number): Promise<Recipe[]> {
  return fetch(`${BASE}/recipes/by-ingredient/${ingredientId}`, { headers: headers(true) }).then(
    (r) => unwrap<Recipe[]>(r),
  )
}

/** Recomendación contextual: documento crudo (sin envoltorio ResponseDTO). */
export function getRecipeRecommendations(): Promise<RecipeRecommendation> {
  return fetch(`${BASE}/recipes/recommend`, { headers: headers(true) }).then((r) => {
    if (!r.ok) throw new Error(`Error ${r.status}`)
    return r.json() as Promise<RecipeRecommendation>
  })
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

export function listIngredients(): Promise<CatalogIngredient[]> {
  return fetch(`${BASE}/ingredients`, { headers: headers(true) }).then((r) =>
    unwrap<CatalogIngredient[]>(r),
  )
}

export function createIngredient(req: CatalogIngredientRequest): Promise<CatalogIngredient> {
  return fetch(`${BASE}/ingredients`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<CatalogIngredient>(r))
}

export function updateIngredient(
  id: number,
  req: CatalogIngredientRequest,
): Promise<CatalogIngredient> {
  return fetch(`${BASE}/ingredients/${id}`, {
    method: 'PUT',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<CatalogIngredient>(r))
}

export function deleteIngredient(id: number): Promise<void> {
  return fetch(`${BASE}/ingredients/${id}`, { method: 'DELETE', headers: headers(true) }).then((r) =>
    unwrap<void>(r),
  )
}

export function seedIngredients(): Promise<SeedResult> {
  return fetch(`${BASE}/ingredients/seed`, { method: 'POST', headers: headers(true) }).then((r) =>
    unwrap<SeedResult>(r),
  )
}

export type ExerciseCategory = 'WARMUP' | 'STRENGTH' | 'RECOVERY'

export interface Exercise {
  id: number
  name: string
  muscleGroup: string | null
  category: ExerciseCategory | null
  requiresEquipment: boolean | null
  equipment: string | null
  description: string | null
  imageUrl: string | null
  instructions: string | null
  metValue: number | null
}

export interface ExerciseRequest {
  name: string
  muscleGroup: string | null
  category: ExerciseCategory | null
  requiresEquipment: boolean | null
  equipment: string | null
  description: string | null
  imageUrl: string | null
  instructions: string | null
  metValue: number | null
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

export type StrengthStatus = 'PLANNED' | 'DONE'

export interface StrengthSession {
  id: number
  date: string
  routineId: number | null
  routineName: string | null
  notes: string | null
  status: StrengthStatus
}

export interface StrengthSessionRequest {
  date: string
  routineId: number | null
  notes: string | null
  status: StrengthStatus
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

export function updateExercise(id: number, req: ExerciseRequest): Promise<Exercise> {
  return fetch(`${BASE}/exercises/${id}`, {
    method: 'PUT',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<Exercise>(r))
}

export function deleteExercise(id: number): Promise<void> {
  return fetch(`${BASE}/exercises/${id}`, { method: 'DELETE', headers: headers(true) }).then((r) =>
    unwrap<void>(r),
  )
}

export function seedExercises(): Promise<SeedResult> {
  return fetch(`${BASE}/exercises/seed`, { method: 'POST', headers: headers(true) }).then((r) =>
    unwrap<SeedResult>(r),
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

export function updateStrengthSession(
  id: number,
  req: StrengthSessionRequest,
): Promise<StrengthSession> {
  return fetch(`${BASE}/strength-sessions/${id}`, {
    method: 'PUT',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<StrengthSession>(r))
}

export function deleteStrengthSession(id: number): Promise<void> {
  return fetch(`${BASE}/strength-sessions/${id}`, { method: 'DELETE', headers: headers(true) }).then(
    (r) => unwrap<void>(r),
  )
}

export type StepKind = 'WARMUP' | 'INTERVAL' | 'RECOVERY' | 'STEADY' | 'COOLDOWN'

export interface PlannedStep {
  id?: number | null
  orderIndex?: number
  kind: StepKind
  repeat: number
  targetDistanceMeters: number | null
  targetDurationSeconds: number | null
  targetPaceMinSecPerKm: number | null
  targetPaceMaxSecPerKm: number | null
  targetHrMin: number | null
  targetHrMax: number | null
  recoverySeconds: number | null
  note: string | null
}

export type SessionStatus = 'PROPOSED' | 'ACCEPTED' | 'REJECTED'

export interface PlannedSession {
  id: number | null
  planId: number | null
  date: string
  type: WorkoutType
  targetDistanceMeters: number | null
  targetDurationSeconds: number | null
  description: string | null
  done: boolean
  status: SessionStatus
  variantGroup: string | null
  variantLabel: string | null
  steps: PlannedStep[]
}

export interface PlannedSessionRequest {
  date: string
  type: WorkoutType
  targetDistanceMeters: number | null
  targetDurationSeconds: number | null
  description: string | null
  status: SessionStatus | null
  variantGroup: string | null
  variantLabel: string | null
  steps: PlanStepRequest[]
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

export interface PlanStepRequest {
  kind: StepKind
  repeat: number | null
  targetDistanceMeters: number | null
  targetDurationSeconds: number | null
  targetPaceMinSecPerKm: number | null
  targetPaceMaxSecPerKm: number | null
  targetHrMin: number | null
  targetHrMax: number | null
  recoverySeconds: number | null
  note: string | null
}

export interface PlanSessionRequest {
  date: string
  type: WorkoutType
  targetDistanceMeters: number | null
  targetDurationSeconds: number | null
  description: string | null
  steps: PlanStepRequest[]
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

// --- Planificación suelta en el calendario (Fase C) ---

export function listPlannedSessions(): Promise<PlannedSession[]> {
  return fetch(`${BASE}/planned`, { headers: headers(true) }).then((r) => unwrap<PlannedSession[]>(r))
}

export function createPlannedSession(req: PlannedSessionRequest): Promise<PlannedSession> {
  return fetch(`${BASE}/planned`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<PlannedSession>(r))
}

export function updatePlannedSession(id: number, req: PlannedSessionRequest): Promise<PlannedSession> {
  return fetch(`${BASE}/planned/${id}`, {
    method: 'PUT',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<PlannedSession>(r))
}

export function acceptPlannedSession(id: number): Promise<PlannedSession> {
  return fetch(`${BASE}/planned/${id}/accept`, { method: 'POST', headers: headers(true) }).then((r) =>
    unwrap<PlannedSession>(r),
  )
}

export function rejectPlannedSession(id: number): Promise<PlannedSession> {
  return fetch(`${BASE}/planned/${id}/reject`, { method: 'POST', headers: headers(true) }).then((r) =>
    unwrap<PlannedSession>(r),
  )
}

export function deletePlannedSession(id: number): Promise<void> {
  return fetch(`${BASE}/planned/${id}`, { method: 'DELETE', headers: headers(true) }).then((r) => unwrap<void>(r))
}

// --- Comparación planificado vs realizado (Fase D) ---

export type AdherenceBand = 'BELOW' | 'WITHIN' | 'ABOVE' | 'NO_DATA'

export interface ComparisonTarget {
  distanceMeters: number | null
  durationSeconds: number | null
  paceMinSecPerKm: number | null
  paceMaxSecPerKm: number | null
  hrMin: number | null
  hrMax: number | null
  totalReps: number
}

export interface ComparisonActual {
  distanceMeters: number | null
  durationSeconds: number | null
  paceSecondsPerKm: number | null
  avgHeartRate: number | null
  maxHeartRate: number | null
}

export type ComparisonMetricKey = 'distance' | 'duration' | 'pace' | 'hr'

export interface ComparisonMetric {
  key: ComparisonMetricKey
  target: number
  targetLow: number
  targetHigh: number
  actual: number | null
  deltaPct: number | null
  band: AdherenceBand
}

export interface Comparison {
  plannedSessionId: number
  date: string
  type: WorkoutType
  matched: boolean
  workoutId: number | null
  target: ComparisonTarget
  actual: ComparisonActual | null
  metrics: ComparisonMetric[]
}

export function getComparison(id: number): Promise<Comparison> {
  return fetch(`${BASE}/planned/${id}/comparison`, { headers: headers(true) }).then((r) =>
    unwrap<Comparison>(r),
  )
}

// --- Inteligencia de planes: recomendación de carga y variantes (Fase E) ---

export type LoadStance = 'DELOAD' | 'MAINTAIN' | 'BUILD'

export interface PlanRecommendation {
  stance: LoadStance
  label: string
  reason: string
  signals: LoadSignals
}

export interface VariantSuggestion {
  baseSessionId: number
  variantGroup: string
  recommendation: PlanRecommendation
  variants: PlannedSession[]
}

export function getRecommendation(): Promise<PlanRecommendation> {
  return fetch(`${BASE}/planned/recommendation`, { headers: headers(true) }).then((r) =>
    unwrap<PlanRecommendation>(r),
  )
}

export function generateVariants(id: number): Promise<VariantSuggestion> {
  return fetch(`${BASE}/planned/${id}/variants`, { method: 'POST', headers: headers(true) }).then(
    (r) => unwrap<VariantSuggestion>(r),
  )
}

// --- Periodización larga: bloques macro/meso/micro (Fase F) ---

export type BlockLevel = 'MACRO' | 'MESO' | 'MICRO'
export type BlockFocus = 'BASE' | 'BUILD' | 'PEAK' | 'TAPER' | 'RECOVERY' | 'RACE' | 'GENERAL'

export interface Block {
  id: number
  parentId: number | null
  level: BlockLevel
  focus: BlockFocus
  name: string
  startDate: string
  endDate: string
  loadStance: LoadStance | null
  note: string | null
  children: Block[]
}

export interface BlockRequest {
  parentId?: number | null
  level: BlockLevel
  focus?: BlockFocus
  name: string
  startDate: string
  endDate: string
  loadStance?: LoadStance | null
  note?: string | null
  children?: BlockRequest[]
}

export interface BlockSummary {
  blockId: number
  startDate: string
  endDate: string
  weeks: number
  plannedSessions: number
  completedWorkouts: number
  plannedDistanceMeters: number
  completedDistanceMeters: number
  adherencePct: number | null
}

export function listBlocks(): Promise<Block[]> {
  return fetch(`${BASE}/blocks`, { headers: headers(true) }).then((r) => unwrap<Block[]>(r))
}

export function createBlock(req: BlockRequest): Promise<Block> {
  return fetch(`${BASE}/blocks`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<Block>(r))
}

export function updateBlock(id: number, req: BlockRequest): Promise<Block> {
  return fetch(`${BASE}/blocks/${id}`, {
    method: 'PUT',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<Block>(r))
}

export function deleteBlock(id: number): Promise<void> {
  return fetch(`${BASE}/blocks/${id}`, { method: 'DELETE', headers: headers(true) }).then((r) =>
    unwrap<void>(r),
  )
}

export function getBlockSummary(id: number): Promise<BlockSummary> {
  return fetch(`${BASE}/blocks/${id}/summary`, { headers: headers(true) }).then((r) =>
    unwrap<BlockSummary>(r),
  )
}

// --- Contexto de entrenamiento por rango para entregar a un agente ---

export interface TrainingContext {
  schemaVersion: number
  generatedAt: string
  username: string
  from: string
  to: string
  guidance: {
    purpose: string
    units: string
    returnFormat: string
    workoutTypes: string[]
    stepKinds: string[]
  }
  summary: {
    workoutCount: number
    totalDistanceMeters: number
    totalDurationSeconds: number
    plannedCount: number
    load: LoadSignals
  }
  workouts: unknown[]
  planned: unknown[]
  blocks: unknown[]
}

export function getTrainingContext(from: string, to: string): Promise<TrainingContext> {
  const q = `?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`
  return fetch(`${BASE}/context/training${q}`, { headers: headers(true) }).then((r) =>
    unwrap<TrainingContext>(r),
  )
}

export type RaceGoal = 'MARATHON' | 'HALF_MARATHON' | 'TEN_K' | 'FIVE_K' | 'GENERAL'
export type Level = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'

export interface SessionTemplate {
  id: number
  name: string
  discipline: WorkoutType
  goal: RaceGoal | null
  level: Level | null
  targetDistanceMeters: number | null
  targetDurationSeconds: number | null
  structure: string | null
  notes: string | null
}

export interface SessionTemplateRequest {
  name: string
  discipline: WorkoutType
  goal: RaceGoal | null
  level: Level | null
  targetDistanceMeters: number | null
  targetDurationSeconds: number | null
  structure: string | null
  notes: string | null
}

export function listSessionTemplates(): Promise<SessionTemplate[]> {
  return fetch(`${BASE}/session-templates`, { headers: headers(true) }).then((r) =>
    unwrap<SessionTemplate[]>(r),
  )
}

export function createSessionTemplate(req: SessionTemplateRequest): Promise<SessionTemplate> {
  return fetch(`${BASE}/session-templates`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<SessionTemplate>(r))
}

export function deleteSessionTemplate(id: number): Promise<void> {
  return fetch(`${BASE}/session-templates/${id}`, { method: 'DELETE', headers: headers(true) }).then(
    (r) => unwrap<void>(r),
  )
}

export function seedSessionTemplates(): Promise<SeedResult> {
  return fetch(`${BASE}/session-templates/seed`, { method: 'POST', headers: headers(true) }).then(
    (r) => unwrap<SeedResult>(r),
  )
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

export interface CorrelationWeekPoint {
  label: string
  weekStart: string
  load: number | null
  km: number | null
  avgPaceSecondsPerKm: number | null
  consumedKcal: number | null
  burnedKcal: number | null
  balanceKcal: number | null
  steps: number | null
  weightKg: number | null
}

export interface Correlation {
  aLabel: string
  bLabel: string
  r: number | null
  n: number
  strength: string
  direction: string
  interpretation: string
}

export interface CorrelationReport {
  generatedAt: string
  weeks: number
  series: CorrelationWeekPoint[]
  correlations: Correlation[]
  note: string
}

export function getCorrelations(): Promise<CorrelationReport> {
  return fetch(`${BASE}/analytics/correlations`, { headers: headers(true) }).then((r) => {
    if (!r.ok) {
      throw new Error(`Error ${r.status}`)
    }
    return r.json() as Promise<CorrelationReport>
  })
}

export interface WeightEntry {
  id: number
  date: string
  weightKg: number
}

export interface WeightEntryRequest {
  date: string
  weightKg: number
}

export interface WeightGoal {
  minKg: number | null
  maxKg: number | null
  maintenanceKcal: number | null
}

export function listWeight(): Promise<WeightEntry[]> {
  return fetch(`${BASE}/weight`, { headers: headers(true) }).then((r) => unwrap<WeightEntry[]>(r))
}

export function upsertWeight(req: WeightEntryRequest): Promise<WeightEntry> {
  return fetch(`${BASE}/weight`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<WeightEntry>(r))
}

export function deleteWeight(id: number): Promise<void> {
  return fetch(`${BASE}/weight/${id}`, { method: 'DELETE', headers: headers(true) }).then((r) =>
    unwrap<void>(r),
  )
}

export function getWeightGoal(): Promise<WeightGoal> {
  return fetch(`${BASE}/weight/goal`, { headers: headers(true) }).then((r) => unwrap<WeightGoal>(r))
}

export interface DailyActivity {
  id: number
  date: string
  steps: number | null
  burnedKcal: number | null
}

export interface DailyActivityRequest {
  date: string
  steps: number | null
  burnedKcal: number | null
}

export function listDailyActivity(): Promise<DailyActivity[]> {
  return fetch(`${BASE}/activity/daily`, { headers: headers(true) }).then((r) =>
    unwrap<DailyActivity[]>(r),
  )
}

export function upsertDailyActivity(req: DailyActivityRequest): Promise<DailyActivity> {
  return fetch(`${BASE}/activity/daily`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<DailyActivity>(r))
}

export function deleteDailyActivity(id: number): Promise<void> {
  return fetch(`${BASE}/activity/daily/${id}`, { method: 'DELETE', headers: headers(true) }).then(
    (r) => unwrap<void>(r),
  )
}

export function setWeightGoal(req: WeightGoal): Promise<WeightGoal> {
  return fetch(`${BASE}/weight/goal`, {
    method: 'PUT',
    headers: headers(true),
    body: JSON.stringify(req),
  }).then((r) => unwrap<WeightGoal>(r))
}

export type EnergyState = 'DEFICIT' | 'BALANCED' | 'SURPLUS' | 'UNKNOWN'

export interface EnergyDay {
  date: string
  consumedKcal: number
  burnedKcal: number
  balanceKcal: number
  state: EnergyState
}

export interface EnergyWeek {
  label: string
  from: string
  to: string
  consumedKcal: number
  burnedKcal: number
  balanceKcal: number
  state: EnergyState
}

export interface EnergySummary {
  generatedAt: string
  maintenanceKcal: number | null
  days: EnergyDay[]
  weeks: EnergyWeek[]
}

export function getEnergy(): Promise<EnergySummary> {
  return fetch(`${BASE}/energy/summary`, { headers: headers(true) }).then((r) => {
    if (!r.ok) {
      throw new Error(`Error ${r.status}`)
    }
    return r.json() as Promise<EnergySummary>
  })
}

export type SignalState = 'OK' | 'WARN' | 'RISK' | 'UNKNOWN'

export interface Breakdown {
  key: string
  workouts: number
  distanceMeters: number
  durationSeconds: number
  load: number
}

export interface DaySummary {
  date: string
  distanceMeters: number
  durationSeconds: number
  load: number
}

export interface RangeStats {
  label: string
  from: string
  to: string
  workouts: number
  distanceMeters: number
  durationSeconds: number
  load: number
  days: DaySummary[]
  byDiscipline: Breakdown[]
  bySource: Breakdown[]
}

export interface DailyDelta {
  dayIndex: number
  currentDate: string
  previousDate: string
  currentLoad: number
  previousLoad: number
  deltaLoad: number
  cumulativeDeltaLoad: number
}

export interface LoadSignals {
  acuteLoad: number
  chronicLoad: number
  acwr: number | null
  acwrState: SignalState
  monotony: number | null
  monotonyState: SignalState
  rampPct: number | null
  rampState: SignalState
}

export interface LoadComparison {
  generatedAt: string
  current: RangeStats
  previous: RangeStats
  dailyDeltas: DailyDelta[]
  signals: LoadSignals
}

export interface CompareRanges {
  currentStart?: string
  currentEnd?: string
  previousStart?: string
  previousEnd?: string
}

export function getCompare(ranges: CompareRanges = {}): Promise<LoadComparison> {
  const qs = new URLSearchParams()
  for (const [k, v] of Object.entries(ranges)) {
    if (v) qs.set(k, v)
  }
  const suffix = qs.toString() ? `?${qs}` : ''
  return fetch(`${BASE}/analytics/compare${suffix}`, { headers: headers(true) }).then((r) => {
    if (!r.ok) {
      throw new Error(`Error ${r.status}`)
    }
    return r.json() as Promise<LoadComparison>
  })
}

// Kore Data Language: import/export de recursos sueltos o en lote.
export interface KdlItem {
  koreType: string
  schemaVersion: number
  payload: unknown
}

export interface KdlDocument {
  kore: string
  kdlVersion: number
  items: KdlItem[]
}

export interface KdlImportResult {
  imported: number
  skipped: number
  errors: string[]
  byType: Record<string, number>
}

export function exportKdl(types: string[] = []): Promise<KdlDocument> {
  const suffix = types.length ? `?types=${types.join(',')}` : ''
  return fetch(`${BASE}/kdl/export${suffix}`, { headers: headers(true) }).then((r) => {
    if (!r.ok) {
      throw new Error(`Error ${r.status}`)
    }
    return r.json() as Promise<KdlDocument>
  })
}

export function importKdl(doc: unknown): Promise<KdlImportResult> {
  return fetch(`${BASE}/kdl/import`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify(doc),
  }).then((r) => unwrap<KdlImportResult>(r))
}

/**
 * Import por sección: `body` es uno o varios recursos de `type` (payload suelto,
 * array o documento KDL). Ej.: importar un plan completo generado por una IA.
 */
export function importKdlAs(type: string, body: unknown): Promise<KdlImportResult> {
  return fetch(`${BASE}/kdl/import/${type}`, {
    method: 'POST',
    headers: headers(true),
    body: JSON.stringify(body),
  }).then((r) => unwrap<KdlImportResult>(r))
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

/** Retención mínima: borra los entrenos de Suunto anteriores a la ventana. */
export function purgeSuunto(): Promise<PurgeResult> {
  const h: Record<string, string> = {}
  const username = getUsername()
  if (username) {
    h['X-CCollector-Username'] = username
  }
  return fetch(`${BASE}/suunto/purge`, { method: 'POST', headers: h }).then((r) =>
    unwrap<PurgeResult>(r),
  )
}
