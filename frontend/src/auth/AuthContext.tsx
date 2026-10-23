import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { ApiError, api } from '../api/client'
import { AuthContext } from './context'
import type { AuthContextValue } from './context'
import type { LoginPayload, SessionUser } from '../api/types'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(null)
  const [loading, setLoading] = useState(true)
  const [authError, setAuthError] = useState<string | null>(null)

  const loadSession = useCallback(async () => {
    try {
      setUser(await api.session())
      setAuthError(null)
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        setUser(null)
        setAuthError(null)
      } else {
        setUser(null)
        setAuthError(
          error instanceof Error
            ? error.message
            : 'No fue posible comprobar la sesión.',
        )
      }
    } finally {
      setLoading(false)
    }
  }, [])

  const refreshSession = useCallback(async () => {
    setLoading(true)
    await loadSession()
  }, [loadSession])

  useEffect(() => {
    void Promise.resolve().then(loadSession)
  }, [loadSession])

  const signIn = useCallback(async (payload: LoginPayload) => {
    const authenticatedUser = await api.login(payload)
    setUser(authenticatedUser)
    setAuthError(null)
    return authenticatedUser
  }, [])

  const signOut = useCallback(async () => {
    await api.logout()
    setUser(null)
  }, [])

  const value: AuthContextValue = useMemo(
    () => ({ user, loading, authError, signIn, signOut, refreshSession }),
    [user, loading, authError, signIn, signOut, refreshSession],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
