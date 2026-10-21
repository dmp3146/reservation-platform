import { createContext } from 'react'
import type { LoginPayload, SessionUser } from '../api/types'

export interface AuthContextValue {
  user: SessionUser | null
  loading: boolean
  authError: string | null
  signIn(payload: LoginPayload): Promise<void>
  signOut(): Promise<void>
  refreshSession(): Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
