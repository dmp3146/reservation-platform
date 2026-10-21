import { useEffect, useState, type FormEvent } from 'react'
import { ApiError, api } from '../api/client'
import type { ServiceCreatePayload, ServiceOffering } from '../api/types'
import { Alert, EmptyState, Loading } from '../components/Feedback'

export function ProviderPage() {
  const [services, setServices] = useState<ServiceOffering[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function loadServices() {
    setLoading(true)
    setError('')
    try {
      setServices(await api.myServices())
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No fue posible consultar tus servicios.')
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => {
    let active = true
    api.myServices()
      .then((result) => { if (active) setServices(result) })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : 'No fue posible consultar tus servicios.')
      })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  async function createService(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formElement = event.currentTarget
    setError('')
    setNotice('')
    setSubmitting(true)
    const form = new FormData(formElement)
    const payload: ServiceCreatePayload = {
      nombre: String(form.get('nombre') ?? '').trim(),
      descripcion: String(form.get('descripcion') ?? '').trim(),
      duracionMinutos: Number(form.get('duracionMinutos')),
    }
    try {
      const created = await api.createService(payload)
      setNotice(`El servicio “${created.nombre}” fue creado correctamente.`)
      formElement.reset()
      await loadServices()
    } catch (cause) {
      setError(cause instanceof ApiError
        ? `${cause.message}${cause.details.length ? ` ${cause.details.join(' ')}` : ''}`
        : 'No fue posible crear el servicio.')
    } finally {
      setSubmitting(false)
    }
  }

  async function retireService(service: ServiceOffering) {
    if (!window.confirm(`¿Retirar “${service.nombre}” de tu oferta?`)) return
    setError('')
    setNotice('')
    try {
      await api.deleteService(service.id)
      setNotice(`“${service.nombre}” fue retirado de la oferta.`)
      await loadServices()
    } catch (cause) {
      setError(cause instanceof ApiError
        ? `${cause.message}${cause.details.length ? ` ${cause.details.join(' ')}` : ''}`
        : 'No fue posible retirar el servicio.')
    }
  }

  return (
    <section className="content-section">
      <div className="page-intro">
        <div><span className="eyebrow">TU NEGOCIO</span><h2>Gestiona tu oferta</h2><p>Crea servicios que tus clientes puedan descubrir y reservar.</p></div>
        <button className="button button-quiet" onClick={() => void loadServices()} disabled={loading}>↻ Actualizar</button>
      </div>
      <Alert>{error}</Alert><Alert tone="success">{notice}</Alert>
      <div className="provider-layout">
        <section className="panel">
          <div className="panel-heading"><div><span className="eyebrow">CATÁLOGO</span><h3>Mis servicios</h3></div><span className="count-chip">{services.length}</span></div>
          {loading ? <Loading /> : services.length === 0 ? <EmptyState title="Aún no tienes servicios" description="Crea tu primera opción para empezar." /> : (
            <div className="provider-service-list">
              {services.map((service) => (
                <article className="provider-service" key={service.id}>
                  <span className="service-icon">✳</span>
                  <div className="provider-service-copy"><h4>{service.nombre}</h4><p>{service.descripcion || 'Sin descripción'}</p><small>{service.duracionMinutos} minutos · {service.activo === false ? 'Retirado' : 'Activo'}</small></div>
                  {service.activo !== false && <button className="button button-danger-quiet button-small" onClick={() => void retireService(service)}>Retirar</button>}
                </article>
              ))}
            </div>
          )}
        </section>
        <section className="panel create-panel">
          <div className="panel-heading"><div><span className="eyebrow">NUEVA OFERTA</span><h3>Crear servicio</h3></div></div>
          <form className="form-stack" onSubmit={(event) => void createService(event)}>
            <label>Nombre del servicio<input name="nombre" required maxLength={150} placeholder="Ej. Consulta inicial" /></label>
            <label>Descripción<textarea name="descripcion" required rows={4} placeholder="Cuenta qué incluye este servicio" /></label>
            <label>Duración <span className="optional-label">minutos</span><input name="duracionMinutos" type="number" min={1} required /></label>
            <button className="button button-primary button-full" disabled={submitting}>{submitting ? 'Guardando…' : 'Crear servicio'}</button>
          </form>
        </section>
      </div>
      <p className="form-hint below-panels">El backend puede rechazar el retiro de servicios con reservas activas. Los permisos se validan siempre en el servidor.</p>
    </section>
  )
}
