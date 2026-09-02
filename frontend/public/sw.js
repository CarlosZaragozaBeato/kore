// Service worker mínimo: hace la app instalable y da un shell offline.
// La API (/api) y los endpoints de infra (/q) van SIEMPRE a red — nunca se
// cachean, para no servir datos obsoletos.
const CACHE = 'kore-shell-v1'

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE).then((c) => c.add('/')).then(() => self.skipWaiting()))
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  )
})

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url)
  if (url.pathname.startsWith('/api') || url.pathname.startsWith('/q')) {
    return // red directa
  }
  if (event.request.mode === 'navigate') {
    event.respondWith(fetch(event.request).catch(() => caches.match('/')))
  }
})
