import { useEffect, useState } from 'react'
import { ApiError, api } from '../api/client'
import type { Provider, ScheduleBlock, ServiceOffering } from '../api/types'
import { Alert, EmptyState, Loading } from '../components/Feedback'

function slotOptions(block: ScheduleBlock, duration: number): string[] {
  const start = block.horaInicio.split(':').map(Number)
  const end = block.horaFin.split(':').map(Number)
  let cursor = start[0] * 60 + start[1]
  const endMinutes = end[0] * 60 + end[1]
  const result: string[] = []
  while (cursor + duration <= endMinutes) {
    result.push(`${String(Math.floor(cursor / 60)).padStart(2, '0')}:${String(cursor % 60).padStart(2, '0')}`)
    cursor += duration
  }
  return result
}

export function ExplorePage() {
  const [tab, setTab] = useState<'services' | 'providers'>('services')
  const [services, setServices] = useState<ServiceOffering[]>([])
  const [providers, setProviders] = useState<Provider[]>([])
  const [providerPage, setProviderPage] = useState(0)
  const [providerTotalPages, setProviderTotalPages] = useState(0)
  const [providerServices, setProviderServices] = useState<ServiceOffering[]>([])
  const [selectedProvider, setSelectedProvider] = useState<Provider | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selected, setSelected] = useState<ServiceOffering | null>(null)
  const [schedules, setSchedules] = useState<ScheduleBlock[]>([])
  const [scheduleLoading, setScheduleLoading] = useState(false)
  const [scheduleError, setScheduleError] = useState('')
  const [selectedSlot, setSelectedSlot] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [notice, setNotice] = useState('')

  async function loadServices() {
    setLoading(true)
    setError('')
    try {
      setServices(await api.availableServices())
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Error al consultar servicios.')
    } finally {
      setLoading(false)
    }
  }

  async function loadProviders(page = 0) {
    setLoading(true)
    setError('')
    try {
      const result = await api.providers(page, 12)
      setProviders(result.items)
      setProviderPage(result.page)
      setProviderTotalPages(result.totalPages)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Error al consultar proveedores.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    api.availableServices()
      .then((result) => { if (active) setServices(result) })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : 'Error al consultar servicios.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  async function showProvider(provider: Provider) {
    setSelectedProvider(provider)
    setProviderServices([])
    setError('')
    setLoading(true)
    try {
      setProviderServices(await api.providerServices(provider.providerId))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Error al consultar servicios del proveedor.')
    } finally {
      setLoading(false)
    }
  }

  async function chooseService(service: ServiceOffering) {
    setSelected(service)
    setSchedules([])
    setSelectedSlot('')
    setNotice('')
    setScheduleError('')
    setScheduleLoading(true)
    try {
      setSchedules(await api.availableSchedules(service.id))
    } catch (cause) {
      setScheduleError(cause instanceof Error ? cause.message : 'Error al consultar horarios.')
    } finally {
      setScheduleLoading(false)
    }
  }

  async function bookSelected() {
    const [blockId, startTime] = selectedSlot.split('|')
    if (!selected || !blockId || !startTime) return
    if (!window.confirm(`¿Confirmas la reserva de ${selected.nombre} el ${selectedSlotLabel}?`)) return
    setSubmitting(true)
    setScheduleError('')
    try {
      const booking = await api.createBooking({
        scheduleBlockId: Number(blockId),
        startTime,
      })
      setNotice(`Reserva #${booking.bookingId} confirmada para ${booking.date} a las ${booking.startTime}.`)
      setSelected(null)
      setSelectedSlot('')
      await loadServices()
    } catch (cause) {
      setScheduleError(cause instanceof ApiError
        ? `${cause.message}${cause.details.length ? ` ${cause.details.join(' ')}` : ''}`
        : 'No fue posible crear la reserva.')
    } finally {
      setSubmitting(false)
    }
  }

  const selectedSlotLabel = selectedSlot
    ? (() => {
        const [blockId, time] = selectedSlot.split('|')
        const block = schedules.find((item) => item.id === Number(blockId))
        return block ? `${block.fecha} a las ${time}` : time
      })()
    : ''

  return (
    <section className="content-section">
      <div className="page-intro">
        <div><span className="eyebrow">DESCUBRE ALGO NUEVO</span><h2>Encuentra tu próximo servicio</h2><p>Explora proveedores y servicios con disponibilidad declarada en la plataforma.</p></div>
        <button className="button button-quiet" onClick={() => tab === 'services' ? void loadServices() : void loadProviders(providerPage)} disabled={loading}>↻ Actualizar</button>
      </div>
      <div className="tab-strip" role="tablist" aria-label="Explorar">
        <button role="tab" aria-selected={tab === 'services'} className={tab === 'services' ? 'selected' : ''} onClick={() => { setTab('services'); setSelectedProvider(null) }}>Servicios</button>
        <button role="tab" aria-selected={tab === 'providers'} className={tab === 'providers' ? 'selected' : ''} onClick={() => { setTab('providers'); setSelectedProvider(null); void loadProviders(0) }}>Proveedores</button>
      </div>
      <Alert>{error}</Alert>
      <Alert tone="success">{notice}</Alert>
      {tab === 'services' && (loading ? <Loading /> : services.length === 0 ? (
        <EmptyState title="Todavía no hay servicios disponibles" description="Cuando un proveedor publique bloques futuros, aparecerán aquí." />
      ) : (
        <div className="service-grid">
          {services.map((service) => (
            <article className="service-card" key={service.id}>
              <div className="service-card-top"><span className="service-icon">✳</span><span className="status-pill status-available">Con horarios</span></div>
              <span className="service-category">{service.nombreProveedor ?? 'Proveedor'}</span>
              <h3>{service.nombre}</h3>
              {service.descripcion && <p>{service.descripcion}</p>}
              <div className="service-card-footer">
                <span>◷ {service.duracionMinutos} min</span>
                <button className="button button-primary button-small" onClick={() => void chooseService(service)}>Ver horarios</button>
              </div>
            </article>
          ))}
        </div>
      ))}
      {tab === 'providers' && !selectedProvider && (loading ? <Loading /> : providers.length === 0 ? (
        <EmptyState title="No hay proveedores registrados" description="Cuando haya negocios en la plataforma, podrás ver su oferta." />
      ) : (
        <>
          <div className="provider-directory">
            {providers.map((provider) => (
              <button className="directory-card" key={provider.providerId} onClick={() => void showProvider(provider)}>
                <span className="service-icon">⌂</span><span><strong>{provider.nombreNegocio}</strong><small>Ver servicios y disponibilidad</small></span><b aria-hidden="true">→</b>
              </button>
            ))}
          </div>
          <div className="pagination">
            <button className="button button-quiet button-small" disabled={providerPage <= 0 || loading} onClick={() => void loadProviders(providerPage - 1)}>Anterior</button>
            <span>Página {providerPage + 1} de {Math.max(providerTotalPages, 1)}</span>
            <button className="button button-quiet button-small" disabled={providerPage + 1 >= providerTotalPages || loading} onClick={() => void loadProviders(providerPage + 1)}>Siguiente</button>
          </div>
        </>
      ))}
      {tab === 'providers' && selectedProvider && (
        <section className="provider-offerings">
          <button className="text-button" onClick={() => setSelectedProvider(null)}>← Todos los proveedores</button>
          <div className="page-intro compact-intro"><div><span className="eyebrow">OFERTA DEL PROVEEDOR</span><h2>{selectedProvider.nombreNegocio}</h2></div></div>
          {loading ? <Loading /> : providerServices.length === 0 ? (
            <EmptyState title="Sin servicios disponibles" description="Este proveedor todavía no tiene servicios activos con horarios futuros." />
          ) : (
            <div className="service-grid">
              {providerServices.map((service) => (
                <article className="service-card" key={service.id}>
                  <div className="service-card-top"><span className="service-icon">✳</span><span className="status-pill status-available">Con horarios</span></div>
                  <span className="service-category">{selectedProvider.nombreNegocio}</span><h3>{service.nombre}</h3>
                  <div className="service-card-footer"><span>◷ {service.duracionMinutos} min</span><button className="button button-primary button-small" onClick={() => void chooseService({ ...service, nombreProveedor: selectedProvider.nombreNegocio })}>Ver horarios</button></div>
                </article>
              ))}
            </div>
          )}
        </section>
      )}

      {selected && (
        <div className="modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setSelected(null) }}>
          <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="booking-title">
            <button className="modal-close" onClick={() => setSelected(null)} aria-label="Cerrar">×</button>
            <span className="eyebrow">RESERVAR SERVICIO</span>
            <h2 id="booking-title">{selected.nombre}</h2>
            <p className="modal-subtitle">{selected.nombreProveedor} · {selected.duracionMinutos} minutos</p>
            <Alert>{scheduleError}</Alert>
            {scheduleLoading ? <Loading label="Buscando bloques futuros…" /> : schedules.length === 0 ? (
              <EmptyState title="No hay bloques futuros" description="El proveedor no tiene horarios declarados para este servicio." />
            ) : (
              <>
                <p className="field-label">Elige una hora disponible</p>
                <div className="schedule-options">
                  {schedules.flatMap((block) => slotOptions(block, selected.duracionMinutos).map((time) => (
                    <label className={`slot-option ${selectedSlot === `${block.id}|${time}` ? 'selected' : ''}`} key={`${block.id}-${time}`}>
                      <input
                        type="radio"
                        name="booking-slot"
                        value={`${block.id}|${time}`}
                        checked={selectedSlot === `${block.id}|${time}`}
                        onChange={(event) => setSelectedSlot(event.target.value)}
                      />
                      <span><strong>{block.fecha}</strong><small>{time} — {block.horaFin}</small></span>
                    </label>
                  )))}
                </div>
                <p className="form-hint">Los bloques representan disponibilidad declarada. Si alguien reservó el horario recientemente, el backend responderá con un conflicto.</p>
              </>
            )}
            <button className="button button-primary button-full" disabled={!selectedSlot || submitting || scheduleLoading} onClick={() => void bookSelected()}>
              {submitting ? 'Confirmando…' : 'Confirmar reserva'}
            </button>
          </section>
        </div>
      )}
    </section>
  )
}
