import { useEffect, useState, type FormEvent } from 'react'
import { ApiError, api } from '../api/client'
import type { ScheduleBlock, ServiceOffering } from '../api/types'
import { Alert, EmptyState, Loading } from '../components/Feedback'
import { ProviderBookingsCalendar } from './ProviderBookingsCalendar'

export function SchedulesPage() {
  const [services, setServices] = useState<ServiceOffering[]>([])
  const [selectedService, setSelectedService] = useState('')
  const [schedules, setSchedules] = useState<ScheduleBlock[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  async function loadSchedules(serviceId: string) {
    setSelectedService(serviceId)
    if (!serviceId) {
      setSchedules([])
      return
    }
    setError('')
    try {
      const result = await api.availableSchedules(Number(serviceId))
      setSchedules(result)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No fue posible consultar los bloques.')
    }
  }

  useEffect(() => {
    let active = true
    api.myServices()
      .then((result) => {
        if (active) setServices(result.filter((service) => service.activo !== false))
      })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : 'No fue posible consultar tus servicios.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  async function createSchedule(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formElement = event.currentTarget
    if (!selectedService) {
      setError('Selecciona un servicio antes de crear disponibilidad.')
      return
    }
    const form = new FormData(formElement)
    const horaInicio = String(form.get('horaInicio') ?? '')
    const horaFin = String(form.get('horaFin') ?? '')
    if (horaFin <= horaInicio) {
      setError('La hora de finalización debe ser posterior a la hora de inicio.')
      return
    }
    setError('')
    setNotice('')
    setSaving(true)
    try {
      const block = await api.createSchedule({
        serviceId: Number(selectedService),
        fecha: String(form.get('fecha') ?? ''),
        horaInicio,
        horaFin,
      })
      setNotice(`Bloque creado para ${block.fecha}, de ${block.horaInicio} a ${block.horaFin}.`)
      formElement.reset()
      await loadSchedules(selectedService)
    } catch (cause) {
      setError(cause instanceof ApiError
        ? `${cause.message}${cause.details.length ? ` ${cause.details.join(' ')}` : ''}`
        : 'No fue posible crear la disponibilidad.')
    } finally {
      setSaving(false)
    }
  }

  const [today] = useState(() => new Date().toLocaleDateString('en-CA'))

  return (
    <section className="content-section">
      <div className="page-intro">
        <div><span className="eyebrow">AGENDA DEL PROVEEDOR</span><h2>Publica tu disponibilidad</h2><p>Define bloques concretos para que los clientes puedan reservar.</p></div>
      </div>
      <Alert>{error}</Alert><Alert tone="success">{notice}</Alert>
      <ProviderBookingsCalendar />
      {loading ? <Loading /> : services.length === 0 ? (
        <EmptyState title="Necesitas un servicio activo" description="Crea primero un servicio para poder publicar bloques de disponibilidad." />
      ) : (
        <div className="provider-layout schedule-layout">
          <section className="panel">
            <div className="panel-heading"><div><span className="eyebrow">PUBLICAR</span><h3>Nuevo bloque</h3></div><span className="service-icon">◷</span></div>
            <form className="form-stack" onSubmit={(event) => void createSchedule(event)}>
              <label>Servicio<select required value={selectedService} onChange={(event) => void loadSchedules(event.target.value)}>
                <option value="">Selecciona un servicio</option>
                {services.map((service) => <option key={service.id} value={service.id}>{service.nombre}</option>)}
              </select></label>
              <label>Fecha<input name="fecha" type="date" min={today} required /></label>
              <div className="form-pair">
                <label>Desde<input name="horaInicio" type="time" required /></label>
                <label>Hasta<input name="horaFin" type="time" required /></label>
              </div>
              <button className="button button-primary button-full" disabled={saving}>{saving ? 'Publicando…' : 'Publicar bloque'}</button>
            </form>
            <p className="form-hint">El backend valida que el bloque sea futuro, que pertenezca a tu servicio y que no se solape.</p>
          </section>
          <section className="panel">
            <div className="panel-heading"><div><span className="eyebrow">DISPONIBILIDAD FUTURA</span><h3>Bloques del servicio</h3></div><span className="count-chip">{schedules.length}</span></div>
            {!selectedService ? <EmptyState title="Selecciona un servicio" description="Verás aquí los bloques futuros que devuelve el backend." /> : schedules.length === 0 ? (
              <EmptyState title="Sin bloques futuros" description="Cuando publiques disponibilidad, aparecerá aquí." />
            ) : (
              <div className="schedule-list">
                {schedules.map((schedule) => (
                  <article className="schedule-row" key={schedule.id}>
                    <div className="schedule-date"><span>{new Intl.DateTimeFormat('es-CO', { weekday: 'short' }).format(new Date(`${schedule.fecha}T12:00:00`)).replace('.', '')}</span><strong>{schedule.fecha.slice(8)}</strong></div>
                    <div><strong>{schedule.fecha}</strong><p>{schedule.horaInicio.slice(0, 5)} — {schedule.horaFin.slice(0, 5)}</p></div>
                    <span className="status-pill status-available">Publicado</span>
                  </article>
                ))}
              </div>
            )}
          </section>
        </div>
      )}
    </section>
  )
}
