import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// El frontend habla siempre con /api (misma-origin en dev vía proxy),
// así el cliente no necesita conocer la URL del backend.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
