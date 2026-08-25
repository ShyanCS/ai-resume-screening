import {
  candidateSignupSchema,
  flattenFieldErrors,
  loginSchema,
  organizationSignupSchema,
} from './auth'

describe('auth schemas', () => {
  it('accepts valid login input', () => {
    const parsed = loginSchema.safeParse({ email: 'a@b.com', password: 'x' })
    expect(parsed.success).toBe(true)
  })

  it('rejects invalid login email with a message', () => {
    const parsed = loginSchema.safeParse({ email: 'nope', password: 'x' })
    expect(parsed.success).toBe(false)
    const errors = flattenFieldErrors((parsed as { error: import('zod').ZodError }).error)
    expect(errors.email).toContain('valid email')
  })

  it('enforces minimum password length on signup', () => {
    const parsed = candidateSignupSchema.safeParse({
      fullName: 'A',
      email: 'a@b.com',
      password: 'short',
    })
    const errors = flattenFieldErrors((parsed as { error: import('zod').ZodError }).error)
    expect(errors.password).toContain('At least 8')
  })

  it('restricts slug characters', () => {
    const parsed = organizationSignupSchema.safeParse({
      orgName: 'Acme',
      slug: 'Bad Slug!',
      adminEmail: 'a@b.com',
      adminPassword: 'longenough1',
      adminFullName: 'Admin',
    })
    const errors = flattenFieldErrors((parsed as { error: import('zod').ZodError }).error)
    expect(errors.slug).toContain('Lowercase')
  })

  it('allows empty slug for auto-derivation', () => {
    const parsed = organizationSignupSchema.safeParse({
      orgName: 'Acme',
      slug: '',
      adminEmail: 'a@b.com',
      adminPassword: 'longenough1',
      adminFullName: 'Admin',
    })
    expect(parsed.success).toBe(true)
  })
})
