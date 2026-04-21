import {defineConfig} from "vite"
import vue from "@vitejs/plugin-vue"
import {dirname, resolve} from "path"
import {fileURLToPath} from "url"

const __filename = fileURLToPath(import.meta.url)
const __dirname = dirname(__filename)

export default defineConfig(({command}) => {
  return {
    plugins: [vue()],
    build: {
      outDir: resolve(__dirname, "dist"),
      emptyOutDir: true
    }
  }
})
