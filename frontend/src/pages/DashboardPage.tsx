import type { AppPage } from '../components/AppShell'
import { useAuth } from '../auth/useAuth'

export function DashboardPage({ onNavigate }: { onNavigate(page: AppPage): void }) {
  const { user } = useAuth()
  const role = user?.role
  const cards = role === 'cliente'
    ? [
        { page: 'explore' as const, number: '01', title: 'Encuentra tu próximo servicio', text: 'Explora proveedores y horarios declarados disponibles.', cta: 'Explorar servicios', tint: 'mint' },
        { page: 'bookings' as const, number: '02', title: 'Organiza tu agenda', text: 'Consulta tus próximas citas y el historial de reservas.', cta: 'Ver mis reservas', tint: 'lavender' },
      ]
    : role === 'proveedor'
      ? [
          { page: 'provider' as const, number: '01', title: 'Presenta tus servicios', text: 'Crea y administra la oferta de tu negocio.', cta: 'Gestionar servicios', tint: 'mint' },
          { page: 'schedules' as const, number: '02', title: 'Define tus horarios', text: 'Publica bloques futuros para que tus clientes puedan reservar.', cta: 'Administrar disponibilidad', tint: 'lavender' },
        ]
      : [
          { page: 'reports' as const, number: '01', title: 'Entiende tu operación', text: 'Consulta el uso de reservas en un rango de fechas.', cta: 'Ver reportes', tint: 'mint' },
        ]

  return (
    <div className="dashboard">
      <section className="welcome-banner">
        <div className="welcome-copy">
          <span className="eyebrow">TU ESPACIO DE RESERVAS</span>
          <h2>Hola, {user?.name.split(' ')[0]}<span className="wave">✳</span></h2>
          <p>
            {role === 'cliente'
              ? 'Un buen plan empieza por encontrar el momento adecuado.'
              : role === 'proveedor'
              ? 'Administra tus servicios y publica bloques de disponibilidad.'
                : 'Los datos reales de tu plataforma, en un solo lugar.'}
          </p>
        </div>
        <div className="welcome-art" aria-hidden="true">
          <div className="art-sun" />
          <div className="art-ring ring-one" />
          <div className="art-ring ring-two" />
          <div className="art-calendar"><span>AGENDA</span><strong>✳</strong><i>● ● ●</i></div>
        </div>
      </section>

      <div className="section-heading">
        <div><span className="eyebrow">CONTINÚA DONDE LO DEJASTE</span><h2>Accesos rápidos</h2></div>
      </div>
      <section className="quick-grid">
        {cards.map((card) => (
          <article className="quick-card" key={card.page}>
            <div className={`quick-illustration ${card.tint}`}>
              <span className="illustration-number">{card.number}</span>
              <span className="illustration-symbol" aria-hidden="true">{card.tint === 'mint' ? '✳' : '◷'}</span>
            </div>
            <div className="quick-card-body">
              <h3>{card.title}</h3>
              <p>{card.text}</p>
              <button className="inline-link" onClick={() => onNavigate(card.page)}>{card.cta}<span aria-hidden="true"> →</span></button>
            </div>
          </article>
        ))}
      </section>

      <section className="tip-banner">
        <div className="tip-icon">i</div>
        <div><strong>Conectado a tu backend</strong><p>Las consultas y operaciones de esta plataforma se realizan contra la API real.</p></div>
      </section>
    </div>
  )
}
