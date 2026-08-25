import { z } from 'zod'

export const loginSchema = z.object({
  email: z.email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
})

export const candidateSignupSchema = z.object({
  fullName: z.string().min(1, 'Full name is required').max(200, 'At most 200 characters'),
  email: z.email('Enter a valid email address'),
  password: z.string().min(8, 'At least 8 characters').max(100, 'At most 100 characters'),
})

export const organizationSignupSchema = z.object({
  orgName: z.string().min(1, 'Organization name is required').max(200, 'At most 200 characters'),
  slug: z
    .string()
    .max(100, 'At most 100 characters')
    .regex(/^[a-z0-9-]*$/, 'Lowercase letters, numbers and hyphens only'),
  adminEmail: z.email('Enter a valid email address'),
  adminPassword: z.string().min(8, 'At least 8 characters').max(100, 'At most 100 characters'),
  adminFullName: z.string().min(1, 'Admin name is required').max(200, 'At most 200 characters'),
})

export type LoginInput = z.infer<typeof loginSchema>
export type CandidateSignupInput = z.infer<typeof candidateSignupSchema>
export type OrganizationSignupInput = z.infer<typeof organizationSignupSchema>

export function flattenFieldErrors(error: z.ZodError): Record<string, string> {
  const result: Record<string, string> = {}
  for (const issue of error.issues) {
    const key = issue.path.join('.')
    if (key && !result[key]) {
      result[key] = issue.message
    }
  }
  return result
}
