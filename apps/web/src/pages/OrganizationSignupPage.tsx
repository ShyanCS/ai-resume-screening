import { Box, Button, Paper, Stack, Typography } from '@mui/material'
import { useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { FormTextField } from '../components/common/FormTextField'
import { api, ApiError } from '../lib/api'
import { flattenFieldErrors, organizationSignupSchema } from '../lib/schemas/auth'

export function OrganizationSignupPage() {
  const [form, setForm] = useState({
    orgName: '',
    slug: '',
    adminEmail: '',
    adminPassword: '',
    adminFullName: '',
  })
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
    const parsed = organizationSignupSchema.safeParse(form)
    if (!parsed.success) {
      setFieldErrors(flattenFieldErrors(parsed.error))
      return
    }
    setFieldErrors({})
    setSubmitting(true)
    try {
      await api('/api/v1/auth/register/organization', {
        method: 'POST',
        body: {
          orgName: parsed.data.orgName,
          slug: parsed.data.slug || undefined,
          admin: {
            email: parsed.data.adminEmail,
            password: parsed.data.adminPassword,
            fullName: parsed.data.adminFullName,
          },
        },
      })
      setSubmitted(true)
    } catch (error) {
      if (error instanceof ApiError && error.fieldErrors) {
        setFieldErrors(error.fieldErrors)
      }
      setSubmitError(error instanceof ApiError ? error.message : 'Unable to register right now')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Box maxWidth={480} mx="auto" mt={8}>
      <Paper sx={{ p: 4 }}>
        <Typography variant="h5" gutterBottom>
          Register your organization
        </Typography>
        {submitted ? (
          <Typography role="status">
            Organization registered. Verify the admin email, then sign in to start hiring.
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
                label="Organization name"
                name="orgName"
                value={form.orgName}
                error={fieldErrors.orgName}
                onChange={handleChange}
              />
              <FormTextField
                label="URL slug (optional)"
                name="slug"
                value={form.slug}
                error={fieldErrors.slug}
                onChange={handleChange}
                helperText="Leave blank to auto-generate from the name"
              />
              <Typography variant="subtitle2" sx={{ mt: 2 }}>
                Admin account
              </Typography>
              <FormTextField
                label="Admin email"
                name="adminEmail"
                type="email"
                value={form.adminEmail}
                error={fieldErrors['admin.email'] ?? fieldErrors.adminEmail}
                onChange={handleChange}
              />
              <FormTextField
                label="Admin password"
                name="adminPassword"
                type="password"
                value={form.adminPassword}
                error={fieldErrors['admin.password'] ?? fieldErrors.adminPassword}
                onChange={handleChange}
              />
              <FormTextField
                label="Admin full name"
                name="adminFullName"
                value={form.adminFullName}
                error={fieldErrors['admin.fullName'] ?? fieldErrors.adminFullName}
                onChange={handleChange}
              />
              <Button type="submit" variant="contained" disabled={submitting} sx={{ mt: 2 }}>
                {submitting ? 'Registering…' : 'Register organization'}
              </Button>
            </Stack>
            <Typography variant="body2" sx={{ mt: 2 }}>
              Already have an account? <RouterLink to="/login">Sign in</RouterLink>
            </Typography>
          </>
        )}
      </Paper>
    </Box>
  )
}
