# Reglas de negocio

Para cada regla: qué significa, dónde se implementa, cómo se prueba y qué error devuelve.
Rutas de código relativas a `backend/src/main/java/com/grupo4/ticketing/`.

| # | Regla | Significado | Dónde | Cómo se prueba | Error si se incumple |
|---|---|---|---|---|---|
| 1 | Email único | El mail identifica al usuario | `pk_usuario`; `UsuarioService` | Registrar mail repetido | “El mail ya está registrado” |
| 2 | Documento único compuesto | (PaisDoc,TipoDoc,NroDoc) único | `uq_usuario_documento` | Insertar doc repetido | error de UNIQUE |
| 3 | Teléfonos múltiples | Varios teléfonos por usuario | `TELEFONO`; `UsuarioService` | Registrarse con varios teléfonos | — |
| 4 | Roles y permisos | Cada endpoint exige un rol | `SessionUtils.requireRol` | Acceder sin rol | 403 “Rol X no tiene permiso” |
| 5 | Jurisdicción del administrador | Admin gestiona solo su país sede | `AdminService.verificarJurisdiccion` | Crear estadio/evento de otro país | 403 “Fuera de jurisdicción” |
| 6 | No superposición de eventos | Sin dos eventos solapados en un estadio | `tr_evento_sin_solapamiento` | Crear evento solapado (ventana 4h) | 409 “RNE 4: evento solapado” |
| 7 | Sectores A/B/C/D | Dominio fijo de sectores | `SECTOR.LetraSector ENUM` | Crear sector “E” | “Letra de sector inválida” |
| 8 | Capacidad como límite duro | No vender más que `CapacidadMax` | `VentaService` + `tr_entrada_capacidad` | Comprar más que la capacidad | “RNE 3: capacidad insuficiente / sobre-aforo” |
| 9 | Comisión vigente con histórico | Aplicar la comisión vigente; conservar histórico | `COMISION`, `sp_nueva_comision`; `VentaService` | Cambiar comisión y comprar | “No hay comisión vigente” / solape |
| 10 | Máx. 5 entradas por venta | Una venta tiene hasta 5 entradas | `VentaService` + `tr_entrada_limite_venta` | Comprar 6 | “RNE 1: más de 5 entradas” |
| 11 | Comprador original vs propietario actual | Pueden diferir | `VENTA.Mail_Comprador` vs `ENTRADA.Mail_Propietario` | Transferir y ver custodia | — |
| 12 | Máx. 3 transferencias | Hasta 3 transferencias antes de validar | `TransferenciaService` + `tr_transferencia_limite` | Intentar la 4ª | “RNE 2: más de 3 transferencias” |
| 13 | Transferencia requiere aceptación | Cambia el dueño solo al aceptar | `TransferenciaService` + `tr_transferencia_resolver` | Crear y aceptar/rechazar | — |
| 14 | No transferir consumida | Una entrada validada no se transfiere | `tr_transferencia_entrada_activa` | Transferir entrada consumida | “RNE 6: solo entradas Activa” |
| 15 | Solo el propietario transfiere | El origen debe ser el dueño actual | `TransferenciaService` | Transferir entrada ajena | 403 “No eres el propietario” |
| 16 | Token dinámico 30s | El token muta y vence a los 30s | `TokenService` | Recargar “Mis entradas” tras 30s | — |
| 17 | No aceptar token vencido | Token fuera de ventana es inválido | `ValidacionService.estaVigente` | Validar token viejo | 410 “Token vencido” |
| 18 | Dispositivo autorizado | Vinculado a funcionario; solo él valida | `ValidacionService` + `DISPOSITIVO` | Validar con dispositivo ajeno | 403 “Dispositivo no asignado” |
| 19 | Validación irreversible | La entrada queda consumida | `tr_validacion_post_insert`, `tr_entrada_consumida_irreversible` | Revalidar | “RNE 7: entrada consumida” |
| 20 | No doble validación | Un token se valida una sola vez | `pk_validacion` (TokenID) | Validar dos veces | error de PK duplicada |
| 21 | Registrar funcionario/dispositivo/token | Trazabilidad de la validación | `VALIDACION` | Ver fila de validación | — |
| 22 | Cobertura de sectores (RNE 5) | Funcionario debe validar en todos sus sectores | `v_cobertura_funcionario`, reporte | `GET /reportes/cobertura/{eventoId}` | (reporte, no error) |
| 23 | Listados por usuario | Compras/transferencias/entradas propias | `VentaService`, `TransferenciaService`, `EntradaService` | Pantallas “Mis …” | — |
| 24 | Rankings | Eventos más vendidos y mayores compradores | `ReporteService` | Pantalla “Reportes” | — |
| 25 | Cadena de custodia | Emisión → propietarios → validación | `EntradaService.cadenaCustodia` | `GET /entradas/{id}/custodia` | — |

## Notas

- Las reglas 8, 10, 12, 13, 14, 19 y 20 tienen **doble defensa**: validación en el
  servicio (mensaje claro y rápido) y trigger/constraint en la BD (garantía aunque se
  acceda por fuera de la app). Esto se alinea con el material de clase: las restricciones
  obligatorias se llevan al modelo físico además de la capa de aplicación.
- La regla 16 (token dinámico) se evalúa por ventana temporal: `ExpiraEn = GeneradoEn + 30s`.
  La validación (regla 17) exige `now < ExpiraEn` además de `Activo = TRUE`.
