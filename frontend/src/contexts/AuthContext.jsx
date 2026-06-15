import { createContext, useContext, useState, useCallback } from 'react'
import { api } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null) // { mail, rol }

  // login: llama al backend, guarda { mail, rol } en el contexto global y devuelve el usuario.
  const login = useCallback(async (mail, contrasena) => {
    const data = await api.login(mail, contrasena)
    setUser(data)
    return data
  }, [])

  // logout: cierra sesión en el backend (invalida JSESSIONID) y limpia el usuario del contexto.
  const logout = useCallback(async () => {
    try { await api.logout() } catch (_) {}
    setUser(null)
  }, [])

  // register: crea el USUARIO_GENERAL en el backend y lo deja logueado automáticamente.
  const register = useCallback(async (body) => {
    const data = await api.register(body)
    setUser(data)
    return data
  }, [])

  return (
    <AuthContext.Provider value={{ user, login, logout, register }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
