const API_BASE = import.meta.env.VITE_API_BASE ?? ''
const SESSION_KEY = 'zindigi_consumer_session'

export function sessionToken(): string | null {
  return sessionStorage.getItem(SESSION_KEY)
}

export function saveSession(token: string) {
  sessionStorage.setItem(SESSION_KEY, token)
}

export function clearSession() {
  sessionStorage.removeItem(SESSION_KEY)
}

export async function api<T>(path: string, options: RequestInit = {}, session = sessionToken()): Promise<T> {
  const headers = new Headers(options.headers)
  if (session) headers.set('X-Financing-Session', session)
  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const res = await fetch(`${API_BASE}${path}`, { ...options, headers })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    throw new Error((data as { error?: string }).error || res.statusText || 'Request failed')
  }
  return data as T
}
