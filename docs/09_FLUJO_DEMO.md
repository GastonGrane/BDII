# Flujo de demo (paso a paso)

Guion para mostrar la demo completa. Se puede hacer por la interfaz web o por API
(cURL en [10_ENDPOINTS_O_PRUEBAS.md](10_ENDPOINTS_O_PRUEBAS.md)). Los datos base ya vienen
en el seed; los pasos de “alta” se muestran para demostrar que funcionan desde la app.

> Para autenticarse por API, guardar la cookie de sesión:
> `curl -c cookies.txt -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"mail":"...","contrasena":"..."}'`
> y reutilizar `-b cookies.txt` en las siguientes llamadas.

| # | Paso | Endpoint / Pantalla | Datos de ejemplo | Resultado esperado |
|---|---|---|---|---|
| 1 | Crear usuario general | `POST /api/auth/registro` · pantalla Registro | mail, contraseña, documento, dirección, teléfonos | 201; usuario creado con sus teléfonos |
| 2 | Crear administrador por país sede | `POST /api/gestion/administradores` (admin) | `{ "datos": {...}, "paisSede": "Argentina", "fechaAsignacion": "2026-02-01" }` | 201; nuevo admin |
| 3 | Crear funcionario | `POST /api/gestion/funcionarios` (admin) | `{ "datos": {...}, "nroLegajo": "LEG-002" }` | 201; funcionario |
| 4 | Crear dispositivo autorizado | `POST /api/gestion/dispositivos` (admin) | `{ "mailFuncionario": "func@ticketing.com" }` | 201; `{ "dispositivoId": N }` |
| 5 | Crear estadio | `POST /api/estadios` (admin) · pantalla Estadios | `{ "nombre": "...", "pais": "Uruguay", "ciudad": "..." }` | 201 si el país coincide con la jurisdicción |
| 6 | Crear sectores A/B/C/D | `POST /api/estadios/{id}/sectores` | `{ "letraSector": "A", "capacidadMax": 100, "costoEntrada": 150 }` | 201 por cada sector |
| 7 | Crear evento | `POST /api/eventos` · pantalla Eventos (admin) | local, visitante, estadio, fechaHora, sectores | 201; 409 si se solapa con otro evento |
| 8 | Habilitar sectores | (incluido en el alta de evento, campo `sectores`) | `["A","B"]` | filas en `EVENTO_SECTOR` |
| 9 | Comprar hasta 5 entradas | `POST /api/ventas` · modal de compra | items con evento/sector/cantidad | 201; error si supera 5 o el aforo |
| 10 | Ver comisión en el total | `GET /api/ventas/mis-compras` | — | monto = subtotal × (1 + 5/100) |
| 11 | Ver entradas asignadas | `GET /api/entradas/mis-entradas` · “Mis entradas” | — | lista con token vigente |
| 12 | Transferir una entrada | `POST /api/transferencias` | `{ "entradaId": 1, "mailDestino": "user2@test.com" }` | 201; entrada queda PendienteTransferencia |
| 13 | Aceptar la transferencia | `PUT /api/transferencias/{id}` (destinatario) | `{ "accion": "ACEPTAR" }` | 200 |
| 14 | Ver cambio de propietario | `GET /api/entradas/mis-entradas` (destinatario) | — | la entrada ahora aparece en user2 |
| 15 | Ver cadena de custodia | `GET /api/entradas/{id}/custodia` | — | comprador original, propietario actual, transferencias |
| 16 | Generar/ver token dinámico | `GET /api/entradas/{id}/token` | — | token con `expiraEn` a 30s; cambia al repetir tras 30s |
| 17 | Validar entrada | `POST /api/validaciones` (funcionario) | `{ "codigoQR": "<token vigente>", "dispositivoId": 1 }` | 200; entrada validada |
| 18 | Confirmar consumo | `GET /api/entradas/mis-entradas` | — | la entrada figura `Consumida` |
| 19 | Revalidar (debe fallar) | `POST /api/validaciones` con el mismo token | — | error: entrada consumida / token inactivo |
| 20 | Ranking de compradores | `GET /api/reportes/ranking-compradores` · “Reportes” | — | user1, user2, user3 ordenados |
| 21 | Eventos más vendidos | `GET /api/reportes/eventos-mas-vendidos` · “Reportes” | — | eventos ordenados por entradas vendidas |

## Demostración rápida del token dinámico

1. Login como `user1@test.com`.
2. `GET /api/entradas/1/token` → anotar `codigoQR` y `expiraEn`.
3. Esperar 31 segundos.
4. `GET /api/entradas/1/token` → el `codigoQR` es **distinto** (se regeneró).
5. Intentar validar el token viejo → **410 Token vencido**.
6. Validar el token nuevo dentro de la ventana → **200** y la entrada queda consumida.

## Demostración del aforo

1. Crear un sector con `capacidadMax: 2`.
2. Habilitarlo en un evento.
3. Comprar 2 entradas → OK.
4. Comprar 1 más → error “RNE 3: capacidad insuficiente / sobre-aforo”.

## Demostración de jurisdicción

1. Login como `admin@ticketing.com` (PaisSede: Uruguay).
2. Crear estadio con `pais: "Brasil"` → **403 Fuera de jurisdicción**.
3. Crear estadio con `pais: "Uruguay"` → **201**.
