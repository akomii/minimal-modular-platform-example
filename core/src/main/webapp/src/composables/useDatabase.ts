import { ref } from "vue"
import { useToast } from "primevue/usetoast"
import { apiCall, type ApiResponse } from "./useApi"

export interface DbSchema {
  name: string
  tables: string[]
}

export interface DbTableData {
  schema: string
  table: string
  columns: string[]
  rows: Record<string, unknown>[]
  totalRows: number
  limit: number
  offset: number
}

export function useDatabase() {
  const toast = useToast()
  const schemas = ref<DbSchema[]>([])
  const current = ref<DbTableData | null>(null)

  async function loadSchemas(): Promise<void> {
    const res = await apiCall("GET", "/api/db/schemas")
    if (res.ok && Array.isArray(res.body)) {
      schemas.value = res.body as DbSchema[]
    } else {
      fail(res)
    }
  }

  async function loadTable(
    schema: string,
    table: string,
    limit: number,
    offset: number
  ): Promise<void> {
    const res = await apiCall(
      "GET",
      `/api/db/tables/${schema}/${table}?limit=${limit}&offset=${offset}`
    )
    if (res.ok && res.body && typeof res.body === "object") {
      current.value = res.body as DbTableData
    } else {
      fail(res)
    }
  }

  function fail(res: ApiResponse): void {
    toast.add({
      severity: "error",
      summary: "Database viewer",
      detail: errorDetail(res),
      life: 6000
    })
  }

  return { schemas, current, loadSchemas, loadTable }
}

// Pulls the RFC 7807 `detail` from a ProblemDetail body, falling back to the status code.
function errorDetail(res: ApiResponse): string {
  if (res.body && typeof res.body === "object" && "detail" in res.body) {
    return String((res.body as Record<string, unknown>).detail)
  }
  return `Request failed (${res.status})`
}
