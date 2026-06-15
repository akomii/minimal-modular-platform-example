import { ref } from "vue"
import { apiCall } from "./useApi"

export type ModuleStatusValue =
  | "RUNNING"
  | "STOPPED"
  | "NOT_CREATED"
  | "UNKNOWN"

// mirrors the backend CoreAccess enum (lowercase over JSON)
export type CoreAccessValue = "none" | "read" | "write"

export interface ModuleInfo {
  id: string
  version: string
  status: ModuleStatusValue
  coreAccess: CoreAccessValue
  authorized: boolean
}

const modules = ref<ModuleInfo[]>([])

export function useModules() {
  async function list(): Promise<void> {
    const res = await apiCall("GET", "/api/modules")
    if (res.ok && Array.isArray(res.body)) {
      modules.value = res.body as ModuleInfo[]
    }
  }

  async function install(id: string): Promise<void> {
    await apiCall("POST", `/api/modules/${id}/install`)
    await list()
  }

  async function authorize(id: string): Promise<void> {
    await apiCall("POST", `/api/modules/${id}/authorize`)
    await list()
  }

  async function start(id: string): Promise<void> {
    await apiCall("POST", `/api/modules/${id}/start`)
    await list()
  }

  async function stop(id: string): Promise<void> {
    await apiCall("POST", `/api/modules/${id}/stop`)
    await list()
  }

  async function remove(id: string, purge: boolean): Promise<void> {
    await apiCall("DELETE", `/api/modules/${id}?purge=${purge}`)
    await list()
  }

  return { modules, list, install, authorize, start, stop, remove }
}
