export interface ApiResponse {
  method: string
  url: string
  status: number
  ok: boolean
  body: unknown
}

export type Severity = "success" | "warn" | "danger" | "secondary"

// Color-codes an HTTP status: 2xx success, 4xx warn, 5xx danger, else neutral.
export function statusSeverity(status: number): Severity {
  if (status >= 200 && status < 300) {
    return "success"
  }
  if (status >= 400 && status < 500) {
    return "warn"
  }
  if (status >= 500) {
    return "danger"
  }
  return "secondary"
}

function getCookie(name: string): string | null {
  const match = document.cookie.match(
    new RegExp("(?:^|; )" + name + "=([^;]*)")
  )
  return match ? decodeURIComponent(match[1]) : null
}

// fetch wrapper that NEVER throws on 4xx/5xx — it captures status + body so callers can react to
// the raw server response. Mutating requests echo the CSRF token (XSRF-TOKEN cookie ->
// X-XSRF-TOKEN header) required by the backend's cookie-based CSRF protection.
export async function apiCall(
  method: string,
  url: string
): Promise<ApiResponse> {
  const headers: Record<string, string> = {}
  if (method !== "GET" && method !== "HEAD") {
    headers["X-XSRF-TOKEN"] = getCookie("XSRF-TOKEN") ?? ""
  }
  let status = 0
  let ok = false
  let body: unknown = null
  try {
    const res = await fetch(url, { method, headers })
    status = res.status
    ok = res.ok
    body = parseBody(await res.text())
  } catch (e) {
    body = e instanceof Error ? e.message : String(e)
  }
  return { method, url, status, ok, body }
}

function parseBody(text: string): unknown {
  if (!text) {
    return ""
  }
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}
