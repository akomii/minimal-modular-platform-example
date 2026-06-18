import { ref } from "vue"
import { useToast } from "primevue/usetoast"
import { apiCall, type ApiResponse } from "./useApi"

export interface PlatformUser {
  id: string
  username: string
  email: string | null
  roles: string[]
}

export interface AssignableRole {
  id: string
  label: string
  module: string | null
}

const users = ref<PlatformUser[]>([])
const roles = ref<AssignableRole[]>([])

export function useUsers() {
  const toast = useToast()

  async function list(): Promise<void> {
    const [usersRes, rolesRes] = await Promise.all([
      apiCall("GET", "/api/users"),
      apiCall("GET", "/api/roles")
    ])
    if (rolesRes.ok && Array.isArray(rolesRes.body)) {
      roles.value = rolesRes.body as AssignableRole[]
    }
    if (usersRes.ok && Array.isArray(usersRes.body)) {
      users.value = usersRes.body as PlatformUser[]
    }
  }

  // Grants/revokes one role, surfaces the server's message on failure, then refreshes so the matrix matches the server.
  async function setRole(
    userId: string,
    roleId: string,
    assigned: boolean
  ): Promise<void> {
    const res = await apiCall(
      assigned ? "PUT" : "DELETE",
      `/api/users/${userId}/roles/${encodeURIComponent(roleId)}`
    )
    if (!res.ok) {
      toast.add({
        severity: "error",
        summary: "Role change failed",
        detail: errorDetail(res),
        life: 6000
      })
    }
    await list()
  }

  return { users, roles, list, setRole }
}

// Pulls the RFC 7807 `detail` from a ProblemDetail body, falling back to the status code.
function errorDetail(res: ApiResponse): string {
  if (res.body && typeof res.body === "object" && "detail" in res.body) {
    return String((res.body as Record<string, unknown>).detail)
  }
  return `Request failed (${res.status})`
}
