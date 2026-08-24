import { render, screen } from '@testing-library/react'
import App from './App'

describe('App', () => {
  it('renders the product heading and tagline', () => {
    render(<App />)
    expect(screen.getByRole('heading', { level: 1, name: /hiresense/i })).toBeInTheDocument()
    expect(screen.getByText(/resume screening and interview assistant/i)).toBeInTheDocument()
  })
})
