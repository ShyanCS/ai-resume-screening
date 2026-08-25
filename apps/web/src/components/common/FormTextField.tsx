import TextField from '@mui/material/TextField'
import type { ChangeEvent } from 'react'

interface FormTextFieldProps {
  label: string
  name: string
  type?: string
  value: string
  error?: string
  helperText?: string
  onChange: (event: ChangeEvent<HTMLInputElement>) => void
}

export function FormTextField({
  label,
  name,
  type = 'text',
  value,
  error,
  helperText,
  onChange,
}: FormTextFieldProps) {
  return (
    <TextField
      fullWidth
      margin="normal"
      label={label}
      name={name}
      type={type}
      value={value}
      onChange={onChange}
      error={Boolean(error)}
      helperText={error ?? helperText}
    />
  )
}
