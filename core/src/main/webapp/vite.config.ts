import {defineConfig} from "vite"
import vue from "@vitejs/plugin-vue"

export default defineConfig(() => {
  return {
    plugins: [vue()],
    server: {
      // Proxy the auth + API routes to the core app so the browser stays on a single
      // origin (localhost:5173) and the BFF session cookie just works in dev.
      // changeOrigin must stay false (the Vite default): core reads the Host header to
      // build the OAuth2 redirect_uri, and the IdP has localhost:5173 registered.
      proxy: {
        "/api": "http://localhost:8080",
        "/oauth2": "http://localhost:8080",
        "/login": "http://localhost:8080",
        "/logout": "http://localhost:8080"
      }
    },
    build: {
      outDir: "dist",
      emptyOutDir: true
    }
  }
})
