// La sesión local es solo un username guardado en el dispositivo.
const KEY = 'ccollector.username'

export function getUsername(): string | null {
  return localStorage.getItem(KEY)
}

export function setUsername(username: string): void {
  localStorage.setItem(KEY, username)
}

export function clearUsername(): void {
  localStorage.removeItem(KEY)
}
