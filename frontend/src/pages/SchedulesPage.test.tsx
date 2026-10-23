import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SchedulesPage } from './SchedulesPage'

const { api } = vi.hoisted(() => ({
  api: {
    myServices: vi.fn(),
    createSchedule: vi.fn(),
    availableSchedules: vi.fn(),
    providerActiveBookings: vi.fn(),
  },
}))

vi.mock('../api/client', () => ({ api, ApiError: class ApiError extends Error {} }))

describe('SchedulesPage', () => {
  beforeEach(() => {
    api.myServices.mockResolvedValue([{
      id: 4,
      nombre: 'Consulta',
      descripcion: 'Atención personalizada',
      duracionMinutos: 30,
      activo: true,
    }])
    api.createSchedule.mockReset()
    api.availableSchedules.mockResolvedValue([])
    api.providerActiveBookings.mockResolvedValue([])
  })

  it('rejects an end time that is not later than the start time', async () => {
    render(<SchedulesPage />)

    fireEvent.change(await screen.findByLabelText('Servicio'), { target: { value: '4' } })
    fireEvent.change(screen.getByLabelText('Fecha'), { target: { value: '2030-01-15' } })
    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '10:00' } })
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '10:00' } })
    fireEvent.submit(screen.getByLabelText('Servicio').closest('form')!)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'La hora de finalización debe ser posterior a la hora de inicio.',
    )
    expect(api.createSchedule).not.toHaveBeenCalled()
  })
})
