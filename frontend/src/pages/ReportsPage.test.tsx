import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { api } from '../api/client'
import { ReportsPage } from './ReportsPage'

vi.mock('../api/client', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/client')>()
  return { ...actual, api: { ...actual.api, occupancyReport: vi.fn() } }
})

describe('ReportsPage', () => {
  it('rejects an inverted date range without requesting the report', async () => {
    render(<ReportsPage />)
    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-10-22' } })
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2026-10-21' } })
    fireEvent.click(screen.getByRole('button', { name: 'Generar reporte' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'La fecha inicial no puede ser posterior a la fecha final.',
    )
    expect(api.occupancyReport).not.toHaveBeenCalled()
  })
})
