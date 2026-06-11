import { apiCall } from "./composables/useApi"

export interface UserInfo {
  username: string
  roles: string[]
}

export interface ModuleAccess {
  allowed: boolean
  status: number
}

export async function fetchUser(): Promise<UserInfo | null> {
  const res = await fetch("/api/user", {
    headers: { Accept: "application/json" }
  })
  if (res.status === 401) return null
  if (!res.ok) throw new Error(`GET /api/user failed: ${res.status}`)
  return res.json()
}

export async function probeModules(): Promise<ModuleAccess> {
  const res = await fetch("/api/modules", {
    headers: { Accept: "application/json" }
  })
  return { allowed: res.ok, status: res.status }
}

export function login(): void {
  // "idp" is the Spring OAuth2 client registration id (provider-agnostic, see application.properties)
  window.location.href = "/oauth2/authorization/idp"
}

export async function logout(): Promise<void> {
  await apiCall("POST", "/logout")
  window.location.href = "/"
}
