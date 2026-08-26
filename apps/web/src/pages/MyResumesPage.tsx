import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useCallback, useEffect, useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import { ErrorBanner } from '../components/common/ErrorBanner'
import { api, ApiError } from '../lib/api'
import { logger } from '../lib/logger'

interface ResumeRow {
  id: number
  originalFilename: string
  fileSizeBytes: number
  status: 'UPLOADED' | 'PARSING' | 'PARSED' | 'FAILED'
  uploadedAt: string
}

const ACTIVE_STATUSES = new Set(['UPLOADED', 'PARSING'])

const STATUS_COLOR: Record<ResumeRow['status'], 'default' | 'info' | 'success' | 'error'> = {
  UPLOADED: 'info',
  PARSING: 'info',
  PARSED: 'success',
  FAILED: 'error',
}

export function MyResumesPage() {
  const [resumes, setResumes] = useState<ResumeRow[]>([])
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const refresh = useCallback(async () => {
    try {
      setResumes(await api<ResumeRow[]>('/api/v1/resumes'))
    } catch (e) {
      logger.error('resumes', 'failed to load resumes', e)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  useEffect(() => {
    const anyActive = resumes.some((r) => ACTIVE_STATUSES.has(r.status))
    if (!anyActive) return
    const timer = setInterval(() => void refresh(), 2500)
    return () => clearInterval(timer)
  }, [resumes, refresh])

  async function handleUpload(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setError(null)
    setUploading(true)
    try {
      const formData = new FormData()
      formData.append('file', file)
      await api('/api/v1/resumes', { method: 'POST', formData })
      await refresh()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Upload failed. Please try again.')
    } finally {
      setUploading(false)
    }
  }

  return (
    <Box sx={{ maxWidth: 720, mx: 'auto', mt: 6 }}>
      <Paper sx={{ p: 4 }}>
        <Typography variant="h5" gutterBottom>
          My Resumes
        </Typography>
        {error ? <ErrorBanner message={error} /> : null}
        <Stack direction="row" spacing={2} sx={{ mb: 3, alignItems: 'center' }}>
          <Button
            variant="contained"
            component="label"
            disabled={uploading}
            data-testid="upload-button"
          >
            {uploading ? 'Uploading…' : 'Upload resume (PDF or DOCX)'}
            <input
              type="file"
              hidden
              accept=".pdf,.docx"
              ref={fileInputRef}
              onChange={handleUpload}
            />
          </Button>
        </Stack>

        {resumes.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            No resumes uploaded yet.
          </Typography>
        ) : (
          <Stack spacing={1}>
            {resumes.map((resume) => (
              <Stack
                key={resume.id}
                direction="row"
                sx={{
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  border: 1,
                  borderColor: 'divider',
                  borderRadius: 1,
                  p: 1.5,
                }}
                data-testid={`resume-row-${resume.id}`}
              >
                <Box>
                  <Typography>{resume.originalFilename}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {(resume.fileSizeBytes / 1024).toFixed(0)} KB ·{' '}
                    {new Date(resume.uploadedAt).toLocaleString()}
                  </Typography>
                </Box>
                <Chip
                  size="small"
                  label={resume.status === 'FAILED' ? `Failed to parse` : resume.status}
                  color={STATUS_COLOR[resume.status]}
                />
              </Stack>
            ))}
          </Stack>
        )}
      </Paper>
    </Box>
  )
}
