# Explicación Frontend — Ticketing Mundial 2026

## ¿Qué es React y cómo lo usamos?

React es una librería JavaScript para construir interfaces de usuario declarativas. En lugar de manipular el DOM directamente, describimos *qué* queremos mostrar y React calcula los cambios necesarios.

Nuestra app usa **Vite** como herramienta de desarrollo y corre en `http://localhost:5173`. Todas las peticiones a `/api` se redirigen automáticamente a `http://localhost:8080` via el proxy de Vite, lo que evita problemas de CORS durante el desarrollo y permite que el navegador envíe la cookie de sesión.

---

## Estructura de carpetas

```
frontend/src/
├── App.jsx                  ← rutas de la app + protección de acceso por rol
├── main.jsx                 ← punto de entrada; monta <App> en el DOM
├── contexts/
│   └── AuthContext.jsx      ← estado global de sesión (usuario logueado)
├── pages/
│   ├── Login.jsx            ← pantalla de login y registro
│   ├── Eventos.jsx          ← lista de eventos + apertura del modal de compra
│   ├── CompraModal.jsx      ← formulario de selección de sector y cantidad
│   ├── MisCosas.jsx         ← mis compras / mis entradas / transferencias
│   ├── Validar.jsx          ← pantalla del funcionario para escanear QR
│   ├── AdminEstadios.jsx    ← ABM de estadios y sectores (solo ADMINISTRADOR)
│   └── AdminEventos.jsx     ← ABM de eventos (solo ADMINISTRADOR)
├── api/
│   └── client.js            ← funciones centralizadas para llamar a la API
└── utils/
    └── format.js            ← formateadores de fecha y dinero
```

---

## Los hooks principales de React

### `useState` — estado local del componente

```jsx
const [form, setForm] = useState({ nombre: '', pais: '', ciudad: '' })
// form      → valor actual (el "qué mostrar")
// setForm   → función para actualizar (dispara re-render automático)
```

Cada componente tiene su propio estado. Cuando `setForm` se llama con un nuevo valor, React re-renderiza solo ese componente y sus hijos.

### `useEffect` — efectos secundarios

```jsx
useEffect(() => {
  api.getEventos().then(data => setEventos(data))
}, [])  // [] = solo al montar el componente (equivale a componentDidMount)
```

Se usa para: cargar datos al entrar a la pantalla. El array de dependencias controla cuándo corre:
- `[]` → solo al montar
- `[valor]` → cada vez que `valor` cambia

### `useCallback` — funciones memorizadas

```jsx
// loadAll: carga las tres secciones en paralelo (Promise.all) y las guarda en estado.
// Se usa también como recarga post-acción (transferir, aceptar, rechazar).
const loadAll = useCallback(() => {
  Promise.all([api.misCompras(), api.misEntradas(), api.misTransferencias()])
    .then(([c, e, t]) => { setCompras(c); setEntradas(e); setTransferencias(t) })
}, [])
```

`useCallback` evita recrear la función en cada render. Necesario aquí porque `loadAll` aparece como dependencia de un `useEffect` — sin memoizar, el efecto se correría en bucle infinito.

### `useContext` — acceso al contexto global

```jsx
const { user, login, logout } = useAuth()
// Accede al AuthContext desde cualquier componente sin pasar props manualmente
```

---

## AuthContext.jsx — el corazón de la sesión

`AuthContext.jsx` crea un "proveedor" que envuelve toda la app. Cualquier componente puede leer el usuario actual y llamar a `login`/`logout`/`register` sin importar qué tan profundo esté en el árbol.

```jsx
// login: llama al backend, guarda el usuario en el estado global, navega según el rol.
async function login(mail, contrasena) {
  const data = await api.login({ mail, contrasena })
  setUser(data)
}

// logout: invalida la sesión en el backend y limpia el estado local.
async function logout() {
  await api.logout()
  setUser(null)
}

// register: registra un nuevo USUARIO_GENERAL, luego inicia sesión automáticamente.
async function register(datos) {
  const data = await api.register(datos)
  setUser(data)
}
```

Al arrancar la app, `AuthContext` llama a `/api/auth/yo` para recuperar la sesión si la cookie `JSESSIONID` sigue vigente — así el usuario no pierde la sesión al refrescar la página.

---

## App.jsx — rutas y control de acceso

React Router v6 mapea URLs a componentes. Las rutas protegidas verifican el rol antes de renderizar:

```jsx
// Ruta pública — redirige al home según rol si ya está logueado
<Route path="/login" element={<Login />} />

// Ruta protegida — solo USUARIO_GENERAL
<Route path="/eventos" element={<RequireAuth rol="USUARIO_GENERAL"><Eventos /></RequireAuth>} />

// Mis cosas — tres URLs diferentes, mismo componente (la tab activa viene del pathname)
<Route path="/mis-compras"         element={<RequireAuth rol="USUARIO_GENERAL"><MisCosas /></RequireAuth>} />
<Route path="/mis-entradas"        element={<RequireAuth rol="USUARIO_GENERAL"><MisCosas /></RequireAuth>} />
<Route path="/mis-transferencias"  element={<RequireAuth rol="USUARIO_GENERAL"><MisCosas /></RequireAuth>} />
```

`RequireAuth` usa `useAuth()` para leer el usuario actual. Si no hay sesión, redirige a `/login`. Si el rol no coincide, redirige al home del rol correcto.

---

## Login.jsx — dos modos en una pantalla

```jsx
// ROLE_HOME: destino de redirección post-login según el rol del usuario recién autenticado.
const ROLE_HOME = { ADMINISTRADOR: '/admin/eventos', FUNCIONARIO: '/validar', USUARIO_GENERAL: '/eventos' }

// change: manejador genérico de inputs; actualiza el campo correspondiente en el objeto form.
function change(e) {
  setForm(f => ({ ...f, [e.target.name]: e.target.value }))
}

// submit: envía login o registro según el modo activo; navega al home del rol en caso de éxito.
async function submit(e) {
  e.preventDefault()
  const data = modo === 'login'
    ? await login(form.mail, form.contrasena)
    : await register(form)
  navigate(ROLE_HOME[data.rol])
}
```

El formulario alterna entre "Iniciar sesión" y "Registrarse" con un `useState` de modo. El mismo handler `submit` decide qué función del AuthContext llamar.

---

## Eventos.jsx + CompraModal.jsx — flujo de compra

### Eventos.jsx

```jsx
// comprando: evento en proceso de compra; cuando no es null, abre el CompraModal con ese evento.
const [comprando, setComprando] = useState(null)

// useEffect: carga los eventos disponibles al montar el componente.
useEffect(() => {
  api.getEventos().then(setEventos).catch(e => setError(e.message)).finally(...)
}, [])
```

Muestra la lista de eventos y, al hacer clic en "Comprar", pone el evento seleccionado en `comprando`. El `CompraModal` recibe ese evento como prop.

### CompraModal.jsx

```jsx
// adj: ajusta la cantidad del sector dado en ±1 (mínimo 0).
function adj(sector, delta) {
  setItems(prev => ({ ...prev, [sector]: Math.max(0, (prev[sector] ?? 0) + delta) }))
}

// confirmar: construye el payload y llama a api.comprar(); cierra el modal en éxito.
async function confirmar() {
  const payload = { items: Object.entries(items).filter(([,n]) => n > 0)
    .map(([letraSector, cantidad]) => ({ letraSector, cantidad, eventoId, estadioId })) }
  await api.comprar(payload)
  onSuccess()
}

// backdropClick: cierra el modal solo si el clic fue en el fondo oscuro (no en el contenido).
function backdropClick(e) {
  if (e.target === e.currentTarget) onClose()
}
```

---

## MisCosas.jsx — tres tabs, un componente

Las rutas `/mis-compras`, `/mis-entradas` y `/mis-transferencias` renderizan el mismo componente. La tab activa se determina desde el pathname actual:

```jsx
function activeTab(pathname) {
  if (pathname.startsWith('/mis-entradas'))       return 'entradas'
  if (pathname.startsWith('/mis-transferencias')) return 'transferencias'
  return 'compras'
}
```

`loadAll` carga los tres endpoints en paralelo con `Promise.all` y se reutiliza como recarga después de transferir, aceptar o rechazar.

El botón "Transferir" en la tab de entradas abre un modal inline. Tras crear la transferencia, `loadAll()` refresca todo para que el estado de la entrada cambie a "En transferencia".

---

## AdminEstadios.jsx — estado aislado por estadio

```jsx
// sectorForms/Errors/Pending: estado aislado por estadioId para que cada card funcione de forma independiente.
// Usar un objeto keyed (no un array) permite actualizar solo el estadio afectado sin re-renderizar los demás.
const [sectorForms,   setSectorForms]   = useState({})
const [sectorErrors,  setSectorErrors]  = useState({})
const [sectorPending, setSectorPending] = useState({})
```

Cada estadio tiene su propio formulario de "Agregar sector". Al actualizar el form del estadio 3, los demás no se re-renderizan. El patrón de "objeto keyed por ID" es la solución estándar cuando hay N formularios independientes en la misma pantalla.

---

## AdminEventos.jsx — sectores dinámicos

```jsx
// estadioSeleccionado: objeto completo del estadio elegido (incluye sus sectores configurados).
// sectoresDisponibles: array de sectores para mostrar los checkboxes; vacío si no hay estadio seleccionado.
const estadioSeleccionado = estadios.find(e => e.estadioId === Number(form.estadioId))
const sectoresDisponibles = estadioSeleccionado?.sectores ?? []

// toggleSector: agrega o quita una letra del array form.sectores (selección múltiple de sectores habilitados).
function toggleSector(letra) {
  setForm(f => ({
    ...f,
    sectores: f.sectores.includes(letra)
      ? f.sectores.filter(s => s !== letra)
      : [...f.sectores, letra],
  }))
}
```

Cuando el usuario cambia de estadio, los sectores seleccionados se limpian (el handler del `<select>` incluye `sectores: []`). Esto previene enviar sectores de un estadio anterior.

El campo `datetime-local` del navegador devuelve `"YYYY-MM-DDTHH:MM"` sin segundos, pero Spring necesita `"YYYY-MM-DDTHH:MM:SS"` — por eso se concatena `':00'` antes de enviar.

---

## client.js — capa de API centralizada

```js
const BASE = '/api'
async function request(path, opts = {}) {
  const res = await fetch(BASE + path, { credentials: 'include', ...opts,
    headers: { 'Content-Type': 'application/json', ...opts.headers } })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.mensaje ?? body.error ?? `HTTP ${res.status}`)
  }
  return res.json()
}

export const api = {
  login:   body  => request('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  comprar: body  => request('/ventas',     { method: 'POST', body: JSON.stringify(body) }),
  // ...
}
```

**`credentials: 'include'`** hace que el navegador envíe la cookie `JSESSIONID` en cada petición — esencial para que el backend reconozca la sesión.

**Manejo de errores:** Si el backend devuelve un error (ej. 409 por RNE 4), `request()` extrae el campo `mensaje` del body JSON y lanza un `Error` con ese texto. El componente lo captura en su `catch` y lo muestra al usuario.

---

## Flujo completo de compra (de clic a respuesta)

1. Usuario ve la lista de eventos en `Eventos.jsx`
2. Hace clic en "Comprar" → `setComprando(evento)` → se abre `CompraModal`
3. Selecciona sectores y cantidades con los botones ±1 (`adj`)
4. Hace clic en "Confirmar" → `confirmar()` llama `api.comprar({ items })`
5. `client.js` hace `POST /api/ventas` con cookie de sesión
6. Backend valida sesión, crea VENTA + ENTRADAs + TOKEN_QR en una transacción
7. Backend devuelve `{ ventaId, entradaIds, montoTotal }` → HTTP 201
8. `CompraModal` cierra y `Eventos.jsx` muestra mensaje de éxito
9. El usuario puede ir a `/mis-entradas` para ver sus entradas con el QR

---

## Flujo completo de transferencia (de clic a resolución)

1. En `MisCosas.jsx` tab "Mis entradas", el usuario hace clic en "Transferir"
2. Se abre el modal inline → ingresa el email del destinatario
3. Clic en "Enviar transferencia" → `api.crearTransferencia({ entradaId, mailDestino })`
4. `POST /api/transferencias` → backend valida RNE 2 (máx. 3) y RNE 6 (entrada Activa)
5. Backend crea la TRANSFERENCIA → trigger `tr_transferencia_marcar_pendiente` pone la ENTRADA en "PendienteTransferencia"
6. `loadAll()` refresca — la entrada ahora muestra estado "En transferencia"
7. El destinatario entra a su cuenta y ve la transferencia en "Mis transferencias" con botones Aceptar/Rechazar
8. Si acepta: `PUT /api/transferencias/{id}?accion=ACEPTAR` → trigger `tr_transferencia_resolver` cambia `Mail_Propietario` y restaura la entrada a Activa
9. Si rechaza: solo restaura Activa, sin cambio de propietario

---

## Preguntas frecuentes de defensa

**¿Qué es un componente React?**
Una función que recibe props y retorna JSX (HTML extendido con expresiones JavaScript). React lo renderiza en el DOM y lo actualiza automáticamente cuando su estado cambia.

**¿Qué diferencia hay entre `state` y `props`?**
- `props`: datos que recibe el componente desde su padre — solo-lectura, el hijo no puede cambiarlos
- `state`: datos internos del componente — pueden cambiar con `setState`, lo que dispara un re-render

**¿Por qué `useCallback` en `loadAll` y no una función normal?**
Porque `loadAll` está en el array de dependencias de un `useEffect`. Cada render crea una nueva referencia de función — sin `useCallback`, el efecto detecta que "la dependencia cambió" y se vuelve a ejecutar, recargando datos en bucle infinito.

**¿Qué es el Vite proxy y por qué lo necesitamos?**
En desarrollo, el frontend corre en `localhost:5173` y el backend en `localhost:8080`. El proxy de Vite redirige todas las peticiones a `/api/*` hacia `localhost:8080`, haciendo que el navegador vea todo en el mismo origen — evita CORS y permite enviar la cookie de sesión.

**¿Por qué tres rutas distintas (mis-compras, mis-entradas, mis-transferencias) si es el mismo componente?**
Para que el usuario pueda navegar directamente con la URL, usar el botón "atrás" del navegador, y bookmarkear cada tab. Un solo componente con lógica de tabs interna no soportaría eso — la URL no cambiaría y no habría historial de navegación.

**¿Qué pasa si el usuario refresca la página?**
`AuthContext` llama a `/api/auth/yo` al montar. Si la cookie `JSESSIONID` sigue vigente en el servidor, el backend devuelve el usuario actual y la app se restaura en el estado correcto. Si expiró, devuelve 401 y el usuario ve el login.

**¿Por qué `credentials: 'include'` en `fetch`?**
Por defecto, `fetch` no envía cookies a dominios diferentes. Con `credentials: 'include'`, el navegador incluye la cookie `JSESSIONID` incluso en peticiones cross-origin. Complementariamente, el backend tiene `allowCredentials = true` en `CorsConfig.java`.

**¿Qué hace `e.target === e.currentTarget` en el backdrop?**
`e.target` es el elemento donde ocurrió el clic. `e.currentTarget` es el elemento que tiene el event listener (el fondo oscuro del modal). Si son iguales, el clic fue directamente sobre el backdrop y no sobre el modal — entonces cerramos. Si el usuario hace clic dentro del modal, `e.target` sería un input o botón y la condición falla, no se cierra.

**¿Por qué `??` en `estadioSeleccionado?.sectores ?? []`?**
El operador `?.` (optional chaining) evita un error si `estadioSeleccionado` es `undefined` (cuando no hay estadio seleccionado). El operador `??` (nullish coalescing) devuelve `[]` si el resultado fue `null` o `undefined`. Juntos: "dame los sectores del estadio seleccionado, o un array vacío si no hay ninguno."

**¿Qué es JSX?**
JSX es una extensión de JavaScript que permite escribir HTML dentro del código JS. Vite lo transforma a llamadas `React.createElement(...)`. `<div className="card">` en JSX se convierte en `React.createElement('div', { className: 'card' }, ...)`.
