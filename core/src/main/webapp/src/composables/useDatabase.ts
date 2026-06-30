import { ref } from "vue"
import { apiCall } from "./useApi"
import { useApiToast } from "./useApiToast"

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
  const { failToast } = useApiToast()
  const schemas = ref<DbSchema[]>([])
  const current = ref<DbTableData | null>(null)

  async function loadSchemas(): Promise<void> {
    const res = await apiCall("GET", "/api/db/schemas")
    if (res.ok && Array.isArray(res.body)) {
      schemas.value = res.body as DbSchema[]
    } else {
      failToast("Database viewer", res)
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
      failToast("Database viewer", res)
    }
  }

  return { schemas, current, loadSchemas, loadTable }
}
