import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ErrorBanner } from './ErrorBanner'

describe('ErrorBanner', () => {
  it('renders a default alert with the default title', () => {
    render(<ErrorBanner />)
    expect(screen.getByRole('alert')).toBeInTheDocument()
    expect(screen.getByText('Something went wrong')).toBeInTheDocument()
  })

  it('renders a custom title and message', () => {
    render(<ErrorBanner title="Upload failed" message="File exceeds 10 MB" />)
    expect(screen.getByText('Upload failed')).toBeInTheDocument()
    expect(screen.getByText('File exceeds 10 MB')).toBeInTheDocument()
  })

  it('omits the retry button when no handler is provided', () => {
    render(<ErrorBanner />)
    expect(screen.queryByRole('button', { name: /retry/i })).not.toBeInTheDocument()
  })

  it('invokes onRetry when the retry button is clicked', async () => {
    const user = userEvent.setup()
    const onRetry = vi.fn()
    render(<ErrorBanner onRetry={onRetry} />)
    await user.click(screen.getByRole('button', { name: /retry/i }))
    expect(onRetry).toHaveBeenCalledTimes(1)
  })
})
