# Endpoints y pruebas (cURL)

Base URL: `http://localhost:8080`. La autenticación es por **sesión HTTP** (cookie).
Guardar la cookie en el login y reutilizarla con `-b cookies.txt`.

```bash
# Login (guarda la cookie de sesión)
curl -c cookies.txt -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"mail":"user1@test.com","contrasena":"user123"}'
```

## Autenticación

| Método | URL | Rol | Body | Respuesta |
|---|---|---|---|---|
| POST | `/api/auth/registro` | público | RegistroRequest (incluye `telefonos`) | 201 |
| POST | `/api/auth/login` | público | `{mail, contrasena}` | `{mail, rol}` + cookie |
| POST | `/api/auth/logout` | autenticado | — | 204 |
| GET | `/api/auth/yo` | autenticado | — | `{mail, rol}` |

```bash
curl -X POST localhost:8080/api/auth/registro -H 'Content-Type: application/json' -d '{
  "mail":"nuevo@test.com","contrasena":"clave123",
  "tipoDoc":"CI","paisDoc":"Uruguay","nroDoc":"99887766",
  "paisDir":"Uruguay","localidad":"Montevideo","calle":"Larrañaga","nroPuerta":"100","codPostal":"11200",
  "telefonos":["+59899333333","+59899444444"]
}'
```

## Estadios y eventos (rol ADMINISTRADOR)

| Método | URL | Body | Nota |
|---|---|---|---|
| GET | `/api/estadios` | — | solo estadios de la jurisdicción del admin |
| POST | `/api/estadios` | `{nombre, pais, ciudad}` | 403 si `pais` ≠ PaisSede |
| POST | `/api/estadios/{id}/sectores` | `{letraSector, capacidadMax, costoEntrada}` | |
| GET | `/api/eventos` | — | público (autenticado) |
| POST | `/api/eventos` | `{equipoLocal, equipoVisitante, fechaHora, estadioId, sectores:[...]}` | 409 si se solapa |

```bash
curl -b cookies.txt -X POST localhost:8080/api/eventos -H 'Content-Type: application/json' -d '{
  "equipoLocal":"Uruguay","equipoVisitante":"España",
  "fechaHora":"2026-07-01T18:00:00","estadioId":1,"sectores":["A","B"]
}'
```

## Ventas, entradas y custodia (rol USUARIO_GENERAL)

| Método | URL | Body | Nota |
|---|---|---|---|
| POST | `/api/ventas` | `{items:[{eventoId, estadioId, letraSector, cantidad}]}` | máx. 5; valida aforo |
| GET | `/api/ventas/mis-compras` | — | monto con comisión |
| GET | `/api/entradas/mis-entradas` | — | token vigente por entrada |
| GET | `/api/entradas/{id}/token` | — | token dinámico (30s) |
| GET | `/api/entradas/{id}/custodia` | — | cadena de custodia |

```bash
curl -b cookies.txt -X POST localhost:8080/api/ventas -H 'Content-Type: application/json' -d '{
  "items":[{"eventoId":1,"estadioId":1,"letraSector":"A","cantidad":2}]
}'

curl -b cookies.txt localhost:8080/api/entradas/1/token
curl -b cookies.txt localhost:8080/api/entradas/1/custodia
```

## Transferencias (rol USUARIO_GENERAL)

| Método | URL | Body | Nota |
|---|---|---|---|
| POST | `/api/transferencias` | `{entradaId, mailDestino}` | máx. 3; solo el propietario |
| PUT | `/api/transferencias/{id}` | `{accion:"ACEPTAR"\|"RECHAZAR"}` | solo el destinatario |
| GET | `/api/transferencias/mis-transferencias` | — | origen y destino |

## Validación (rol FUNCIONARIO)

| Método | URL | Body | Error esperado |
|---|---|---|---|
| GET | `/api/dispositivos/mios` | — | — |
| POST | `/api/validaciones` | `{codigoQR, dispositivoId}` | 410 token vencido; 409 ya consumida; 403 dispositivo ajeno |

```bash
# Login funcionario, luego validar el token vigente
curl -c cookies.txt -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"mail":"func@ticketing.com","contrasena":"func123"}'
curl -b cookies.txt -X POST localhost:8080/api/validaciones -H 'Content-Type: application/json' \
  -d '{"codigoQR":"<token vigente>","dispositivoId":1}'
```

## Reportes (rol ADMINISTRADOR)

| Método | URL | Descripción |
|---|---|---|
| GET | `/api/reportes/eventos-mas-vendidos` | ranking de eventos por entradas |
| GET | `/api/reportes/ranking-compradores` | ranking de compradores |
| GET | `/api/reportes/cobertura/{eventoId}` | sectores asignados sin validar (RNE 5) |

## Gestión / altas (rol ADMINISTRADOR)

| Método | URL | Body |
|---|---|---|
| POST | `/api/gestion/funcionarios` | `{datos: RegistroRequest, nroLegajo}` |
| POST | `/api/gestion/administradores` | `{datos: RegistroRequest, paisSede, fechaAsignacion}` |
| POST | `/api/gestion/dispositivos` | `{mailFuncionario}` |
| POST | `/api/gestion/asignaciones` | `{mailFuncionario, eventoId, estadioId, letraSector}` |

```bash
curl -b cookies.txt -X POST localhost:8080/api/gestion/funcionarios -H 'Content-Type: application/json' -d '{
  "datos":{"mail":"func2@test.com","contrasena":"func123","tipoDoc":"CI","paisDoc":"Uruguay","nroDoc":"55667788",
           "paisDir":"Uruguay","localidad":"Montevideo","calle":"Rivera","nroPuerta":"200","codPostal":"11300","telefonos":[]},
  "nroLegajo":"LEG-002"
}'
```

## Orden recomendado para probar todo

1. Login admin → crear estadio, sectores y evento.
2. (Opcional) crear funcionario y dispositivo por `/api/gestion/*`.
3. Login usuario → comprar, ver entradas y token, transferir.
4. Login segundo usuario → aceptar transferencia.
5. Login funcionario → validar el token vigente.
6. Login admin → ver reportes.
