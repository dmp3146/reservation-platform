import { useEffect, useState } from 'react'
import { ApiError, api } from '../api/client'
import type { Booking, BookingDetail } from '../api/types'
import { Alert, EmptyState, Loading } from '../components/Feedback'

function readableDate(date: string): string {
  return new Intl.DateTimeFormat('es-CO', { dateStyle: 'long' }).format(new Date(`${date}T12:00:00`))
}

export function BookingsPage() {
  const [tab, setTab] = useState<'active' | 'history'>('active')
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [detail, setDetail] = useState<BookingDetail | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)

  async function loadBookings(target = tab) {
    setLoading(true)
    setError('')
    try {
      setBookings(target === 'active' ? await api.activeBookings() : await api.bookingHistory())
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No fue posible consultar tus reservas.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    const query = tab === 'active' ? api.activeBookings() : api.bookingHistory()
    query
      .then((result) => { if (active) setBookings(result) })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : 'No fue posible consultar tus reservas.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [tab])

  async function openDetail(id: number) {
    setError('')
    try {
      setDetail(await api.bookingDetail(id))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No fue posible consultar el detalle.')
    }
  }

  async function cancelBooking(booking: Booking) {
    if (!window.confirm(`¿Deseas cancelar la reserva #${booking.bookingId}?`)) return
    setBusyId(booking.bookingId)
    setError('')
    setNotice('')
    try {
      const result = await api.cancelBooking(booking.bookingId)
      setNotice(`La reserva #${result.bookingId} quedó ${result.status}.`)
      await loadBookings(tab)
      if (detail?.bookingId === result.bookingId) setDetail(null)
    } catch (cause) {
      setError(cause instanceof ApiError
        ? `${cause.message}${cause.details.length ? ` ${cause.details.join(' ')}` : ''}`
        : 'No fue posible cancelar la reserva.')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <section className="content-section">
      <div className="page-intro">
        <div><span className="eyebrow">TU AGENDA</span><h2>Citas y actividad</h2><p>Consulta el estado de tus reservas y revisa cada detalle.</p></div>
        <button className="button button-quiet" onClick={() => void loadBookings()} disabled={loading}>↻ Actualizar</button>
      </div>
      <div className="tab-strip" role="tablist" aria-label="Reservas">
        <button role="tab" aria-selected={tab === 'active'} className={tab === 'active' ? 'selected' : ''} onClick={() => { setError(''); setNotice(''); setTab('active') }}>Próximas</button>
        <button role="tab" aria-selected={tab === 'history'} className={tab === 'history' ? 'selected' : ''} onClick={() => { setError(''); setNotice(''); setTab('history') }}>Historial</button>
      </div>
      <Alert>{error}</Alert>
      <Alert tone="success">{notice}</Alert>
      {loading ? <Loading /> : bookings.length === 0 ? (
        <EmptyState title={tab === 'active' ? 'No tienes reservas activas' : 'Tu historial está vacío'} description={tab === 'active' ? 'Cuando reserves un servicio, tu próxima cita aparecerá aquí.' : 'Las reservas canceladas y completadas aparecerán aquí.'} />
      ) : (
        <div className="booking-list">
          {bookings.map((booking) => (
            <article className="booking-row" key={booking.bookingId}>
              <div className="booking-date-block"><span>{new Intl.DateTimeFormat('es-CO', { month: 'short' }).format(new Date(`${booking.date}T12:00:00`)).replace('.', '')}</span><strong>{booking.date.slice(-2)}</strong></div>
              <div className="booking-main">
                <div className="booking-title-row"><h3>{booking.serviceName}</h3><span className={`status-pill status-${booking.status}`}>{booking.status}</span></div>
                <p>{booking.providerName} · {readableDate(booking.date)}</p>
                <span className="booking-time">◷ {booking.startTime.slice(0, 5)} – {booking.endTime.slice(0, 5)}</span>
              </div>
              <div className="booking-actions">
                <button className="button button-quiet button-small" onClick={() => void openDetail(booking.bookingId)}>Ver detalle</button>
                {tab === 'active' && booking.status === 'activa' && (
                  <button className="button button-danger-quiet button-small" disabled={busyId === booking.bookingId} onClick={() => void cancelBooking(booking)}>
                    {busyId === booking.bookingId ? 'Cancelando…' : 'Cancelar'}
                  </button>
                )}
              </div>
            </article>
          ))}
        </div>
      )}
      {detail && (
        <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setDetail(null) }}>
          <section className="modal-card detail-card" role="dialog" aria-modal="true" aria-labelledby="detail-title">
            <button className="modal-close" onClick={() => setDetail(null)} aria-label="Cerrar">×</button>
            <span className="eyebrow">RESERVA #{detail.bookingId}</span>
            <h2 id="detail-title">{detail.serviceName}</h2>
            <dl className="detail-grid">
              <div><dt>Proveedor</dt><dd>{detail.providerName}</dd></div>
              <div><dt>Cliente</dt><dd>{detail.customerName}</dd></div>
              <div><dt>Fecha</dt><dd>{readableDate(detail.date)}</dd></div>
              <div><dt>Horario</dt><dd>{detail.startTime.slice(0, 5)} – {detail.endTime.slice(0, 5)}</dd></div>
              <div><dt>Estado</dt><dd><span className={`status-pill status-${detail.status}`}>{detail.status}</span></dd></div>
            </dl>
          </section>
        </div>
      )}
    </section>
  )
}
