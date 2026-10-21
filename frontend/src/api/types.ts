export interface ApiSuccess<T> {
  message: string
  data: T
}

export interface ApiProblem {
  errorCode: string
  message: string
  details: string[]
  traceId: string
}

export interface SessionUser {
  id: number
  name: string
  email: string
  role: 'cliente' | 'proveedor' | 'responsable' | string
}

export interface Provider {
  providerId: number
  nombreNegocio: string
}

export interface ProviderPage {
  items: Provider[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ServiceOffering {
  id: number
  nombre: string
  descripcion?: string
  duracionMinutos: number
  activo?: boolean | null
  providerId?: number
  nombreProveedor?: string
  tieneHorariosFuturos?: boolean
}

export interface ScheduleBlock {
  id: number
  serviceId: number
  fecha: string
  horaInicio: string
  horaFin: string
}

export interface Booking {
  bookingId: number
  serviceId: number
  serviceName: string
  providerName: string
  date: string
  startTime: string
  endTime: string
  status: 'activa' | 'cancelada' | 'completada' | string
}

export interface BookingDetail extends Booking {
  customerName: string
}

export interface OccupancyReport {
  startDate: string
  endDate: string
  totalBookings: number
  effectiveBookings: number
  cancelledBookings: number
  bookingsWithoutServiceAttribution: number
  hasBookings: boolean
  services: {
    serviceId: number
    serviceName: string
    effectiveBookings: number
    cancelledBookings: number
    reservedMinutes: number
  }[]
  resources: {
    resourceId: number
    resourceName: string
    effectiveBookings: number
  }[]
  resourceMetricsAvailable: boolean
  resourceMetricsLimitation: string
}

export interface LoginPayload {
  email: string
  password: string
}

export interface RegisterPayload {
  name: string
  email: string
  password: string
  confirmPassword: string
}

export interface ProviderRegisterPayload extends RegisterPayload {
  businessName: string
  phone: string
}

export interface ServiceCreatePayload {
  nombre: string
  descripcion: string
  duracionMinutos: number
}

export interface ScheduleCreatePayload {
  serviceId: number
  fecha: string
  horaInicio: string
  horaFin: string
}

export interface BookingCreatePayload {
  scheduleBlockId: number
  startTime: string
}
