import type { ReactNode } from 'react'
import { useAuth } from '../auth/useAuth'
import { ThemeToggle } from './ThemeToggle'

export type AppPage = 'home' | 'explore' | 'bookings' | 'provider' | 'schedules' | 'reports'

const navByRole: Record<string, { id: AppPage; label: string; glyph: string }[]> = {
  cliente: [
    { id: 'home', label: 'Resumen', glyph: '⌂' },
    { id: 'explore', label: 'Explorar servicios', glyph: '⌕' },
    { id: 'bookings', label: 'Mis reservas', glyph: '▣' },
  ],
  proveedor: [
    { id: 'home', label: 'Resumen', glyph: '⌂' },
    { id: 'provider', label: 'Mis servicios', glyph: '✳' },
    { id: 'schedules', label: 'Disponibilidad', glyph: '◷' },
  ],
  responsable: [
    { id: 'home', label: 'Resumen', glyph: '⌂' },
    { id: 'reports', label: 'Reportes', glyph: '▥' },
  ],
}

const pageTitle: Record<AppPage, string> = {
  home: 'Resumen',
  explore: 'Explorar servicios',
  bookings: 'Mis reservas',
  provider: 'Mis servicios',
  schedules: 'Disponibilidad',
  reports: 'Reportes de ocupación',
}

export function AppShell({
  page,
  onNavigate,
  onSignOut,
  children,
}: {
  page: AppPage
  onNavigate(page: AppPage): void
  onSignOut(): void
  children: ReactNode
}) {
  const { user } = useAuth()
  const items = navByRole[user?.role ?? ''] ?? [
    { id: 'home' as const, label: 'Resumen', glyph: '⌂' },
  ]
  const roleLabel = user?.role === 'responsable' ? 'Responsable' : user?.role

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <button className="brand sidebar-brand" onClick={() => onNavigate('home')} aria-label="Ir al inicio">
          <span className="brand-mark">r</span>
          <span>reserva<span className="brand-dot">.</span></span>
        </button>
        <div className="workspace-label">PLATAFORMA</div>
        <nav className="side-nav" aria-label="Navegación principal">
          {items.map((item) => (
            <button
              key={item.id}
              className={`nav-item ${page === item.id ? 'active' : ''}`}
              onClick={() => onNavigate(item.id)}
              aria-current={page === item.id ? 'page' : undefined}
            >
              <span className="nav-glyph" aria-hidden="true">{item.glyph}</span>
              {item.label}
              {page === item.id && <span className="nav-active-mark" />}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="sidebar-help">
            <span className="help-orb">?</span>
            <div><strong>¿Necesitas ayuda?</strong><small>Prueba los flujos de la API.</small></div>
          </div>
          <div className="profile-row">
            <div className="avatar">{user?.name.charAt(0).toUpperCase()}</div>
            <div className="profile-copy">
              <strong>{user?.name}</strong>
              <small>{roleLabel}</small>
            </div>
            <button className="icon-button signout-button" onClick={onSignOut} aria-label="Cerrar sesión" title="Cerrar sesión">
              ↗
            </button>
          </div>
        </div>
      </aside>

      <div className="main-column">
        <header className="topbar">
          <div>
            <span className="breadcrumb">Plataforma <span>/</span> {pageTitle[page]}</span>
            <h1>{pageTitle[page]}</h1>
          </div>
          <div className="topbar-right">
            <ThemeToggle />
            <span className="secure-indicator"><span /> Sesión activa</span>
            <div className="avatar avatar-small">{user?.name.charAt(0).toUpperCase()}</div>
          </div>
        </header>
        <main className="page-content">{children}</main>
      </div>
    </div>
  )
}
