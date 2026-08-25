import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../auth/AuthContext'
import { LoginPage } from './LoginPage'

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function renderLogin() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
    </MemoryRouter>,
  )
}

afterEach(() => {
  localStorage.clear()
  vi.unstubAllGlobals()
})

describe('LoginPage', () => {
  it('renders email and password fields', () => {
    renderLogin()
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/^password/i)).toBeInTheDocument()
  })

  it('shows field errors and skips the network call on invalid input', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()

    renderLogin()
    await user.type(screen.getByLabelText(/email/i), 'not-an-email')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByText(/valid email/i)).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('submits credentials, stores session, and navigates home on success', async () => {
    const fetchMock = vi.fn(async () =>
      jsonResponse(200, {
        accessToken: 'a1',
        refreshToken: 'r1',
        user: { id: 3, email: 'ok@example.com', fullName: 'Ok', platformRole: 'CANDIDATE' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()

    render(
      <MemoryRouter initialEntries={['/login']}>
        <AuthProvider>
          <LoginPage />
          <div data-testid="home-marker" />
        </AuthProvider>
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText(/email/i), 'ok@example.com')
    await user.type(screen.getByLabelText(/^password/i), 'sup3rSecret!')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => {
      const stored = JSON.parse(localStorage.getItem('hiresense.auth') ?? '{}')
      expect(stored.user?.email).toBe('ok@example.com')
    })
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/v1/auth/login')
  })

  it('surfaces a banner for rejected credentials', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => jsonResponse(401, { detail: 'Invalid email or password' })),
    )
    const user = userEvent.setup()

    renderLogin()
    await user.type(screen.getByLabelText(/email/i), 'bad@example.com')
    await user.type(screen.getByLabelText(/^password/i), 'wrong-pass-1')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent(/invalid email or password/i)
  })
})
