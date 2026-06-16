import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

const NAV = {
  USUARIO_GENERAL: [
    { to: '/eventos',          label: 'Eventos' },
    { to: '/mis-compras',      label: 'Mis compras' },
    { to: '/mis-entradas',     label: 'Mis entradas' },
    { to: '/mis-transferencias', label: 'Transferencias' },
  ],
  FUNCIONARIO: [
    { to: '/validar', label: 'Validar ingreso' },
  ],
  ADMINISTRADOR: [
    { to: '/admin/estadios', label: 'Estadios' },
    { to: '/admin/eventos',  label: 'Eventos' },
    { to: '/admin/reportes', label: 'Reportes' },
  ],
}

export default function Layout({ children }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const links = user ? (NAV[user.rol] ?? []) : []

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <>
      <header className="app-header">
        <div className="header-logo">
          🏆 MUNDIAL <span>2026</span>
          <span className="logo-sub">Ticketing</span>
        </div>

        {user && (
          <nav className="header-nav">
            {links.map(l => (
              <NavLink
                key={l.to}
                to={l.to}
                className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}
              >
                {l.label}
              </NavLink>
            ))}
          </nav>
        )}

        {user && (
          <div className="header-user">
            <span>{user.mail}</span>
            <span className="rol-badge">{user.rol.replace('_', ' ')}</span>
            <button
              className="btn btn-secondary btn-sm"
              style={{ borderColor: 'rgba(255,255,255,.25)', color: '#cbd5e1', background: 'transparent' }}
              onClick={handleLogout}
            >
              Salir
            </button>
          </div>
        )}
      </header>

      <main className="main-content">
        {children}
      </main>
    </>
  )
}
