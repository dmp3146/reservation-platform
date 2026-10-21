import { useState, type FormEvent } from 'react'
import { ApiError, api } from '../api/client'
import type { OccupancyReport } from '../api/types'
import { Alert, EmptyState, Loading } from '../components/Feedback'

function todayString() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

export function ReportsPage() {
  const today = todayString()
  const [startDate, setStartDate] = useState(today)
  const [endDate, setEndDate] = useState(today)
  const [report, setReport] = useState<OccupancyReport | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function generateReport(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setReport(null)
    if (startDate > endDate) {
      setError('La fecha inicial no puede ser posterior a la fecha final.')
      return
    }
    setLoading(true)
    try {
      setReport(await api.occupancyReport(startDate, endDate))
    } catch (cause) {
      setError(cause instanceof ApiError
        ? `${cause.message}${cause.details.length ? ` ${cause.details.join(' ')}` : ''}`
        : 'No fue posible generar el reporte.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="content-section">
      <div className="page-intro">
        <div><span className="eyebrow">DATOS DEL NEGOCIO</span><h2>Ocupación y uso</h2><p>Consulta agregados reales de reservas en un periodo.</p></div>
      </div>
      <section className="panel report-filter">
        <form className="report-form" onSubmit={(event) => void generateReport(event)}>
          <label>Desde<input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} required /></label>
          <span className="date-divider">hasta</span>
          <label>Hasta<input type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} required /></label>
          <button className="button button-primary" disabled={loading}>{loading ? 'Generando…' : 'Generar reporte'}</button>
        </form>
      </section>
      <Alert>{error}</Alert>
      {loading && <Loading label="Calculando métricas del periodo…" />}
      {report && (
        <>
          <div className="metric-grid">
            <article className="metric-card metric-primary"><span>RESERVAS TOTALES</span><strong>{report.totalBookings}</strong><small>En el periodo seleccionado</small><i aria-hidden="true">▦</i></article>
            <article className="metric-card"><span>EFECTIVAS</span><strong>{report.effectiveBookings}</strong><small>Activas y completadas</small><i aria-hidden="true">↗</i></article>
            <article className="metric-card"><span>CANCELADAS</span><strong>{report.cancelledBookings}</strong><small>Canceladas en el periodo</small><i aria-hidden="true">↘</i></article>
            <article className="metric-card"><span>SIN ATRIBUCIÓN A SERVICIO</span><strong>{report.bookingsWithoutServiceAttribution}</strong><small>Según el modelo actual</small><i aria-hidden="true">○</i></article>
          </div>
          {!report.hasBookings ? <EmptyState title="Sin reservas en este periodo" description="Prueba otro rango para consultar actividad." /> : (
            <div className="report-tables">
              <section className="panel">
                <div className="panel-heading"><div><span className="eyebrow">DEMANDA</span><h3>Por servicio</h3></div></div>
                {report.services.length === 0 ? <p className="muted-note">No hay reservas atribuibles a servicios en este periodo.</p> : (
                  <div className="table-scroll"><table><thead><tr><th>Servicio</th><th>Efectivas</th><th>Canceladas</th><th>Minutos</th></tr></thead><tbody>
                    {report.services.map((metric) => <tr key={metric.serviceId}><td>{metric.serviceName}</td><td>{metric.effectiveBookings}</td><td>{metric.cancelledBookings}</td><td>{metric.reservedMinutes}</td></tr>)}
                  </tbody></table></div>
                )}
              </section>
              <section className="panel">
                <div className="panel-heading"><div><span className="eyebrow">CAPACIDAD</span><h3>Por recurso</h3></div></div>
                {!report.resourceMetricsAvailable && <Alert tone="info">{report.resourceMetricsLimitation}</Alert>}
                {report.resources.length === 0 ? <p className="muted-note">{report.resourceMetricsLimitation}</p> : (
                  <div className="table-scroll"><table><thead><tr><th>Recurso</th><th>Reservas efectivas</th></tr></thead><tbody>
                    {report.resources.map((metric) => <tr key={metric.resourceId}><td>{metric.resourceName}</td><td>{metric.effectiveBookings}</td></tr>)}
                  </tbody></table></div>
                )}
              </section>
            </div>
          )}
          <p className="report-footnote">Periodo consultado: {report.startDate} — {report.endDate}. Las métricas corresponden a la respuesta del backend.</p>
        </>
      )}
      {!report && !loading && !error && <EmptyState title="Elige un periodo" description="Genera el reporte para ver las métricas reales de ocupación." />}
    </section>
  )
}
