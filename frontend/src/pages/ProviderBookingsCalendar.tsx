import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client'
import type { Booking } from '../api/types'
import { Alert, Loading } from '../components/Feedback'

function localDateKey(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function monthDays(month: Date): { date: Date; currentMonth: boolean }[] {
  const firstDay = new Date(month.getFullYear(), month.getMonth(), 1)
  const mondayOffset = (firstDay.getDay() + 6) % 7
  const gridStart = new Date(month.getFullYear(), month.getMonth(), 1 - mondayOffset)
  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(gridStart)
    date.setDate(gridStart.getDate() + index)
    return { date, currentMonth: date.getMonth() === month.getMonth() }
  })
}

export function ProviderBookingsCalendar() {
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [month, setMonth] = useState(() => {
    const now = new Date()
    return new Date(now.getFullYear(), now.getMonth(), 1)
  })
  const [selectedDate, setSelectedDate] = useState(() => localDateKey(new Date()))

  const loadBookings = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setBookings(await api.providerActiveBookings())
    } catch (cause) {
      setError(cause instanceof Error
        ? cause.message
        : 'No fue posible consultar tus reservas.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let active = true
    api.providerActiveBookings()
      .then((result) => { if (active) setBookings(result) })
      .catch((cause: unknown) => {
        if (active) {
          setError(cause instanceof Error
            ? cause.message
            : 'No fue posible consultar tus reservas.')
        }
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  const selectedBookings = bookings.filter(
    (booking) => booking.date === selectedDate && booking.status === 'activa',
  )
  const monthBookingCount = bookings.filter(
    (booking) => booking.status === 'activa'
      && booking.date.startsWith(
        `${month.getFullYear()}-${String(month.getMonth() + 1).padStart(2, '0')}`,
      ),
  ).length
  const monthLabel = new Intl.DateTimeFormat('es-CO', {
    month: 'long',
    year: 'numeric',
  }).format(month)

  function changeMonth(offset: number) {
    const nextMonth = new Date(month.getFullYear(), month.getMonth() + offset, 1)
    setMonth(nextMonth)
    setSelectedDate(localDateKey(nextMonth))
  }

  function goToToday() {
    const today = new Date()
    setMonth(new Date(today.getFullYear(), today.getMonth(), 1))
    setSelectedDate(localDateKey(today))
  }

  return (
    <section className="panel provider-calendar" aria-labelledby="provider-calendar-title">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">CITAS CONFIRMADAS</span>
          <h3 id="provider-calendar-title">Agenda del proveedor</h3>
        </div>
        <span className="count-chip" aria-label={`${monthBookingCount} reservas este mes`}>
          {monthBookingCount}
        </span>
      </div>
      <Alert>{error}</Alert>
      {loading ? <Loading label="Cargando tus reservas…" /> : (
        <div className="provider-calendar-layout">
          <div className="calendar-main">
            <div className="calendar-toolbar">
              <div className="calendar-month-nav">
                <button className="button button-quiet button-small" onClick={() => changeMonth(-1)} aria-label="Mes anterior">‹</button>
                <h4>{monthLabel}</h4>
                <button className="button button-quiet button-small" onClick={() => changeMonth(1)} aria-label="Mes siguiente">›</button>
              </div>
              <div className="calendar-actions">
                <button className="button button-quiet button-small" onClick={goToToday}>Hoy</button>
                <button className="button button-quiet button-small" onClick={() => void loadBookings()} disabled={loading}>Actualizar</button>
              </div>
            </div>
            <div className="calendar-grid" role="grid" aria-label={`Agenda de ${monthLabel}`}>
              {['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'].map((day) => (
                <span className="calendar-weekday" role="columnheader" key={day}>{day}</span>
              ))}
              {monthDays(month).map(({ date, currentMonth }) => {
                const dateKey = localDateKey(date)
                const dayBookings = bookings.filter(
                  (booking) => booking.date === dateKey && booking.status === 'activa',
                )
                return (
                  <button
                    className={`calendar-day ${currentMonth ? '' : 'outside-month'} ${selectedDate === dateKey ? 'selected' : ''} ${dayBookings.length ? 'has-bookings' : ''}`}
                    key={dateKey}
                    role="gridcell"
                    aria-label={`${date.getDate()} ${new Intl.DateTimeFormat('es-CO', { month: 'long' }).format(date)}, ${dayBookings.length} reservas`}
                    aria-pressed={selectedDate === dateKey}
                    onClick={() => setSelectedDate(dateKey)}
                  >
                    <span>{date.getDate()}</span>
                    {dayBookings.length > 0 && <small>{dayBookings.length} cita{dayBookings.length === 1 ? '' : 's'}</small>}
                  </button>
                )
              })}
            </div>
            <p className="calendar-legend"><span /> Día con reserva confirmada</p>
          </div>
          <aside className="calendar-day-detail" aria-live="polite">
            <span className="eyebrow">
              {new Intl.DateTimeFormat('es-CO', {
                weekday: 'long',
                day: 'numeric',
                month: 'long',
              }).format(new Date(`${selectedDate}T12:00:00`))}
            </span>
            <h4>{selectedBookings.length ? 'Reservas del día' : 'Sin reservas este día'}</h4>
            {selectedBookings.length ? (
              <div className="calendar-booking-list">
                {selectedBookings.map((booking) => (
                  <article className="calendar-booking" key={booking.bookingId}>
                    <span className="calendar-booking-time">
                      {booking.startTime.slice(0, 5)} – {booking.endTime.slice(0, 5)}
                    </span>
                    <strong>{booking.serviceName}</strong>
                    <small>Reserva confirmada</small>
                  </article>
                ))}
              </div>
            ) : (
              <p>Las citas confirmadas aparecerán aquí.</p>
            )}
          </aside>
        </div>
      )}
    </section>
  )
}
