import { ref } from "vue"
import { useToast } from "primevue/usetoast"
import { apiCall, type ApiResponse } from "./useApi"
import { useModuleUis } from "./useModuleUis"

export type ModuleStatusValue =
  | "RUNNING"
  | "STOPPED"
  | "NOT_CREATED"
  | "UNKNOWN"

// mirrors the backend CoreAccess enum (lowercase over JSON)
export type CoreAccessValue = "none" | "read" | "write"

export interface ModuleDependency {
  id: string
  version: string
}

export interface ModuleEndpoint {
  label: string
  method: string
  path: string
}

export interface ModuleInfo {
  id: string
  version: string
  ports: string[]
  status: ModuleStatusValue
  coreAccess: CoreAccessValue
  authorized: boolean
  dependsOn: ModuleDependency[]
  endpoints: ModuleEndpoint[]
}

const modules = ref<ModuleInfo[]>([])

export function useModules() {
  const toast = useToast()

  async function list(): Promise<void> {
    const res = await apiCall("GET", "/api/modules")
    if (res.ok && Array.isArray(res.body)) {
      modules.value = res.body as ModuleInfo[]
    }
  }

  // Runs a mutating action, surfaces the server's message on failure, then refreshes the module
  // list and the visible UI tabs (starting/stopping a module adds/removes its tab).
  async function run(method: string, url: string): Promise<void> {
    const res = await apiCall(method, url)
    if (!res.ok) {
      toast.add({
        severity: "error",
        summary: "Action failed",
        detail: errorDetail(res),
        life: 6000
      })
    }
    await list()
    await useModuleUis().refresh()
  }

  function install(id: string): Promise<void> {
    return run("POST", `/api/modules/${id}/install`)
  }

  function authorize(id: string): Promise<void> {
    return run("POST", `/api/modules/${id}/authorize`)
  }

  function start(id: string): Promise<void> {
    return run("POST", `/api/modules/${id}/start`)
  }

  function stop(id: string): Promise<void> {
    return run("POST", `/api/modules/${id}/stop`)
  }

  function remove(id: string, purge: boolean): Promise<void> {
    return run("DELETE", `/api/modules/${id}?purge=${purge}`)
  }

  return { modules, list, install, authorize, start, stop, remove }
}

// Pulls the RFC 7807 `detail` from a ProblemDetail body, falling back to the status code.
function errorDetail(res: ApiResponse): string {
  if (res.body && typeof res.body === "object" && "detail" in res.body) {
    return String((res.body as Record<string, unknown>).detail)
  }
  return `Request failed (${res.status})`
}
