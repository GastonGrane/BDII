const BASE = '/api'

async function request(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  })

  if (res.status === 204 || res.headers.get('content-length') === '0') return null

  const text = await res.text()
  const data = text ? JSON.parse(text) : null

  if (!res.ok) {
    const msg = data?.error || `Error ${res.status}`
    const err = new Error(msg)
    err.status = res.status
    throw err
  }

  return data
}

export const api = {
  get:    (path)         => request(path),
  post:   (path, body)   => request(path, { method: 'POST',   body: JSON.stringify(body) }),
  put:    (path, body)   => request(path, { method: 'PUT',    body: JSON.stringify(body) }),

  // Auth
  login:    (mail, contrasena)  => api.post('/auth/login',    { mail, contrasena }),
  logout:   ()                  => api.post('/auth/logout',   {}),
  register: (body)              => api.post('/auth/registro', body),

  // Eventos
  getEventos: ()                => api.get('/eventos'),
  crearEvento: (body)           => api.post('/eventos', body),

  // Estadios
  getEstadios:  ()              => api.get('/estadios'),
  crearEstadio: (body)          => api.post('/estadios', body),
  crearSector: (id, body)       => api.post(`/estadios/${id}/sectores`, body),

  // Ventas
  comprar: (body)               => api.post('/ventas', body),
  misCompras: ()                => api.get('/ventas/mis-compras'),

  // Entradas
  misEntradas: ()               => api.get('/entradas/mis-entradas'),

  // Transferencias
  crearTransferencia: (body)    => api.post('/transferencias', body),
  resolverTransferencia: (id, accion) => api.put(`/transferencias/${id}`, { accion }),
  misTransferencias: ()         => api.get('/transferencias/mis-transferencias'),

  // Dispositivos
  misDispositivos: ()           => api.get('/dispositivos/mios'),

  // Validaciones
  validar: (body)               => api.post('/validaciones', body),
}
