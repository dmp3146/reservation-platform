import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import { AccessPage } from './AccessPage'

const { signIn } = vi.hoisted(() => ({ signIn: vi.fn() }))

vi.mock('../auth/useAuth', () => ({
  useAuth: () => ({ signIn }),
}))

describe('AccessPage', () => {
  beforeEach(() => {
    signIn.mockReset()
  })

  it('defaults to client and opens the existing client registration form', () => {
    render(<AccessPage />)

    expect(screen.getByRole('radio', { name: 'Soy cliente' })).toHaveAttribute('aria-checked', 'true')
    expect(screen.getByRole('radio', { name: 'Soy proveedor' })).toHaveAttribute('aria-checked', 'false')
    fireEvent.click(screen.getByRole('button', { name: 'Regístrate' }))

    expect(screen.getByRole('heading', { name: 'Crea tu cuenta' })).toBeInTheDocument()
    expect(screen.getByLabelText('Nombre completo')).toBeRequired()
    expect(screen.queryByLabelText(/rol/i)).not.toBeInTheDocument()
  })

  it('preserves entered login fields and opens provider registration from provider selection', () => {
    render(<AccessPage />)
    fireEvent.change(screen.getByLabelText('Correo electrónico'), {
      target: { value: 'provider@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Contraseña'), {
      target: { value: 'password123' },
    })
    fireEvent.click(screen.getByRole('radio', { name: 'Soy proveedor' }))

    expect(screen.getByLabelText('Correo electrónico')).toHaveValue('provider@example.com')
    expect(screen.getByLabelText('Contraseña')).toHaveValue('password123')
    expect(screen.getByRole('button', { name: 'Iniciar sesión como proveedor' })).toBeInTheDocument()
    expect(screen.getByText('¿Quieres registrar tu negocio?')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Regístrate como proveedor' }))

    expect(screen.getByRole('heading', { name: 'Registra tu negocio' })).toBeInTheDocument()
    expect(screen.getByLabelText('Nombre del negocio')).toBeRequired()
    expect(screen.getByLabelText('Correo electrónico')).toHaveValue('provider@example.com')
    expect(screen.getByLabelText(/^Contraseña/)).toHaveValue('password123')
  })

  it('sends only email and password and reports the authenticated backend identity', async () => {
    const authenticatedUser = {
      id: 7,
      name: 'Rosa Cliente',
      email: 'rosa@example.com',
      role: 'cliente',
    }
    signIn.mockResolvedValue(authenticatedUser)
    const onAuthenticated = vi.fn()
    render(<AccessPage onAuthenticated={onAuthenticated} />)
    fireEvent.change(screen.getByLabelText('Correo electrónico'), {
      target: { value: 'rosa@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Contraseña'), {
      target: { value: 'password123' },
    })
    fireEvent.click(screen.getByRole('radio', { name: 'Soy proveedor' }))
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión como proveedor' }))

    await waitFor(() => expect(onAuthenticated).toHaveBeenCalledWith(
      authenticatedUser,
      'proveedor',
    ))
    expect(signIn).toHaveBeenCalledWith({
      email: 'rosa@example.com',
      password: 'password123',
    })
    expect(onAuthenticated).toHaveBeenCalledWith(authenticatedUser, 'proveedor')
  })

  it('accepts the provider role returned by the backend when provider is selected', async () => {
    const authenticatedUser = {
      id: 8,
      name: 'Rosa Proveedor',
      email: 'rosa@example.com',
      role: 'proveedor',
    }
    signIn.mockResolvedValue(authenticatedUser)
    const onAuthenticated = vi.fn()
    render(<AccessPage onAuthenticated={onAuthenticated} />)
    fireEvent.click(screen.getByRole('radio', { name: 'Soy proveedor' }))
    fireEvent.change(screen.getByLabelText('Correo electrónico'), {
      target: { value: 'rosa@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Contraseña'), {
      target: { value: 'password123' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión como proveedor' }))

    await waitFor(() => expect(onAuthenticated).toHaveBeenCalledWith(
      authenticatedUser,
      'proveedor',
    ))
    expect(signIn).toHaveBeenCalledWith({
      email: 'rosa@example.com',
      password: 'password123',
    })
  })

  it('rejects mismatched registration passwords before making a request', async () => {
    render(<AccessPage />)
    fireEvent.click(screen.getByRole('button', { name: 'Regístrate' }))
    fireEvent.change(screen.getByLabelText('Nombre completo'), { target: { value: 'Lina' } })
    fireEvent.change(screen.getByLabelText('Correo electrónico'), { target: { value: 'lina@example.com' } })
    fireEvent.change(screen.getByLabelText(/^Contraseña/), { target: { value: 'password123' } })
    fireEvent.change(screen.getByLabelText('Confirmar contraseña'), { target: { value: 'different123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Las contraseñas no coinciden.')
    expect(signIn).not.toHaveBeenCalled()
  })

  it('keeps displaying backend authentication errors', async () => {
    signIn.mockRejectedValue(new ApiError({
      errorCode: 'INVALID_CREDENTIALS',
      message: 'Correo o contraseña incorrectos.',
      details: [],
      traceId: 'trace-login',
    }, 401))
    render(<AccessPage />)

    fireEvent.change(screen.getByLabelText('Correo electrónico'), {
      target: { value: 'wrong@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Contraseña'), {
      target: { value: 'wrong-password' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión como cliente' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Correo o contraseña incorrectos.')
  })
})
