import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// The backend only allows the origin http://localhost:5173 (CORS_ALLOWED_ORIGINS),
// so the dev server must always use this port.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: { port: 5173, strictPort: true },
})
