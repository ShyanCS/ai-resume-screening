import { logger } from './logger'

export interface StoredUser {
  id: number
  email: string
  fullName: string
  platformRole: string
}

export interface StoredAuth {
  accessToken: string
  refreshToken: string
  user: StoredUser
}

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors?: Record<string, string>

  constructor(status: number, message: string, fieldErrors?: Record<string, string>) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const STORAGE_KEY = 'hiresense.auth'

export function loadStoredAuth(): StoredAuth | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as StoredAuth) : null
  } catch {
    return null
  }
}

export function saveStoredAuth(auth: StoredAuth): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(auth))
}

export function clearStoredAuth(): void {
  localStorage.removeItem(STORAGE_KEY)
}

let refreshInFlight: Promise<boolean> | null = null

async function tryRefresh(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = doRefresh().finally(() => {
      refreshInFlight = null
    })
  }
  return refreshInFlight
}

async function doRefresh(): Promise<boolean> {
  const auth = loadStoredAuth()
  if (!auth?.refreshToken) {
    return false
  }
  try {
    const response = await fetch(`${BASE_URL}/api/v1/auth/token/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: auth.refreshToken }),
    })
    if (!response.ok) {
      clearStoredAuth()
      return false
    }
    const tokens = (await response.json()) as { accessToken: string; refreshToken: string }
    saveStoredAuth({ ...auth, accessToken: tokens.accessToken, refreshToken: tokens.refreshToken })
    return true
  } catch (error) {
    logger.warn('api', 'token refresh failed', error)
    return false
  }
}

interface RequestOptions {
  method?: string
  body?: unknown
}

async function request<T>(
  path: string,
  options: RequestOptions,
  allowRefreshRetry: boolean,
): Promise<T> {
  const auth = loadStoredAuth()
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (auth?.accessToken && !path.startsWith('/api/v1/auth/')) {
    headers.Authorization = `Bearer ${auth.accessToken}`
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    method: options.method ?? 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  })

  if (response.status === 401 && allowRefreshRetry && !path.startsWith('/api/v1/auth/')) {
    if (await tryRefresh()) {
      return request<T>(path, options, false)
    }
  }

  if (!response.ok) {
    let detail = `Request failed with status ${response.status}`
    let fieldErrors: Record<string, string> | undefined
    try {
      const problem = (await response.json()) as {
        detail?: string
        errors?: Record<string, string>
      }
      if (problem.detail) {
        detail = problem.detail
      }
      if (problem.errors) {
        fieldErrors = problem.errors
      }
    } catch {
      logger.warn('api', `non-json error body for ${path}`)
    }
    throw new ApiError(response.status, detail, fieldErrors)
  }

  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  return request<T>(path, options, true)
}
