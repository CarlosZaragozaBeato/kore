// Cliente HTTP mínimo. En dev, /api se proxya al backend (ver vite.config.ts);
// en prod se sirve bajo el mismo origen. VITE_API_BASE permite sobrescribirlo.
const BASE = import.meta.env.VITE_API_BASE ?? '/api/v1'

export interface ResponseDTO<T> {
  success: boolean
  data: T | null
  error: string | null
}

export async function apiGet<T>(path: string): Promise<ResponseDTO<T>> {
  const res = await fetch(`${BASE}${path}`)
  return (await res.json()) as ResponseDTO<T>
}
