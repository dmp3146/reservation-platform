import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from '../api/client'
import type { Booking } from '../api/types'
import { BookingsPage } from './BookingsPage'

vi.mock('../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/client')>()
  return {
    ...actual,
    api: {
      ...actual.api,
      activeBookings: vi.fn(),
      bookingHistory: vi.fn(),
      bookingDetail: vi.fn(),
      cancelBooking: vi.fn(),
    },
  }
})

const booking: Booking = {
  bookingId: 41,
  serviceId: 9,
  serviceName: 'Consulta inicial',
  providerName: 'Estudio Verde',
  date: '2026-11-01',
  startTime: '09:00:00',
  endTime: '09:30:00',
  status: 'activa',
}

describe('BookingsPage', () => {
  afterEach(() => vi.restoreAllMocks())

  it('shows the empty state returned by the active bookings endpoint', async () => {
    vi.mocked(api.activeBookings).mockResolvedValue([])

    render(<BookingsPage />)

    expect(await screen.findByText('No tienes reservas activas')).toBeInTheDocument()
  })

  it('cancels the selected reservation and refreshes the active list', async () => {
    vi.stubGlobal('confirm', vi.fn(() => true))
    vi.mocked(api.activeBookings)
      .mockResolvedValueOnce([booking])
      .mockResolvedValueOnce([])
    vi.mocked(api.cancelBooking).mockResolvedValue({ ...booking, status: 'cancelada' })

    render(<BookingsPage />)

    fireEvent.click(await screen.findByRole('button', { name: 'Cancelar' }))

    await waitFor(() => expect(api.cancelBooking).toHaveBeenCalledWith(41))
    expect(await screen.findByText('No tienes reservas activas')).toBeInTheDocument()
    expect(await screen.findByRole('status')).toHaveTextContent('quedó cancelada')
  })
})
