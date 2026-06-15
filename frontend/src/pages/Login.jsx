import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

// ROLE_HOME: a qué ruta redirigir después del login según el rol devuelto por el backend.
const ROLE_HOME = {
  USUARIO_GENERAL: '/eventos',
  FUNCIONARIO:     '/validar',
  ADMINISTRADOR:   '/admin/estadios',
}

export default function Login() {
  const { login } = useAuth()
  const navigate  = useNavigate()

  const [form, setForm]     = useState({ mail: '', contrasena: '' })
  const [error, setError]   = useState('')
  const [loading, setLoading] = useState(false)

  // change: actualiza el campo del formulario cuyo name coincide con e.target.name y limpia el error.
  function change(e) {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))
    setError('')
  }

  // submit: valida campos, llama al contexto de auth y redirige al home del rol si el login es exitoso.
  async function submit(e) {
    e.preventDefault()
    if (!form.mail || !form.contrasena) {
      setError('Completá todos los campos.')
      return
    }
    setLoading(true)
    try {
      const user = await login(form.mail, form.contrasena)
      navigate(ROLE_HOME[user.rol] ?? '/')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="center-page">
      <div className="card card-auth">
        <h1>Iniciar sesión</h1>
        <p className="subtitle">Accedé a tu cuenta del Mundial 2026</p>

        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={submit} noValidate>
          <div className="form-group">
            <label htmlFor="mail">Correo electrónico</label>
            <input
              id="mail"
              name="mail"
              type="email"
              placeholder="tu@email.com"
              value={form.mail}
              onChange={change}
              autoComplete="email"
              autoFocus
            />
          </div>

          <div className="form-group">
            <label htmlFor="contrasena">Contraseña</label>
            <input
              id="contrasena"
              name="contrasena"
              type="password"
              placeholder="••••••••"
              value={form.contrasena}
              onChange={change}
              autoComplete="current-password"
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-full"
            style={{ marginTop: 8 }}
            disabled={loading}
          >
            {loading ? 'Ingresando…' : 'Ingresar'}
          </button>
        </form>

        <div className="divider">o</div>

        <p className="text-center text-muted">
          ¿No tenés cuenta?{' '}
          <Link to="/registro">Registrate</Link>
        </p>
      </div>
    </div>
  )
}
