import { fireEvent, render, screen } from '@testing-library/react'
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

  it('keeps the authenticated customer role when provider was only the selected login preference', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input)
      if (url.endsWith('/auth/me')) {
        return new Response(JSON.stringify({
          errorCode: 'AUTHENTICATION_REQUIRED',
          message: 'Debes iniciar sesión.',
          details: [],
          traceId: 'trace',
        }), { status: 401, headers: { 'Content-Type': 'application/json' } })
      }
      if (url.endsWith('/auth/csrf')) {
        return new Response(JSON.stringify({
          token: 'csrf-token',
          headerName: 'X-CSRF-TOKEN',
        }), { status: 200, headers: { 'Content-Type': 'application/json' } })
      }
      if (url.endsWith('/auth/login')) {
        expect(JSON.parse(String(init?.body))).toEqual({
          email: 'customer@example.com',
          password: 'password123',
        })
        return new Response(JSON.stringify({
          message: 'session',
          data: {
            id: 9,
            name: 'Carla Cliente',
            email: 'customer@example.com',
            role: 'cliente',
          },
        }), { status: 200, headers: { 'Content-Type': 'application/json' } })
      }
      throw new Error(`Unexpected request: ${url}`)
    })
    vi.stubGlobal('fetch', fetchMock)

    render(<AuthProvider><App /></AuthProvider>)
    await screen.findByRole('heading', { name: 'Inicia sesión' })
    fireEvent.click(screen.getByRole('radio', { name: 'Soy proveedor' }))
    fireEvent.change(screen.getByLabelText('Correo electrónico'), {
      target: { value: 'customer@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Contraseña'), {
      target: { value: 'password123' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión como proveedor' }))

    expect(await screen.findByRole('button', { name: 'Mis reservas' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Mis servicios' })).not.toBeInTheDocument()
    expect(await screen.findByRole('status')).toHaveTextContent(
      'Iniciaste sesión con una cuenta de cliente, no como proveedor.',
    )
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('preserves responsible navigation when signing in from the default customer option', async () => {
    const fetchMock = vi.fn((input: RequestInfo | URL) => {
      const url = String(input)
      if (url.endsWith('/auth/me')) {
        return Promise.resolve(new Response(JSON.stringify({
          errorCode: 'AUTHENTICATION_REQUIRED',
          message: 'Debes iniciar sesión.',
          details: [],
          traceId: 'trace',
        }), { status: 401, headers: { 'Content-Type': 'application/json' } }))
      }
      if (url.endsWith('/auth/csrf')) {
        return Promise.resolve(new Response(JSON.stringify({
          token: 'csrf-token',
          headerName: 'X-CSRF-TOKEN',
        }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
      }
      if (url.endsWith('/auth/login')) {
        return Promise.resolve(new Response(JSON.stringify({
          message: 'session',
          data: {
            id: 10,
            name: 'Rafa Responsable',
            email: 'manager@example.com',
            role: 'responsable',
          },
        }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
      }
      throw new Error(`Unexpected request: ${url}`)
    })
    vi.stubGlobal('fetch', fetchMock)

    render(<AuthProvider><App /></AuthProvider>)
    await screen.findByRole('heading', { name: 'Inicia sesión' })
    fireEvent.change(screen.getByLabelText('Correo electrónico'), {
      target: { value: 'manager@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Contraseña'), {
      target: { value: 'password123' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión como cliente' }))

    expect(await screen.findByRole('button', { name: 'Reportes' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Mis reservas' })).not.toBeInTheDocument()
    expect(await screen.findByRole('status')).toHaveTextContent(
      'Iniciaste sesión con una cuenta de responsable, no como cliente.',
    )
  })
})
