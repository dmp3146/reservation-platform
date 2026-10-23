import type {
  ApiProblem,
  ApiSuccess,
  BookedSlot,
  Booking,
  BookingCreatePayload,
  BookingDetail,
  LoginPayload,
  OccupancyReport,
  ProviderPage,
  ProviderRegisterPayload,
  RegisterPayload,
  ScheduleBlock,
  ScheduleCreatePayload,
  ServiceCreatePayload,
  ServiceOffering,
  SessionUser,
} from './types'

const apiBase = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')
const apiRoot = `${apiBase}/api/v1`

export class ApiError extends Error {
  readonly status?: number
  readonly code: string
  readonly details: string[]
  readonly traceId?: string

  constructor(problem: ApiProblem, status: number)
  constructor(problem: undefined, status: number | undefined, message: string)
  constructor(
    problem: ApiProblem | undefined,
    status: number | undefined,
    message?: string,
  ) {
    super(problem?.message ?? message ?? 'No fue posible completar la solicitud.')
    this.name = 'ApiError'
    this.status = status
    this.code = problem?.errorCode ?? (status === undefined ? 'NETWORK_ERROR' : 'HTTP_ERROR')
    this.details = problem?.details ?? []
    this.traceId = problem?.traceId
  }
}

interface CsrfToken {
  token: string
  headerName: string
}

let csrfToken: CsrfToken | null = null

async function fetchCsrf(): Promise<CsrfToken> {
  let response: Response
  try {
    response = await fetch(`${apiRoot}/auth/csrf`, {
      credentials: 'include',
      headers: { Accept: 'application/json' },
    })
  } catch {
    throw new ApiError(
      undefined,
      undefined,
      'No se pudo conectar con el backend. Comprueba que esté iniciado.',
    )
  }
  if (!response.ok) {
    throw await errorFromResponse(response)
  }
  const body = (await response.json()) as CsrfToken
  csrfToken = body
  return body
}

async function errorFromResponse(response: Response): Promise<ApiError> {
  let problem: ApiProblem | undefined
  try {
    problem = (await response.json()) as ApiProblem
  } catch {
    problem = undefined
  }
  if (problem?.errorCode) {
    return new ApiError(problem, response.status)
  }
  if ([502, 503, 504].includes(response.status)) {
    return new ApiError(
      undefined,
      response.status,
      'El backend no está disponible en este momento. Comprueba que esté iniciado.',
    )
  }
  return new ApiError(
    undefined,
    response.status,
    response.status === 401
      ? 'Tu sesión venció o no has iniciado sesión.'
      : response.status === 403
        ? 'No tienes permiso para realizar esta acción.'
        : `El servidor respondió con el código ${response.status}.`,
  )
}

async function request<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body) {
    headers.set('Content-Type', 'application/json')
  }
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const csrf = csrfToken ?? (await fetchCsrf())
    headers.set(csrf.headerName, csrf.token)
  }

  let response: Response
  try {
    response = await fetch(`${apiRoot}${path}`, {
      ...init,
      method,
      headers,
      credentials: 'include',
    })
  } catch {
    throw new ApiError(
      undefined,
      undefined,
      'No se pudo conectar con el backend. Comprueba que esté iniciado.',
    )
  }

  if (!response.ok) {
    throw await errorFromResponse(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

function data<T>(promise: Promise<ApiSuccess<T>>): Promise<T> {
  return promise.then((response) => response.data)
}

export const api = {
  async login(payload: LoginPayload): Promise<SessionUser> {
    csrfToken = null
    const user = await data(
      request<ApiSuccess<SessionUser>>('/auth/login', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    )
    csrfToken = null
    return user
  },

  async logout(): Promise<void> {
    await request<void>('/auth/logout', { method: 'POST' })
    csrfToken = null
  },

  session(): Promise<SessionUser> {
    return data(request<ApiSuccess<SessionUser>>('/auth/me'))
  },

  async registerCustomer(payload: RegisterPayload): Promise<void> {
    await request<ApiSuccess<unknown>>('/users', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  async registerProvider(payload: ProviderRegisterPayload): Promise<void> {
    await request<ApiSuccess<unknown>>('/providers', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  providers(page = 0, size = 20): Promise<ProviderPage> {
    return data(
      request<ApiSuccess<ProviderPage>>(`/providers?page=${page}&size=${size}`),
    )
  },

  providerServices(providerId: number): Promise<ServiceOffering[]> {
    return data(
      request<ApiSuccess<ServiceOffering[]>>(
        `/providers/${providerId}/services`,
      ),
    )
  },

  availableServices(): Promise<ServiceOffering[]> {
    return data(
      request<ApiSuccess<ServiceOffering[]>>('/services/available'),
    )
  },

  myServices(): Promise<ServiceOffering[]> {
    return data(
      request<ApiSuccess<ServiceOffering[]>>('/providers/me/services'),
    )
  },

  createService(payload: ServiceCreatePayload): Promise<ServiceOffering> {
    return data(
      request<ApiSuccess<ServiceOffering>>('/services', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    )
  },

  deleteService(serviceId: number): Promise<void> {
    return data(
      request<ApiSuccess<void>>(`/services/${serviceId}`, { method: 'DELETE' }),
    )
  },

  availableSchedules(serviceId: number): Promise<ScheduleBlock[]> {
    return data(
      request<ApiSuccess<ScheduleBlock[]>>(
        `/services/${serviceId}/schedules/available`,
      ),
    )
  },

  createSchedule(payload: ScheduleCreatePayload): Promise<ScheduleBlock> {
    return data(
      request<ApiSuccess<ScheduleBlock>>('/providers/me/schedules', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    )
  },

  createBooking(payload: BookingCreatePayload): Promise<Booking> {
    return data(
      request<ApiSuccess<Booking>>('/bookings', {
        method: 'POST',
        body: JSON.stringify(payload),
      }),
    )
  },

  activeBookings(): Promise<Booking[]> {
    return data(request<ApiSuccess<Booking[]>>('/bookings/active'))
  },

  providerActiveBookings(): Promise<Booking[]> {
    return data(request<ApiSuccess<Booking[]>>('/bookings/provider/active'))
  },

  bookedSlots(serviceId: number): Promise<BookedSlot[]> {
    return data(
      request<ApiSuccess<BookedSlot[]>>(
        `/bookings/service/${serviceId}/active-slots`,
      ),
    )
  },

  bookingHistory(): Promise<Booking[]> {
    return data(request<ApiSuccess<Booking[]>>('/bookings/history'))
  },

  bookingDetail(bookingId: number): Promise<BookingDetail> {
    return data(
      request<ApiSuccess<BookingDetail>>(`/bookings/${bookingId}`),
    )
  },

  cancelBooking(bookingId: number): Promise<Booking> {
    return data(
      request<ApiSuccess<Booking>>(`/bookings/${bookingId}/cancel`, {
        method: 'PATCH',
      }),
    )
  },

  occupancyReport(startDate: string, endDate: string): Promise<OccupancyReport> {
    const query = new URLSearchParams({ startDate, endDate })
    return data(
      request<ApiSuccess<OccupancyReport>>(
        `/reports/occupancy?${query.toString()}`,
      ),
    )
  },
}

export function resetCsrfForTests(): void {
  csrfToken = null
}
