import { ref } from "vue"
import { useToast } from "primevue/usetoast"
import { apiCall, type ApiResponse } from "./useApi"

// mirrors the backend ConfigType enum (lowercase over JSON)
export type ConfigTypeValue = "string" | "number" | "boolean" | "secret"

// mirrors the backend SettingDTO, used for both core settings and module fields
export interface Setting {
  key: string
  label: string
  type: ConfigTypeValue
  required: boolean
  defaultValue: string | null
  value: string | null
}

// Shared stores so live config stays consistent across views — e.g. disabling the database viewer
// here also drops its tab from the header.
const coreSettings = ref<Setting[]>([])
// declared fields per module, keyed by module id (all configurable modules are shown at once)
const moduleConfigs = ref<Record<string, Setting[]>>({})

export function useConfiguration() {
  const toast = useToast()

  async function loadCore(): Promise<void> {
    const res = await apiCall("GET", "/api/config")
    if (res.ok && Array.isArray(res.body)) {
      coreSettings.value = res.body as Setting[]
    }
  }

  // Core changes are in-process and take effect immediately once saved.
  async function saveCore(values: Record<string, string>): Promise<void> {
    const res = await apiCall("PUT", "/api/config", values)
    handle(res, "Settings saved", (body) => {
      if (Array.isArray(body)) {
        coreSettings.value = body as Setting[]
      }
    })
  }

  // Restoring drops the stored overrides; the server replies with the settings now showing their defaults.
  async function resetCore(): Promise<void> {
    const res = await apiCall("DELETE", "/api/config")
    handle(res, "Settings restored to defaults", (body) => {
      if (Array.isArray(body)) {
        coreSettings.value = body as Setting[]
      }
    })
  }

  function setModuleFields(id: string, fields: Setting[]): void {
    moduleConfigs.value = { ...moduleConfigs.value, [id]: fields }
  }

  async function loadModuleConfig(id: string): Promise<void> {
    const res = await apiCall("GET", `/api/modules/${id}/config`)
    setModuleFields(id, res.ok && Array.isArray(res.body) ? (res.body as Setting[]) : [])
  }

  // Saving only persists; the module keeps running with its old config until Apply.
  async function saveModuleConfig(
    id: string,
    values: Record<string, string>
  ): Promise<void> {
    const res = await apiCall("PUT", `/api/modules/${id}/config`, values)
    handle(res, "Configuration saved", (body) => {
      if (Array.isArray(body)) {
        setModuleFields(id, body as Setting[])
      }
    })
  }

  // Like saving, this only persists (here, by dropping overrides); the module keeps running until Apply.
  async function resetModuleConfig(id: string): Promise<void> {
    const res = await apiCall("DELETE", `/api/modules/${id}/config`)
    handle(res, "Configuration restored to defaults", (body) => {
      if (Array.isArray(body)) {
        setModuleFields(id, body as Setting[])
      }
    })
  }

  // Apply recreates the container so the saved config takes effect (a brief restart).
  async function applyModuleConfig(id: string): Promise<void> {
    const res = await apiCall("POST", `/api/modules/${id}/config/apply`)
    handle(res, "Applied — module restarted")
  }

  // Shows a success toast (running the optional state update) or surfaces the server's error message.
  function handle(
    res: ApiResponse,
    success: string,
    onOk?: (body: unknown) => void
  ): void {
    if (res.ok) {
      onOk?.(res.body)
      toast.add({ severity: "success", summary: success, life: 3000 })
    } else {
      toast.add({
        severity: "error",
        summary: "Action failed",
        detail: errorDetail(res),
        life: 6000
      })
    }
  }

  return {
    coreSettings,
    moduleConfigs,
    loadCore,
    saveCore,
    resetCore,
    loadModuleConfig,
    saveModuleConfig,
    resetModuleConfig,
    applyModuleConfig
  }
}

// Pulls the RFC 7807 `detail` from a ProblemDetail body, falling back to the status code.
function errorDetail(res: ApiResponse): string {
  if (res.body && typeof res.body === "object" && "detail" in res.body) {
    return String((res.body as Record<string, unknown>).detail)
  }
  return `Request failed (${res.status})`
}
