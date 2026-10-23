import { useState } from 'react'
import { AppShell, type AppPage } from './components/AppShell'
import { Alert, Loading } from './components/Feedback'
import { useAuth } from './auth/useAuth'
import { AccessPage } from './pages/AccessPage'
import { BookingsPage } from './pages/BookingsPage'
import { DashboardPage } from './pages/DashboardPage'
import { ExplorePage } from './pages/ExplorePage'
import { ProviderPage } from './pages/ProviderPage'
import { ReportsPage } from './pages/ReportsPage'
import { SchedulesPage } from './pages/SchedulesPage'
import type { SessionUser } from './api/types'
import type { LoginAccountType } from './pages/AccessPage'

function mismatchNotice(user: SessionUser, selectedAccountType: LoginAccountType): string {
  if (user.role === selectedAccountType) return ''
  const roleNames: Record<string, string> = {
    cliente: 'cliente',
    proveedor: 'proveedor',
    responsable: 'responsable',
  }
  const actualRole = roleNames[user.role] ?? user.role
  return `Iniciaste sesión con una cuenta de ${actualRole}, no como ${selectedAccountType}. Se mantuvo el rol y los permisos de tu cuenta.`
}

function AccessBoundary() {
  const { loading, user, authError, refreshSession } = useAuth()
  const [accessNotice, setAccessNotice] = useState('')
  if (loading) return <div className="session-loading"><Loading label="Comprobando tu sesión…" /></div>
  if (!user) {
    return (
      <AccessPage
        sessionError={authError}
        onRetry={() => void refreshSession()}
        onAuthenticated={(authenticatedUser, selectedAccountType) => {
          setAccessNotice(mismatchNotice(authenticatedUser, selectedAccountType))
        }}
      />
    )
  }
  return <AuthenticatedApp accessNotice={accessNotice} onDismissAccessNotice={() => setAccessNotice('')} />
}

function AuthenticatedApp({
  accessNotice,
  onDismissAccessNotice,
}: {
  accessNotice: string
  onDismissAccessNotice(): void
}) {
  const { user, signOut } = useAuth()
  const [page, setPage] = useState<AppPage>('home')
  const [signOutError, setSignOutError] = useState('')
  if (!user) return null

  async function logout() {
    onDismissAccessNotice()
    setSignOutError('')
    try {
      await signOut()
    } catch (cause) {
      setSignOutError(cause instanceof Error ? cause.message : 'No fue posible cerrar la sesión.')
    }
  }

  const allowedPages: Record<string, AppPage[]> = {
    cliente: ['home', 'explore', 'bookings'],
    proveedor: ['home', 'provider', 'schedules'],
    responsable: ['home', 'reports'],
  }
  const permitted = allowedPages[user.role] ?? ['home']
  const activePage = permitted.includes(page) ? page : 'home'

  return (
    <AppShell page={activePage} onNavigate={setPage} onSignOut={() => void logout()}>
      {accessNotice && <Alert tone="info">{accessNotice}</Alert>}
      {signOutError && <Alert>{signOutError}</Alert>}
      {activePage === 'home' && <DashboardPage onNavigate={setPage} />}
      {activePage === 'explore' && <ExplorePage />}
      {activePage === 'bookings' && <BookingsPage />}
      {activePage === 'provider' && <ProviderPage />}
      {activePage === 'schedules' && <SchedulesPage />}
      {activePage === 'reports' && <ReportsPage />}
    </AppShell>
  )
}

export default function App() {
  return <AccessBoundary />
}
