import { useState, type FormEvent } from 'react'
import { ApiError, api } from '../api/client'
import type { ProviderRegisterPayload, RegisterPayload } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { Alert } from '../components/Feedback'

export function AccessPage({
  sessionError,
  onRetry,
}: {
  sessionError?: string | null
  onRetry?: () => void
}) {
  const { signIn } = useAuth()
  const [mode, setMode] = useState<'login' | 'customer' | 'provider'>('login')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const formElement = event.currentTarget
    setError('')
    setNotice('')
    setSubmitting(true)
    const form = new FormData(formElement)
    const email = String(form.get('email') ?? '').trim()
    const password = String(form.get('password') ?? '')

    try {
      if (mode === 'login') {
        await signIn({ email, password })
        return
      }

      if (password !== String(form.get('confirmPassword') ?? '')) {
        setError('Las contraseñas no coinciden.')
        return
      }

      const common: RegisterPayload = {
        name: String(form.get('name') ?? '').trim(),
        email,
        password,
        confirmPassword: String(form.get('confirmPassword') ?? ''),
      }
      if (mode === 'customer') {
        await api.registerCustomer(common)
      } else {
        const payload: ProviderRegisterPayload = {
          ...common,
          businessName: String(form.get('businessName') ?? '').trim(),
          phone: String(form.get('phone') ?? '').trim(),
        }
        await api.registerProvider(payload)
      }
      setNotice('Cuenta creada. Ya puedes iniciar sesión.')
      setMode('login')
      formElement.reset()
    } catch (cause) {
      setError(
        cause instanceof ApiError
          ? [cause.message, ...cause.details].join(' ')
          : 'No fue posible completar la operación.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="access-page">
      <section className="access-story">
        <div className="brand brand-light">
          <span className="brand-mark">r</span>
          <span>reserva<span className="brand-dot">.</span></span>
        </div>
        <div className="story-copy">
          <span className="eyebrow">TIEMPO PARA LO QUE IMPORTA</span>
          <h1>Tu próxima cita, a un paso.</h1>
          <p>Encuentra servicios, elige un horario y deja que nosotros nos encarguemos del resto.</p>
        </div>
        <div className="story-note">
          <span className="story-stars">✳</span>
          <span>Una plataforma universitaria conectada a reservas reales.</span>
        </div>
      </section>

      <section className="access-form-wrap">
        <div className="access-form-card">
          <div className="mobile-brand brand">
            <span className="brand-mark">r</span>
            <span>reserva<span className="brand-dot">.</span></span>
          </div>
          <div className="access-heading">
            <span className="eyebrow">{mode === 'login' ? 'BIENVENIDO DE NUEVO' : 'ÚNETE A RESERVA'}</span>
            <h2>
              {mode === 'login'
                ? 'Inicia sesión'
                : mode === 'customer'
                  ? 'Crea tu cuenta'
                  : 'Registra tu negocio'}
            </h2>
            <p>
              {mode === 'login'
                ? 'Ingresa tus datos para continuar.'
                : 'Completa tus datos para empezar.'}
            </p>
          </div>
          {sessionError && (
            <div className="session-error-row">
              <Alert>{sessionError}</Alert>
              <button className="button button-quiet button-small" onClick={onRetry}>Reintentar</button>
            </div>
          )}
          <Alert>{error}</Alert>
          <Alert tone="success">{notice}</Alert>
          <form className="form-stack" onSubmit={handleSubmit}>
            {mode !== 'login' && (
              <label>
                Nombre completo
                <input name="name" autoComplete="name" required maxLength={150} />
              </label>
            )}
            <label>
              Correo electrónico
              <input name="email" type="email" autoComplete="email" required maxLength={150} />
            </label>
            {mode === 'provider' && (
              <>
                <label>
                  Nombre del negocio
                  <input name="businessName" required maxLength={150} />
                </label>
                <label>
                  Teléfono <span className="optional-label">Opcional</span>
                  <input name="phone" type="tel" maxLength={30} />
                </label>
              </>
            )}
            <label>
              Contraseña
              <input
                name="password"
                type="password"
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                required
                minLength={8}
              />
              {mode !== 'login' && <small>Mínimo 8 caracteres; máximo 72 bytes UTF-8.</small>}
            </label>
            {mode !== 'login' && (
              <label>
                Confirmar contraseña
                <input name="confirmPassword" type="password" autoComplete="new-password" required />
              </label>
            )}
            <button className="button button-primary button-full" disabled={submitting}>
              {submitting
                ? 'Procesando…'
                : mode === 'login'
                  ? 'Entrar a mi cuenta'
                  : 'Crear cuenta'}
            </button>
          </form>
          <div className="access-switch">
            {mode === 'login' ? (
              <>
                <span>¿Primera vez aquí?</span>
                <button className="text-button" onClick={() => { setMode('customer'); setError(''); setNotice('') }}>
                  Crear cuenta de cliente
                </button>
                <button className="text-button" onClick={() => { setMode('provider'); setError(''); setNotice('') }}>
                  Registrar negocio
                </button>
              </>
            ) : (
              <button className="text-button" onClick={() => { setMode('login'); setError(''); setNotice('') }}>
                Ya tengo una cuenta · Iniciar sesión
              </button>
            )}
          </div>
          <p className="security-note">Conexión protegida por sesión segura y verificación CSRF.</p>
        </div>
      </section>
    </main>
  )
}
