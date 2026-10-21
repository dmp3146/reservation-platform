import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AccessPage } from './AccessPage'

const { signIn } = vi.hoisted(() => ({ signIn: vi.fn() }))

vi.mock('../auth/useAuth', () => ({
  useAuth: () => ({ signIn }),
}))

describe('AccessPage', () => {
  it('offers real account types without a client-controlled role field', () => {
    render(<AccessPage />)

    fireEvent.click(screen.getByRole('button', { name: 'Crear cuenta de cliente' }))

    expect(screen.getByRole('heading', { name: 'Crea tu cuenta' })).toBeInTheDocument()
    expect(screen.getByLabelText('Nombre completo')).toBeRequired()
    expect(screen.queryByLabelText(/rol/i)).not.toBeInTheDocument()
  })

  it('rejects mismatched registration passwords before making a request', async () => {
    render(<AccessPage />)
    fireEvent.click(screen.getByRole('button', { name: 'Crear cuenta de cliente' }))
    fireEvent.change(screen.getByLabelText('Nombre completo'), { target: { value: 'Lina' } })
    fireEvent.change(screen.getByLabelText('Correo electrónico'), { target: { value: 'lina@example.com' } })
    fireEvent.change(screen.getByLabelText(/^Contraseña/), { target: { value: 'password123' } })
    fireEvent.change(screen.getByLabelText('Confirmar contraseña'), { target: { value: 'different123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Las contraseñas no coinciden.')
    expect(signIn).not.toHaveBeenCalled()
  })
})
