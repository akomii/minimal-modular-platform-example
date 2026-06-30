import { ref } from "vue"
import { apiCall } from "./useApi"
import { useApiToast } from "./useApiToast"

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
  const { failToast } = useApiToast()

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
      failToast("Role change failed", res)
    }
    await list()
  }

  return { users, roles, list, setRole }
}
