import { createContext, useContext, useState, useCallback, useEffect } from 'react'
import { api } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user,    setUser]    = useState(null)  // { mail, rol }
  const [loading, setLoading] = useState(true)  // true mientras se verifica la sesión al montar

  // Al montar: consulta /auth/yo para restaurar la sesión si la cookie JSESSIONID sigue vigente.
  // Esto evita que recargar la página cierre la sesión.
  useEffect(() => {
    api.get('/auth/yo')
      .then(data => setUser(data))
      .catch(() => {})          // 401 = sin sesión activa, user queda null
      .finally(() => setLoading(false))
  }, [])

  const login = useCallback(async (mail, contrasena) => {
    const data = await api.login(mail, contrasena)
    setUser(data)
    return data
  }, [])

  const logout = useCallback(async () => {
    try { await api.logout() } catch (_) {}
    setUser(null)
  }, [])

  const register = useCallback(async (body) => {
    const data = await api.register(body)
    setUser(data)
    return data
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, register }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
