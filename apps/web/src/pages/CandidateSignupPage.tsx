import { Box, Button, Paper, Stack, Typography } from '@mui/material'
import { useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { FormTextField } from '../components/common/FormTextField'
import { api, ApiError } from '../lib/api'
import { candidateSignupSchema, flattenFieldErrors } from '../lib/schemas/auth'

export function CandidateSignupPage() {
  const [form, setForm] = useState({ fullName: '', email: '', password: '' })
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [submitted, setSubmitted] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitError(null)
    const parsed = candidateSignupSchema.safeParse(form)
    if (!parsed.success) {
      setFieldErrors(flattenFieldErrors(parsed.error))
      return
    }
    setFieldErrors({})
    setSubmitting(true)
    try {
      await api('/api/v1/auth/register/candidate', { method: 'POST', body: parsed.data })
      setSubmitted(true)
    } catch (error) {
      if (error instanceof ApiError && error.fieldErrors) {
        setFieldErrors(error.fieldErrors)
      }
      setSubmitError(error instanceof ApiError ? error.message : 'Unable to sign up right now')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Box maxWidth={420} mx="auto" mt={8}>
      <Paper sx={{ p: 4 }}>
        <Typography variant="h5" gutterBottom>
          Create your candidate account
        </Typography>
        {submitted ? (
          <Typography role="status">
            Account created. Check your inbox for the verification link, then sign in.
          </Typography>
        ) : (
          <>
            {submitError ? (
              <Box component="div" color="error.main" mb={1}>
                {submitError}
              </Box>
            ) : null}
            <Stack component="form" onSubmit={handleSubmit} noValidate>
              <FormTextField
                label="Full name"
                name="fullName"
                value={form.fullName}
                error={fieldErrors.fullName}
                onChange={handleChange}
              />
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
                {submitting ? 'Creating…' : 'Create account'}
              </Button>
            </Stack>
            <Typography variant="body2" sx={{ mt: 2 }}>
              Already registered? <RouterLink to="/login">Sign in</RouterLink>
            </Typography>
          </>
        )}
      </Paper>
    </Box>
  )
}
