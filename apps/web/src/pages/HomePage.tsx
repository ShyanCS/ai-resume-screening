import { Box, Button, Typography } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function HomePage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <Box maxWidth={720} mx="auto" mt={8}>
      <Typography variant="h4" gutterBottom>
        HireSense
      </Typography>
      <Typography variant="body1" data-testid="signed-in-as">
        Signed in as {user?.email} ({user?.platformRole})
      </Typography>
      <Button variant="outlined" onClick={handleLogout} sx={{ mt: 2 }}>
        Sign out
      </Button>
    </Box>
  )
}
