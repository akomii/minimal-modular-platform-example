export interface UserInfo {
  username: string
  roles: string[]
}

export interface ModuleAccess {
  allowed: boolean
  status: number
  count?: number
}

function getCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp("(?:^|; )" + name + "=([^;]*)"))
  return match ? decodeURIComponent(match[1]) : null
}

export async function fetchUser(): Promise<UserInfo | null> {
  const res = await fetch("/api/user", {headers: {Accept: "application/json"}})
  if (res.status === 401) return null
  if (!res.ok) throw new Error(`GET /api/user failed: ${res.status}`)
  return res.json()
}

export async function probeModules(): Promise<ModuleAccess> {
  const res = await fetch("/api/modules", {headers: {Accept: "application/json"}})
  if (res.ok) {
    const modules = await res.json()
    return {allowed: true, status: res.status, count: Array.isArray(modules) ? modules.length : undefined}
  }
  return {allowed: false, status: res.status}
}

export function login(): void {
  // "idp" is the Spring OAuth2 client registration id (provider-agnostic, see application.properties)
  window.location.href = "/oauth2/authorization/idp"
}

export async function logout(): Promise<void> {
  await fetch("/logout", {
    method: "POST",
    headers: {"X-XSRF-TOKEN": getCookie("XSRF-TOKEN") ?? ""}
  })
  window.location.href = "/"
}
