# Matriz de cumplimiento de la letra del obligatorio

Estado: **Cumple / Parcial / No cumple**. Tipo: **Obligatorio (O) / Opcional (Op)**.
Las rutas de código son relativas a `backend/src/main/java/com/grupo4/ticketing/`.

## 1. Arquitectura y tecnología

| ID | Requisito | Tipo | Cómo se cumple | Código | SQL | Cómo probarlo | Estado |
|---|---|---|---|---|---|---|---|
| A1 | BD con soporte SQL | O | MySQL 8 (InnoDB) | `application.properties` | `sql/schema.sql` | Conectar a MySQL | Cumple |
| A2 | Persistencia SQL real (no mocks) | O | JPA con `ddl-auto=none`, lógica en triggers/SP | `application.properties` | `sql/triggers.sql` | Revisar que no hay H2/mocks | Cumple |
| A3 | Cliente/servidor | O | API REST + frontend React | `controller/*`, `frontend/` | — | Backend 8080, frontend 3000 | Cumple |
| A4 | Java o .NET | O | Java 21 + Spring Boot 3.3.5 | `pom.xml` | — | `./mvnw -v` | Cumple |
| A5 | BD ejecutable en Linux | O | MySQL multiplataforma; guía Linux | — | — | [07_GUIA_EJECUCION.md](07_GUIA_EJECUCION.md) | Cumple |
| A6 | Scripts SQL de creación | O | schema/triggers/seed ordenados | — | `sql/*` | Ejecutarlos en orden | Cumple |
| A7 | Documentación de ejecución | O | README + docs | — | — | `README.md` | Cumple |

## 2. Usuarios y registro

| ID | Requisito | Tipo | Cómo se cumple | Código | SQL | Cómo probarlo | Estado |
|---|---|---|---|---|---|---|---|
| U1 | Registro de usuario | O | `POST /api/auth/registro` | `AuthService.registrar` | `USUARIO`, `USUARIO_GENERAL` | Registrarse desde la app | Cumple |
| U2 | Email único identificador | O | `Mail` PK | `entity/Usuario.java` | `pk_usuario` | Registrar mail repetido → error | Cumple |
| U3 | Documento compuesto | O | PaisDoc+TipoDoc+NroDoc | `entity/Usuario.java` | columnas en `USUARIO` | — | Cumple |
| U4 | Unicidad de documento | O | `UNIQUE(PaisDoc,TipoDoc,NroDoc)` | — | `uq_usuario_documento` | Insertar doc repetido → error | Cumple |
| U5 | Dirección compuesta | O | País/Localidad/Calle/NroPuerta/CodPostal | `entity/Usuario.java` | `USUARIO` | — | Cumple |
| U6 | Teléfonos múltiples | O | Lista en registro → tabla `TELEFONO` | `UsuarioService`, `dto/RegistroRequest` | `TELEFONO` (1:N) | Registrarse con varios teléfonos | Cumple |
| U7 | Cuenta como repositorio de entradas | O | `ENTRADA.Mail_Propietario` + “Mis entradas” | `EntradaService.misEntradas` | `ENTRADA` | Ver “Mis entradas” | Cumple |
| U8 | Control de acceso por roles | O | `requireRol` por endpoint | `util/SessionUtils.java` | — | Acceder sin rol → 403 | Cumple |

## 3. Roles

| ID | Requisito | Tipo | Cómo se cumple | Código | SQL | Cómo probarlo | Estado |
|---|---|---|---|---|---|---|---|
| R1 | Administrador por país sede | O | Subtipo `ADMINISTRADOR` (PaisSede, FechaAsignacion) | `entity/Administrador.java` | `ADMINISTRADOR` | Login admin | Cumple |
| R2 | Funcionario de validación | O | Subtipo `FUNCIONARIO` (NroLegajo) | `entity/Funcionario.java` | `FUNCIONARIO` | Login funcionario | Cumple |
| R3 | Usuario general | O | Subtipo `USUARIO_GENERAL` (FechaRegistro, EstadoVerificacion) | `entity/UsuarioGeneral.java` | `USUARIO_GENERAL` | Login user | Cumple |
| R4 | Admin: solo su jurisdicción | O | Verifica país del recurso vs `PaisSede` | `AdminService.verificarJurisdiccion` | — | Crear estadio de otro país → 403 | Cumple |
| R5 | Admin: fecha de asignación | O | `FechaAsignacion` | — | `ADMINISTRADOR` | — | Cumple |
| R6 | Funcionario: legajo | O | `NroLegajo UNIQUE` | — | `uq_funcionario_legajo` | — | Cumple |
| R7 | Funcionario vinculado a dispositivo | O | `DISPOSITIVO.Mail_Funcionario NOT NULL` | `entity/Dispositivo.java` | `fk_disp_funcionario` | — | Cumple |
| R8 | Usuario gral: fecha registro y verificación | O | `FechaRegistro`, `EstadoVerificacion` | `AuthService` | `USUARIO_GENERAL` | — | Cumple |

## 4. Estadios, sectores y eventos

| ID | Requisito | Tipo | Cómo se cumple | Código | SQL | Cómo probarlo | Estado |
|---|---|---|---|---|---|---|---|
| E1 | Gestión de estadios | O | `POST /api/estadios` | `AdminService.crearEstadio` | `ESTADIO` | Crear estadio | Cumple |
| E2 | Sectores A/B/C/D | O | `ENUM('A','B','C','D')` | — | `SECTOR.LetraSector` | — | Cumple |
| E3 | Capacidad parametrizable | O | `CapacidadMax` por sector | `AdminService.crearSector` | `SECTOR` | Crear sector | Cumple |
| E4 | Capacidad como límite duro | O | Validación de aforo + trigger | `VentaService` | `tr_entrada_capacidad` | Sobrevender → error | Cumple |
| E5 | Precio variable por sector | O | `CostoEntrada` | — | `SECTOR` | — | Cumple |
| E6 | Evento (local/visitante/estadio/fecha) | O | `POST /api/eventos` | `AdminService.crearEvento` | `EVENTO` | Crear evento | Cumple |
| E7 | No superposición en estadio | O | Trigger (ventana 4h) | — | `tr_evento_sin_solapamiento` | Crear evento solapado → 409 | Cumple |
| E8 | Admin que da de alta | O | `EVENTO.Mail_Administrador` | — | `fk_evento_admin` | — | Cumple |
| E9 | Habilitar sectores por evento | O | `EVENTO_SECTOR` | `AdminService.crearEvento` | `EVENTO_SECTOR` | — | Cumple |
| E10 | Vender solo sectores habilitados | O | Verifica `EVENTO_SECTOR` | `VentaService` | `fk_entrada_es` | Comprar sector no habilitado → error | Cumple |

## 5. Ventas y entradas

| ID | Requisito | Tipo | Cómo se cumple | Código | SQL | Cómo probarlo | Estado |
|---|---|---|---|---|---|---|---|
| V1 | Venta centralizada y transaccional | O | `@Transactional` en compra | `VentaService.comprar` | `VENTA` | Comprar | Cumple |
| V2 | Múltiples entradas / sectores por venta | O | Lista de ítems | `dto/CompraRequest` | `ENTRADA` | Comprar varios sectores | Cumple |
| V3 | ID único por entrada | O | `EntradaID` PK autoincremental | — | `pk_entrada` | — | Cumple |
| V4 | Entrada inicial al comprador | O | `setPropietario(comprador)` | `VentaService` | `ENTRADA` | — | Cumple |
| V5 | Venta con fecha, estado, monto | O | Estados ENUM; monto calculado | `VentaService` | `VENTA`, `v_monto_total_venta` | Ver “Mis compras” | Cumple |
| V6 | Comisión del 5% con histórico | O | `COMISION` + `sp_nueva_comision` | `VentaService` | `COMISION` | — | Cumple |
| V7 | Máximo 5 entradas por venta | O | Servicio + trigger | `VentaService` | `tr_entrada_limite_venta` | Comprar 6 → error | Cumple |
| V8 | Validar capacidad / evitar sobreventa | O | Lock + conteo + trigger | `VentaService` | `tr_entrada_capacidad` | — | Cumple |

## 6 y 7. Tenencia separada y transferencias

| ID | Requisito | Tipo | Cómo se cumple | Código | SQL | Cómo probarlo | Estado |
|---|---|---|---|---|---|---|---|
| T1 | Comprador original ≠ propietario actual | O | `VENTA.Mail_Comprador` vs `ENTRADA.Mail_Propietario` | — | esquema | Ver custodia | Cumple |
| T2 | Transferencia con aceptación | O | Estado Pendiente/Aceptada/Rechazada | `TransferenciaService` | `TRANSFERENCIA` | Transferir y aceptar | Cumple |
| T3 | Cambio de propietario solo al aceptar | O | Trigger | — | `tr_transferencia_resolver` | — | Cumple |
| T4 | Log histórico | O | Tabla `TRANSFERENCIA` | — | `TRANSFERENCIA` | — | Cumple |
| T5 | Cadena de custodia reconstruible | O | `GET /entradas/{id}/custodia` | `EntradaService.cadenaCustodia` | — | Consultar custodia | Cumple |
| T6 | Máximo 3 transferencias | O | Servicio + trigger | `TransferenciaService` | `tr_transferencia_limite` | 4ª transferencia → error | Cumple |
| T7 | No transferir consumida / no propietario | O | Validaciones + trigger | `TransferenciaService` | `tr_transferencia_entrada_activa` | — | Cumple |

## 8. Seguridad, token dinámico y validación

| ID | Requisito | Tipo | Cómo se cumple | Código | SQL | Cómo probarlo | Estado |
|---|---|---|---|---|---|---|---|
| S1 | Token asociado a entrada | O | `TOKEN_QR.EntradaID` | `entity/TokenQr.java` | `fk_token_entrada` | — | Cumple |
| S2 | Token con generación y expiración | O | `GeneradoEn`, `ExpiraEn` | `TokenService` | `TOKEN_QR` | — | Cumple |
| S3 | Ventana de 30s / regeneración | O | `TokenService.tokenVigente` regenera si venció | `TokenService` | — | Recargar “Mis entradas” | Cumple |
| S4 | No aceptar token vencido | O | Rechazo 410 en validación | `ValidacionService` | — | Validar token viejo → error | Cumple |
| S5 | Registrar token, funcionario, dispositivo | O | `VALIDACION` ternaria | `ValidacionService` | `VALIDACION` | Validar | Cumple |
| S6 | Consumo irreversible | O | Triggers | — | `tr_validacion_post_insert`, `tr_entrada_consumida_irreversible` | Revalidar → error | Cumple |
| S7 | No doble validación | O | PK de `VALIDACION` + estado | `ValidacionService` | `pk_validacion` | — | Cumple |
| S8 | Dispositivo autorizado (ID único, ligado a funcionario) | O | `DISPOSITIVO` | `GestionService.crearDispositivo` | `DISPOSITIVO` | — | Cumple |
| S9 | Dispositivo no autorizado no valida | O | Verifica dueño del dispositivo | `ValidacionService` | — | Validar con dispositivo ajeno → 403 | Cumple |
| S10 | Asignación funcionario–sector y cobertura (RNE 5) | O | `ASIGNACION_FUNCIONARIO` + reporte | `GestionService`, `ReporteService` | `v_cobertura_funcionario`, `sp_verificar_cobertura` | `GET /reportes/cobertura/1` | Cumple |
| S11 | QR visual / escaneo con cámara | Op | No implementado (código QR como texto) | — | — | — | No (opcional) |

## 10. Listados y consultas

| ID | Requisito | Tipo | Cómo se cumple | Código | Cómo probarlo | Estado |
|---|---|---|---|---|---|---|
| L1 | Compras por usuario | O | `GET /ventas/mis-compras` | `VentaService.misCompras` | “Mis compras” | Cumple |
| L2 | Transferencias por usuario | O | `GET /transferencias/mis-transferencias` | `TransferenciaService.misTransferencias` | “Transferencias” | Cumple |
| L3 | Entradas asignadas | O | `GET /entradas/mis-entradas` | `EntradaService.misEntradas` | “Mis entradas” | Cumple |
| L4 | Eventos más vendidos | O | `GET /reportes/eventos-mas-vendidos` | `ReporteService` | “Reportes” | Cumple |
| L5 | Ranking de compradores | O | `GET /reportes/ranking-compradores` | `ReporteService` | “Reportes” | Cumple |
| L6 | Cadena de custodia | O | `GET /entradas/{id}/custodia` | `EntradaService` | cURL | Cumple |
| L7 | Multiusuario y permisos | O | Sesión + roles | `SessionUtils` | — | Cumple |

## 11. Base de datos e integridad

| ID | Requisito | Tipo | Estado | Evidencia |
|---|---|---|---|---|
| BD1 | PK/FK/UNIQUE/CHECK | O | Cumple | `sql/schema.sql` |
| BD2 | Índices útiles | Op | Cumple | índices en `schema.sql` |
| BD3 | Transacciones en operaciones críticas | O | Cumple | `@Transactional` en compra/transferencia/validación |
| BD4 | Concurrencia anti-sobreventa | O | Cumple | lock pesimista + `tr_entrada_capacidad` |
| BD5 | Integridad referencial | O | Cumple | FK en todo el modelo |
| BD6 | Normalización (1FN–3FN) | O | Cumple | ver [04_MODELO_DATOS.md](04_MODELO_DATOS.md) |

## 9 y 14. Documentación

| ID | Requisito | Estado | Evidencia |
|---|---|---|---|
| DOC1 | Modelo de datos documentado | Cumple | `04_MODELO_DATOS.md` |
| DOC2 | Bitácora de modelado | Cumple | `05_BITACORA_MODELADO.md` |
| DOC3 | Reglas de negocio | Cumple | `06_REGLAS_NEGOCIO.md` |
| DOC4 | Guía de ejecución | Cumple | `07_GUIA_EJECUCION.md` |
| DOC5 | Guía de defensa y flujo demo | Cumple | `08`, `09` |
| DOC6 | Endpoints / pruebas | Cumple | `10_ENDPOINTS_O_PRUEBAS.md` |
| DOC7 | MER en imagen (`BDIIFifaSegundaVersion.drawio.png`) | Parcial | El MER es la base conceptual; colocar la imagen en `docs/diagramas/`. Ver `04_MODELO_DATOS.md`. |
