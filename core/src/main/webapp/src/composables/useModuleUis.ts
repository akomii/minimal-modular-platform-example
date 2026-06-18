import { ref } from "vue"
import { fetchModuleUis, type ModuleUi } from "../auth"

// Shared store of the module UI tabs the current user may see. Refreshed on load and after any
// module lifecycle action, so a module's tab appears/disappears without a page reload.
const moduleUis = ref<ModuleUi[]>([])

export function useModuleUis() {
  async function refresh(): Promise<void> {
    moduleUis.value = await fetchModuleUis()
  }

  return { moduleUis, refresh }
}
