import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

const EMPTY = {
  mail: '', contrasena: '',
  paisDoc: 'Uruguay', tipoDoc: 'CI', nroDoc: '',
  paisDir: 'Uruguay', localidad: '', calle: '', nroPuerta: '', codPostal: '',
}

export default function Register() {
  const { register } = useAuth()
  const navigate      = useNavigate()

  const [form, setForm]     = useState(EMPTY)
  const [error, setError]   = useState('')
  const [loading, setLoading] = useState(false)

  function change(e) {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))
    setError('')
  }

  async function submit(e) {
    e.preventDefault()
    const required = ['mail','contrasena','paisDoc','tipoDoc','nroDoc',
                      'paisDir','localidad','calle','nroPuerta','codPostal']
    const empty = required.find(k => !form[k]?.trim())
    if (empty) { setError('Completá todos los campos.'); return }

    setLoading(true)
    try {
      await register(form)
      navigate('/eventos')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const f = (name, label, opts = {}) => (
    <div className="form-group">
      <label htmlFor={name}>{label}</label>
      <input
        id={name} name={name}
        value={form[name]} onChange={change}
        {...opts}
      />
    </div>
  )

  const s = (name, label, options) => (
    <div className="form-group">
      <label htmlFor={name}>{label}</label>
      <select id={name} name={name} value={form[name]} onChange={change}>
        {options.map(o => <option key={o} value={o}>{o}</option>)}
      </select>
    </div>
  )

  return (
    <div className="center-page">
      <div className="card card-auth" style={{ maxWidth: 520 }}>
        <h1>Crear cuenta</h1>
        <p className="subtitle">Registrate para comprar entradas al Mundial 2026</p>

        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={submit} noValidate>
          {/* Acceso */}
          <div className="form-section-title">Datos de acceso</div>
          {f('mail',       'Correo electrónico', { type: 'email', placeholder: 'tu@email.com', autoComplete: 'email' })}
          {f('contrasena', 'Contraseña',         { type: 'password', placeholder: 'Mínimo 6 caracteres' })}

          {/* Documento */}
          <div className="form-section-title">Documento</div>
          <div className="form-row">
            {s('paisDoc', 'País emisor', ['Uruguay','Argentina','Brasil','Paraguay','Chile','Otro'])}
            {s('tipoDoc', 'Tipo',        ['CI','Pasaporte','Otro'])}
          </div>
          {f('nroDoc', 'Número de documento', { placeholder: 'Ej: 12345678' })}

          {/* Dirección */}
          <div className="form-section-title">Dirección</div>
          {s('paisDir', 'País',
            ['Uruguay','Argentina','Brasil','Paraguay','Chile','Bolivia','Perú','Colombia','Otro'])}
          <div className="form-row">
            {f('localidad', 'Localidad / Ciudad', { placeholder: 'Montevideo' })}
            {f('codPostal', 'Código postal',       { placeholder: '11000' })}
          </div>
          <div className="form-row">
            {f('calle',    'Calle',   { placeholder: 'Av. 18 de Julio' })}
            {f('nroPuerta','Número',  { placeholder: '1234' })}
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-full"
            style={{ marginTop: 12 }}
            disabled={loading}
          >
            {loading ? 'Creando cuenta…' : 'Crear cuenta'}
          </button>
        </form>

        <div className="divider">o</div>

        <p className="text-center text-muted">
          ¿Ya tenés cuenta?{' '}
          <Link to="/login">Iniciar sesión</Link>
        </p>
      </div>
    </div>
  )
}
