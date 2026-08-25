import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { CandidateSignupPage } from './CandidateSignupPage'

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

afterEach(() => {
  vi.unstubAllGlobals()
  localStorage.clear()
})

describe('CandidateSignupPage', () => {
  it('submits valid data to the signup endpoint and shows confirmation', async () => {
    const fetchMock = vi.fn(async (_url: string, _init?: { body?: string }) =>
      jsonResponse(201, { id: 1 }),
    )
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <CandidateSignupPage />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText(/full name/i), 'New Candidate')
    await user.type(screen.getByLabelText(/^email/i), 'new@example.com')
    await user.type(screen.getByLabelText(/^password/i), 'sup3rSecret!')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByRole('status')).toHaveTextContent(/account created/i)

    const [url, init] = fetchMock.mock.calls[0]
    expect(String(url)).toContain('/api/v1/auth/register/candidate')
    expect(JSON.parse(String(init?.body))).toEqual({
      fullName: 'New Candidate',
      email: 'new@example.com',
      password: 'sup3rSecret!',
    })
  })

  it('maps server conflict errors onto the form', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => jsonResponse(409, { detail: 'Email already registered' })),
    )
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <CandidateSignupPage />
      </MemoryRouter>,
    )

    await user.type(screen.getByLabelText(/full name/i), 'Dup User')
    await user.type(screen.getByLabelText(/^email/i), 'dup@example.com')
    await user.type(screen.getByLabelText(/^password/i), 'sup3rSecret!')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    await waitFor(() => {
      expect(screen.getByText(/email already registered/i)).toBeInTheDocument()
    })
  })

  it('blocks submission when validation fails', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <CandidateSignupPage />
      </MemoryRouter>,
    )

    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByText(/full name is required/i)).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })
})
