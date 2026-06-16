import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import Layout   from './components/Layout'
import Login    from './pages/Login'
import Register from './pages/Register'
import Eventos       from './pages/Eventos'
import MisCosas      from './pages/MisCosas'
import Validar       from './pages/Validar'
import AdminEstadios from './pages/AdminEstadios'
import AdminEventos  from './pages/AdminEventos'
import AdminReportes from './pages/AdminReportes'

function RequireAuth({ roles, children }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.rol)) return <Navigate to="/" replace />
  return children
}

function RoleHome() {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  const homes = { USUARIO_GENERAL: '/eventos', FUNCIONARIO: '/validar', ADMINISTRADOR: '/admin/estadios' }
  return <Navigate to={homes[user.rol] ?? '/login'} replace />
}

function AppRoutes() {
  return (
    <Layout>
      <Routes>
        <Route path="/login"    element={<Login />} />
        <Route path="/registro" element={<Register />} />
        <Route path="/"         element={<RoleHome />} />

        {/* USUARIO_GENERAL */}
        <Route path="/eventos" element={
          <RequireAuth><Eventos /></RequireAuth>
        } />
        <Route path="/mis-compras" element={
          <RequireAuth roles={['USUARIO_GENERAL']}><MisCosas /></RequireAuth>
        } />
        <Route path="/mis-entradas" element={
          <RequireAuth roles={['USUARIO_GENERAL']}><MisCosas /></RequireAuth>
        } />
        <Route path="/mis-transferencias" element={
          <RequireAuth roles={['USUARIO_GENERAL']}><MisCosas /></RequireAuth>
        } />

        {/* FUNCIONARIO */}
        <Route path="/validar" element={
          <RequireAuth roles={['FUNCIONARIO']}>
            <Validar />
          </RequireAuth>
        } />

        {/* ADMINISTRADOR */}
        <Route path="/admin/estadios" element={
          <RequireAuth roles={['ADMINISTRADOR']}><AdminEstadios /></RequireAuth>
        } />
        <Route path="/admin/eventos" element={
          <RequireAuth roles={['ADMINISTRADOR']}><AdminEventos /></RequireAuth>
        } />
        <Route path="/admin/reportes" element={
          <RequireAuth roles={['ADMINISTRADOR']}><AdminReportes /></RequireAuth>
        } />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  )
}
