import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { api, clearStoredAuth, loadStoredAuth, saveStoredAuth } from '../lib/api'
import type { StoredAuth, StoredUser } from '../lib/api'

interface AuthContextValue {
  user: StoredUser | null
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

interface LoginApiResponse {
  accessToken: string
  refreshToken: string
  user: StoredUser
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<StoredUser | null>(() => loadStoredAuth()?.user ?? null)

  const login = useCallback(async (email: string, password: string) => {
    const response = await api<LoginApiResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: { email, password },
    })
    const stored: StoredAuth = {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      user: response.user,
    }
    saveStoredAuth(stored)
    setUser(response.user)
  }, [])

  const logout = useCallback(async () => {
    const auth = loadStoredAuth()
    if (auth) {
      try {
        await api('/api/v1/auth/logout', {
          method: 'POST',
          body: { refreshToken: auth.refreshToken },
        })
      } catch {
        void 0
      }
    }
    clearStoredAuth()
    setUser(null)
  }, [])

  const value = useMemo(() => ({ user, login, logout }), [user, login, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
