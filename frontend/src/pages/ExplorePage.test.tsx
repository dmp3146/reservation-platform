import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, api } from '../api/client'
import type { ScheduleBlock, ServiceOffering } from '../api/types'
import { ExplorePage } from './ExplorePage'

vi.mock('../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/client')>()
  return {
    ...actual,
    api: {
      ...actual.api,
      availableServices: vi.fn(),
      availableSchedules: vi.fn(),
      bookedSlots: vi.fn(),
      createBooking: vi.fn(),
    },
  }
})

const service: ServiceOffering = {
  id: 17,
  nombre: 'Consulta inicial',
  descripcion: 'Orientación personalizada',
  duracionMinutos: 30,
  providerId: 4,
  nombreProveedor: 'Estudio Verde',
  tieneHorariosFuturos: true,
}

const block: ScheduleBlock = {
  id: 28,
  serviceId: 17,
  fecha: '2026-11-01',
  horaInicio: '09:00:00',
  horaFin: '10:00:00',
}

describe('ExplorePage', () => {
  afterEach(() => vi.restoreAllMocks())

  it('creates a booking only with a selected time from the returned block', async () => {
    vi.stubGlobal('confirm', vi.fn(() => true))
    vi.mocked(api.availableServices).mockResolvedValue([service])
    vi.mocked(api.availableSchedules).mockResolvedValue([block])
    vi.mocked(api.bookedSlots).mockResolvedValue([])
    vi.mocked(api.createBooking).mockResolvedValue({
      bookingId: 51,
      serviceId: service.id,
      serviceName: service.nombre,
      providerName: 'Estudio Verde',
      date: block.fecha,
      startTime: '09:00:00',
      endTime: '09:30:00',
      status: 'activa',
    })

    render(<ExplorePage />)

    fireEvent.click(await screen.findByRole('button', { name: 'Ver horarios' }))
    fireEvent.click((await screen.findAllByRole('radio'))[0])
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar reserva' }))

    await waitFor(() => expect(api.createBooking).toHaveBeenCalledWith({
      scheduleBlockId: 28,
      startTime: '09:00',
    }))
    expect(await screen.findByRole('status')).toHaveTextContent('Reserva #51 confirmada')
  })

  it('shows the server conflict when a selected schedule is no longer available', async () => {
    vi.stubGlobal('confirm', vi.fn(() => true))
    vi.mocked(api.availableServices).mockResolvedValue([service])
    vi.mocked(api.availableSchedules).mockResolvedValue([block])
    vi.mocked(api.bookedSlots).mockResolvedValue([])
    vi.mocked(api.createBooking).mockRejectedValue(new ApiError({
      errorCode: 'BOOKING_CONFLICT',
      message: 'El horario ya no está disponible.',
      details: [],
      traceId: 'trace-conflict',
    }, 409))

    render(<ExplorePage />)

    fireEvent.click(await screen.findByRole('button', { name: 'Ver horarios' }))
    fireEvent.click((await screen.findAllByRole('radio'))[0])
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar reserva' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('El horario ya no está disponible.')
  })

  it('shows an occupied time as unavailable and prevents selecting it', async () => {
    vi.mocked(api.availableServices).mockResolvedValue([service])
    vi.mocked(api.availableSchedules).mockResolvedValue([block])
    vi.mocked(api.bookedSlots).mockResolvedValue([{
      date: block.fecha,
      startTime: '09:00:00',
      endTime: '09:30:00',
    }])

    render(<ExplorePage />)
    fireEvent.click(await screen.findByRole('button', { name: 'Ver horarios' }))

    const bookedOption = await screen.findByRole('radio', { name: /09:00/ })
    expect(bookedOption).toBeDisabled()
    expect(bookedOption.closest('label')).toHaveClass('slot-option-booked')
    expect(screen.getByRole('button', { name: 'Confirmar reserva' })).toBeDisabled()
  })
})
