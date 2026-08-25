import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import App from './App'

describe('App', () => {
  it('routes the login path to the sign-in screen', () => {
    render(
      <MemoryRouter initialEntries={['/login']}>
        <App />
      </MemoryRouter>,
    )
    expect(screen.getByRole('heading', { name: /sign in to hiresense/i })).toBeInTheDocument()
  })

  it('routes the candidate signup path', () => {
    render(
      <MemoryRouter initialEntries={['/register/candidate']}>
        <App />
      </MemoryRouter>,
    )
    expect(screen.getByRole('heading', { name: /candidate account/i })).toBeInTheDocument()
  })
})
