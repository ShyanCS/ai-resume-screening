import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '../auth/AuthContext'
import { MyResumesPage } from './MyResumesPage'

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function seedAuth() {
  localStorage.setItem(
    'hiresense.auth',
    JSON.stringify({
      accessToken: 'a',
      refreshToken: 'r',
      user: { id: 1, email: 'x@y.z', fullName: 'X', platformRole: 'CANDIDATE' },
    }),
  )
}

function renderPage() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <MyResumesPage />
      </AuthProvider>
    </MemoryRouter>,
  )
}

afterEach(() => {
  vi.unstubAllGlobals()
  localStorage.clear()
})

interface RecordedCall {
  url: string
  method?: string
  body?: unknown
}

describe('MyResumesPage', () => {
  it('shows empty state when no resumes exist', async () => {
    seedAuth()
    const fetchMock = vi.fn(async () => jsonResponse(200, []))
    vi.stubGlobal('fetch', fetchMock)

    renderPage()

    expect(await screen.findByText(/no resumes uploaded yet/i)).toBeInTheDocument()
  })

  it('uploads a file via multipart and lists the returned resume', async () => {
    seedAuth()
    const user = userEvent.setup()
    const calls: RecordedCall[] = []

    const fetchMock = vi.fn(async (url: string, init?: { method?: string; body?: unknown }) => {
      calls.push({ url, method: init?.method, body: init?.body })
      if (init?.method === 'POST') {
        return jsonResponse(201, {
          id: 9,
          originalFilename: 'cv.pdf',
          fileSizeBytes: 2048,
          status: 'PARSED',
          uploadedAt: new Date().toISOString(),
        })
      }
      return jsonResponse(200, [
        {
          id: 9,
          originalFilename: 'cv.pdf',
          fileSizeBytes: 2048,
          status: 'PARSED',
          uploadedAt: new Date().toISOString(),
        },
      ])
    })
    vi.stubGlobal('fetch', fetchMock)

    renderPage()
    await screen.findByText(/no resumes uploaded yet/i)

    const file = new File(['%PDF-1.4 test'], 'cv.pdf', { type: 'application/pdf' })
    await user.upload(screen.getByTestId('upload-button').querySelector('input')!, file)

    await waitFor(() => {
      expect(screen.getByTestId('resume-row-9')).toBeInTheDocument()
      expect(screen.getByText('cv.pdf')).toBeInTheDocument()
      expect(screen.getByText('PARSED')).toBeInTheDocument()
    })

    const uploadCall = calls.find((c) => c.method === 'POST')
    expect(uploadCall).toBeTruthy()
    expect(uploadCall!.body).toBeInstanceOf(FormData)
  }, 20000)
})
