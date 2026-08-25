import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider, useAuth } from './AuthContext'
import { ProtectedRoute } from './ProtectedRoute'

function fetchJsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

beforeEach(() => {
  localStorage.clear()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function LoginProbe() {
  const { login, logout, user } = useAuth()
  return (
    <div>
      <span data-testid="user">{user ? user.email : 'anonymous'}</span>
      <button
        type="button"
        onClick={() =>
          act(async () => {
            await login('me@example.com', 'sup3rSecret!')
          })
        }
      >
        do-login
      </button>
      <button
        type="button"
        onClick={() =>
          act(async () => {
            await logout()
          })
        }
      >
        do-logout
      </button>
    </div>
  )
}

describe('AuthContext', () => {
  it('login stores auth and exposes the user; logout clears both', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        fetchJsonResponse(200, {
          accessToken: 'a1',
          refreshToken: 'r1',
          user: { id: 7, email: 'me@example.com', fullName: 'Me', platformRole: 'CANDIDATE' },
        }),
      ),
    )

    render(
      <AuthProvider>
        <LoginProbe />
      </AuthProvider>,
    )

    expect(screen.getByTestId('user').textContent).toBe('anonymous')
    await user.click(screen.getByText('do-login'))
    expect(screen.getByTestId('user').textContent).toBe('me@example.com')

    const stored = JSON.parse(localStorage.getItem('hiresense.auth') ?? '{}')
    expect(stored.accessToken).toBe('a1')

    vi.stubGlobal(
      'fetch',
      vi.fn(async () => fetchJsonResponse(204, null)),
    )
    await user.click(screen.getByText('do-logout'))
    expect(screen.getByTestId('user').textContent).toBe('anonymous')
    expect(localStorage.getItem('hiresense.auth')).toBeNull()
  })

  it('surfaces ApiError when credentials are rejected', async () => {
    const user = userEvent.setup()
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => fetchJsonResponse(401, { detail: 'Invalid email or password' })),
    )

    let capturedMessage = ''
    function CapturingComponent() {
      const { login } = useAuth()
      return (
        <button
          type="button"
          onClick={() =>
            void login('me@example.com', 'wrong-pass').catch((e: Error) => {
              capturedMessage = e.message
            })
          }
        >
          fail-login
        </button>
      )
    }

    render(
      <AuthProvider>
        <CapturingComponent />
      </AuthProvider>,
    )
    await user.click(screen.getByText('fail-login'))
    expect(capturedMessage).toBe('Invalid email or password')
  })
})

describe('ProtectedRoute', () => {
  it('redirects anonymous visitors to login', () => {
    render(
      <MemoryRouter initialEntries={['/private']}>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<div>login-page</div>} />
            <Route
              path="/private"
              element={
                <ProtectedRoute>
                  <div>secret</div>
                </ProtectedRoute>
              }
            />
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    )
    expect(screen.queryByText('secret')).toBeNull()
    expect(screen.getByText('login-page')).toBeInTheDocument()
  })

  it('renders children for authenticated users', () => {
    localStorage.setItem(
      'hiresense.auth',
      JSON.stringify({
        accessToken: 'a',
        refreshToken: 'r',
        user: { id: 1, email: 'x@y.z', fullName: 'X', platformRole: 'CANDIDATE' },
      }),
    )
    render(
      <MemoryRouter initialEntries={['/private']}>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<div>login-page</div>} />
            <Route
              path="/private"
              element={
                <ProtectedRoute>
                  <div>secret</div>
                </ProtectedRoute>
              }
            />
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    )
    expect(screen.getByText('secret')).toBeInTheDocument()
  })
})
