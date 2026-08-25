import { Box, Button, Paper, Stack, Typography } from '@mui/material'
import { useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ErrorBanner } from '../components/common/ErrorBanner'
import { FormTextField } from '../components/common/FormTextField'
import { ApiError } from '../lib/api'
import { flattenFieldErrors, loginSchema } from '../lib/schemas/auth'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitError(null)
    const parsed = loginSchema.safeParse(form)
    if (!parsed.success) {
      setFieldErrors(flattenFieldErrors(parsed.error))
      return
    }
    setFieldErrors({})
    setSubmitting(true)
    try {
      await login(parsed.data.email, parsed.data.password)
      navigate('/')
    } catch (error) {
      setSubmitError(error instanceof ApiError ? error.message : 'Unable to sign in right now')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Box maxWidth={420} mx="auto" mt={8}>
      <Paper sx={{ p: 4 }}>
        <Typography variant="h5" gutterBottom>
          Sign in to HireSense
        </Typography>
        {submitError ? <ErrorBanner message={submitError} /> : null}
        <Stack component="form" onSubmit={handleSubmit} noValidate>
          <FormTextField
            label="Email"
            name="email"
            type="email"
            value={form.email}
            error={fieldErrors.email}
            onChange={handleChange}
          />
          <FormTextField
            label="Password"
            name="password"
            type="password"
            value={form.password}
            error={fieldErrors.password}
            onChange={handleChange}
          />
          <Button type="submit" variant="contained" disabled={submitting} sx={{ mt: 2 }}>
            {submitting ? 'Signing in…' : 'Sign in'}
          </Button>
        </Stack>
        <Typography variant="body2" sx={{ mt: 2 }}>
          New candidate? <RouterLink to="/register/candidate">Create an account</RouterLink>
        </Typography>
        <Typography variant="body2">
          Hiring for a company?{' '}
          <RouterLink to="/register/organization">Register organization</RouterLink>
        </Typography>
      </Paper>
    </Box>
  )
}
