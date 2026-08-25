import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, api, clearStoredAuth, loadStoredAuth, saveStoredAuth } from './api'

type FetchInit = { headers?: Record<string, string>; body?: string; method?: string }

function fetchMock(handler: (_url: string, _init?: FetchInit) => Response | Promise<Response>) {
  const fn = vi.fn(async (url: string, init?: FetchInit) => handler(url, init))
  vi.stubGlobal('fetch', fn)
  return fn
}

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

const auth = {
  accessToken: 'access-1',
  refreshToken: 'refresh-1',
  user: { id: 1, email: 'a@b.c', fullName: 'A B', platformRole: 'CANDIDATE' },
}

beforeEach(() => {
  localStorage.clear()
  saveStoredAuth(auth)
})

afterEach(() => {
  vi.unstubAllGlobals()
  clearStoredAuth()
})

describe('auth storage', () => {
  it('round-trips and clears stored auth', () => {
    expect(loadStoredAuth()).toEqual(auth)
    clearStoredAuth()
    expect(loadStoredAuth()).toBeNull()
  })
})

describe('api request', () => {
  it('sends bearer token and parses json response', async () => {
    const fetchFn = fetchMock(() => jsonResponse(200, { hello: 'world' }))

    const result = await api<{ hello: string }>('/api/v1/me')

    expect(result).toEqual({ hello: 'world' })
    const [, init] = fetchFn.mock.calls[0]
    expect((init?.headers as Record<string, string>).Authorization).toBe('Bearer access-1')
  })

  it('throws ApiError with problem detail and field errors', async () => {
    fetchMock(() =>
      jsonResponse(400, {
        detail: 'Request validation failed',
        errors: { email: 'must be valid' },
      }),
    )

    const error = await api('/api/v1/auth/register/candidate', {
      method: 'POST',
      body: {},
    }).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(400)
    expect((error as ApiError).message).toBe('Request validation failed')
    expect((error as ApiError).fieldErrors).toEqual({ email: 'must be valid' })
  })

  it('refreshes once on 401 and retries the original request', async () => {
    const fetchFn = fetchMock(() => jsonResponse(500, {}))
    let callsToJobs = 0
    fetchFn.mockImplementation(async (url: string, init?: FetchInit) => {
      if (url.endsWith('/api/v1/auth/token/refresh')) {
        return jsonResponse(200, { accessToken: 'access-2', refreshToken: 'refresh-2' })
      }
      if (url.endsWith('/api/v1/jobs')) {
        callsToJobs += 1
        const header = (init?.headers as Record<string, string>).Authorization
        if (header === 'Bearer access-1') {
          return jsonResponse(401, {})
        }
        return jsonResponse(200, { jobs: [] })
      }
      return jsonResponse(500, {})
    })

    const result = await api<{ jobs: unknown[] }>('/api/v1/jobs')

    expect(result).toEqual({ jobs: [] })
    expect(callsToJobs).toBe(2)
    expect(loadStoredAuth()?.accessToken).toBe('access-2')
  })

  it('clears storage when refresh fails and surfaces the original 401', async () => {
    fetchMock((url) => {
      if (url.endsWith('/api/v1/auth/token/refresh')) {
        return jsonResponse(401, {})
      }
      return jsonResponse(401, { detail: 'Authentication required' })
    })

    const error = await api('/api/v1/me').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(401)
    expect(loadStoredAuth()).toBeNull()
  })

  it('does not attempt refresh for auth endpoints themselves', async () => {
    const fetchFn = fetchMock(() => jsonResponse(401, { detail: 'Invalid email or password' }))

    const error = await api('/api/v1/auth/login', { method: 'POST', body: {} }).catch(
      (e: unknown) => e,
    )

    expect((error as ApiError).status).toBe(401)
    const refreshCalls = fetchFn.mock.calls.filter(([url]) =>
      String(url).endsWith('/token/refresh'),
    )
    expect(refreshCalls).toHaveLength(0)
  })
})
