// Tema de la app. Kore nace en oscuro; el claro es opcional y se recuerda.
const KEY = 'kore.theme'

export type Theme = 'dark' | 'light'

export function getTheme(): Theme {
  try {
    return localStorage.getItem(KEY) === 'light' ? 'light' : 'dark'
  } catch {
    return 'dark'
  }
}

export function applyTheme(theme: Theme): void {
  document.documentElement.dataset.theme = theme
}

export function setTheme(theme: Theme): void {
  try {
    localStorage.setItem(KEY, theme)
  } catch {
    // almacenamiento no disponible: aplica igualmente en memoria
  }
  applyTheme(theme)
}
