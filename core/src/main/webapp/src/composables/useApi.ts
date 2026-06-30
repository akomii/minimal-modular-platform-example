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
// X-XSRF-TOKEN header) required by the backend's cookie-based CSRF protection. An optional `body`:
// a string is sent raw as text/plain, anything else as JSON.
export async function apiCall(
  method: string,
  url: string,
  body?: unknown
): Promise<ApiResponse> {
  const headers: Record<string, string> = {}
  if (method !== "GET" && method !== "HEAD") {
    headers["X-XSRF-TOKEN"] = getCookie("XSRF-TOKEN") ?? ""
  }
  const init: RequestInit = { method, headers }
  if (body !== undefined) {
    if (typeof body === "string") {
      headers["Content-Type"] = "text/plain"
      init.body = body
    } else {
      headers["Content-Type"] = "application/json"
      init.body = JSON.stringify(body)
    }
  }
  let status = 0
  let ok = false
  let responseBody: unknown = null
  try {
    const res = await fetch(url, init)
    status = res.status
    ok = res.ok
    responseBody = parseBody(await res.text())
  } catch (e) {
    responseBody = e instanceof Error ? e.message : String(e)
  }
  return { method, url, status, ok, body: responseBody }
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

// Pulls the RFC 7807 `detail` from a ProblemDetail body, falling back to the status code.
export function errorDetail(res: ApiResponse): string {
  if (res.body && typeof res.body === "object" && "detail" in res.body) {
    return String((res.body as Record<string, unknown>).detail)
  }
  return `Request failed (${res.status})`
}
