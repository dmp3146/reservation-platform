import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api, resetCsrfForTests } from './client'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('API client', () => {
  beforeEach(() => {
    resetCsrfForTests()
    vi.restoreAllMocks()
  })

  it('sends session credentials and CSRF header for a write request', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({
        token: 'csrf-test-token',
        headerName: 'X-CSRF-TOKEN',
        parameterName: '_csrf',
      }))
      .mockResolvedValueOnce(jsonResponse({
        message: 'created',
        data: { bookingId: 44 },
      }, 201))
    vi.stubGlobal('fetch', fetchMock)

    await api.createBooking({ scheduleBlockId: 12, startTime: '10:00' })

    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ credentials: 'include' })
    expect(fetchMock.mock.calls[1][1]).toMatchObject({ credentials: 'include' })
    expect(new Headers(fetchMock.mock.calls[1][1]?.headers).get('X-CSRF-TOKEN'))
      .toBe('csrf-test-token')
    expect(JSON.parse(String(fetchMock.mock.calls[1][1]?.body))).toEqual({
      scheduleBlockId: 12,
      startTime: '10:00',
    })
  })

  it('preserves authentication status and standard API error details', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(jsonResponse({
      errorCode: 'AUTHENTICATION_REQUIRED',
      message: 'Debes iniciar sesión.',
      details: [],
      traceId: 'trace-test',
    }, 401)))

    await expect(api.session()).rejects.toMatchObject({
      status: 401,
      code: 'AUTHENTICATION_REQUIRED',
      traceId: 'trace-test',
    })
  })

  it('preserves forbidden responses so the UI can distinguish authorization errors', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(jsonResponse({
      errorCode: 'ACCESS_DENIED',
      message: 'No tienes permiso para acceder a este recurso.',
      details: [],
      traceId: 'trace-forbidden',
    }, 403)))

    await expect(api.session()).rejects.toMatchObject({
      status: 403,
      code: 'ACCESS_DENIED',
      message: 'No tienes permiso para acceder a este recurso.',
    })
  })

  it('reports unavailable backend without converting it to empty data', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValueOnce(new TypeError('offline')))

    await expect(api.availableServices()).rejects.toMatchObject({
      code: 'NETWORK_ERROR',
      message: expect.stringContaining('backend'),
    })
  })
})
