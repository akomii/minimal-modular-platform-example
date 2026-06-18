import { apiCall } from "./composables/useApi"

export interface UserInfo {
  username: string
  roles: string[]
}

export interface ModuleUi {
  moduleId: string
  label: string
  url: string
}

export async function fetchUser(): Promise<UserInfo | null> {
  const res = await fetch("/api/user", {
    headers: { Accept: "application/json" }
  })
  if (res.status === 401) return null
  if (!res.ok) throw new Error(`GET /api/user failed: ${res.status}`)
  return res.json()
}

// The module UI tabs the current user may see (empty for a user with no module roles).
export async function fetchModuleUis(): Promise<ModuleUi[]> {
  const res = await fetch("/api/ui", {
    headers: { Accept: "application/json" }
  })
  return res.ok ? res.json() : []
}

export function login(): void {
  // "idp" is the Spring OAuth2 client registration id (provider-agnostic, see application.properties)
  window.location.href = "/oauth2/authorization/idp"
}

export async function logout(): Promise<void> {
  // the backend returns the IdP end-session URL; navigating to it ends the Keycloak SSO session too, then Keycloak redirects back to the app
  const res = await apiCall("POST", "/logout")
  window.location.href =
    typeof res.body === "string" && res.body ? res.body : "/"
}
