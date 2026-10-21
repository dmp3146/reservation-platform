import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { AuthProvider } from './auth/AuthContext'

describe('application access', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('restores a provider session and shows only provider navigation', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({
      message: 'session',
      data: {
        id: 8,
        name: 'Rosa Proveedor',
        email: 'rosa@example.com',
        role: 'proveedor',
      },
    }), { status: 200, headers: { 'Content-Type': 'application/json' } })))

    render(<AuthProvider><App /></AuthProvider>)

    expect(await screen.findByRole('heading', { name: /Hola, Rosa/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Mis servicios' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Mis reservas' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Reportes' })).not.toBeInTheDocument()
  })

  it('shows the login page when no authenticated session exists', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({
      errorCode: 'AUTHENTICATION_REQUIRED',
      message: 'Debes iniciar sesión.',
      details: [],
      traceId: 'trace',
    }), { status: 401, headers: { 'Content-Type': 'application/json' } })))

    render(<AuthProvider><App /></AuthProvider>)

    expect(await screen.findByRole('heading', { name: 'Inicia sesión' })).toBeInTheDocument()
  })
})
