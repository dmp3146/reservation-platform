import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api } from '../api/client'
import { ProviderBookingsCalendar } from './ProviderBookingsCalendar'

vi.mock('../api/client', () => ({
  api: { providerActiveBookings: vi.fn() },
}))

describe('ProviderBookingsCalendar', () => {
  beforeEach(() => {
    vi.mocked(api.providerActiveBookings).mockResolvedValue([])
  })

  it('shows confirmed service and time on the selected appointment day', async () => {
    const now = new Date()
    const date = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    vi.mocked(api.providerActiveBookings).mockResolvedValue([{
      bookingId: 71,
      serviceId: 15,
      serviceName: 'Consulta inicial',
      providerName: 'Estudio Verde',
      date,
      startTime: '14:00:00',
      endTime: '14:30:00',
      status: 'activa',
    }])

    render(<ProviderBookingsCalendar />)

    expect(await screen.findByText('Consulta inicial')).toBeInTheDocument()
    expect(screen.getByText('14:00 – 14:30')).toBeInTheDocument()
    expect(screen.getByText('Reserva confirmada')).toBeInTheDocument()
  })
})
