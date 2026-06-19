# Modelo de datos

Este documento describe el modelo conceptual (MER), el pasaje al modelo lógico
relacional, el modelo físico implementado en MySQL y la justificación de normalización.
El modelo se basa en el MER entregado por el equipo
(`BDIIFifaSegundaVersion.drawio.png`), que es la base conceptual obligatoria.

> La imagen del MER debe colocarse en `docs/diagramas/BDIIFifaSegundaVersion.drawio.png`.
> Este documento describe en texto ese mismo modelo y cómo se implementó.

## 1. Modelo conceptual (MER)

### Entidades y atributos principales

- **USUARIO** (superclase). Identificador: `Mail`. Atributo compuesto **Documento**
  (PaisDoc, TipoDoc, NroDoc); atributo compuesto **Dirección** (PaisDir, Localidad,
  Calle, NroPuerta, CodPostal); atributo **multivaluado** Teléfonos.
- **ADMINISTRADOR** (subtipo). PaisSede, FechaAsignacion.
- **FUNCIONARIO** (subtipo). NroLegajo.
- **USUARIO_GENERAL** (subtipo). FechaRegistro, EstadoVerificacion.
- **ESTADIO**. EstadioID, Nombre, Pais, Ciudad.
- **SECTOR** (débil de Estadio). LetraSector, CapacidadMax, CostoEntrada.
- **EVENTO**. EventoID, EquipoLocal, EquipoVisitante, FechaHora.
- **COMISION**. ComisionID, Porcentaje, F_Desde, F_Hasta.
- **VENTA**. VentaID, Fecha, Estado, (MontoTotal calculado).
- **ENTRADA**. EntradaID, EstadoEntrada, Costo_Historico.
- **TRANSFERENCIA**. TransfID, FechaSol, Estado.
- **TOKEN_QR**. TokenID, CodigoQR, GeneradoEn, **ExpiraEn**, Activo.
- **DISPOSITIVO**. DispositivoID.
- **VALIDACION** (relación ternaria materializada). FechaHora.

### Relaciones y cardinalidades (interpretación Look-Through)

| Relación | Cardinalidad | Lectura |
|---|---|---|
| USUARIO → {ADMIN, FUNC, GRAL} | Especialización disjunta y total | Cada usuario es exactamente de un subtipo. |
| ADMINISTRADOR **da de alta** EVENTO | 1:N | Un admin da de alta varios eventos; un evento lo da de alta un admin. |
| ESTADIO **tiene** SECTOR | 1:N (débil) | Un estadio tiene varios sectores; un sector pertenece a un estadio. |
| EVENTO **se realiza en** ESTADIO | N:1 | Varios eventos en un estadio; un evento en un estadio. |
| EVENTO **habilita** SECTOR | N:N → `EVENTO_SECTOR` | Un evento habilita varios sectores; un sector puede habilitarse en varios eventos. |
| USUARIO_GENERAL **realiza** VENTA | 1:N | Un usuario realiza varias ventas; una venta tiene un comprador. |
| VENTA **aplica** COMISION | N:1 | Cada venta referencia la comisión vigente al momento. |
| VENTA **genera** ENTRADA | 1:N | Una venta genera de 1 a 5 entradas; una entrada pertenece a una venta. |
| USUARIO_GENERAL **posee** ENTRADA | 1:N | Un usuario posee varias entradas; cada entrada tiene un propietario actual. |
| USUARIO_GENERAL **origen/destino de** TRANSFERENCIA | 1:N | Un usuario puede iniciar/recibir varias transferencias. |
| TRANSFERENCIA **tiene** ENTRADA | N:1 | Varias transferencias sobre una entrada (máx. 3). |
| ENTRADA **corresponde a** EVENTO/SECTOR | N:1 (vía `EVENTO_SECTOR`) | Cada entrada corresponde a un sector habilitado de un evento. |
| ENTRADA **genera** TOKEN_QR | 1:N | Una entrada genera muchos tokens en el tiempo; uno activo a la vez. |
| FUNCIONARIO + DISPOSITIVO + TOKEN_QR **en** VALIDACION | ternaria | Cada validación registra un funcionario, un dispositivo y un token. |
| FUNCIONARIO **asignado a** EVENTO/SECTOR | N:N → `ASIGNACION_FUNCIONARIO` | Un funcionario se asigna a sectores de un evento. |

## 2. Modelo lógico (paso a tablas)

- La **especialización** USUARIO → subtipos se implementa con el patrón *tabla por
  subclase*: cada subtipo tiene su tabla con PK = FK hacia `USUARIO`.
- Los **teléfonos** (multivaluado) van a la tabla `TELEFONO(Mail_Usuario, Telefono)`
  con PK compuesta — nunca como texto separado por comas.
- El **documento** se modela con columnas y `UNIQUE(PaisDoc, TipoDoc, NroDoc)`.
- La **dirección** se modela con columnas en `USUARIO` conservando sus componentes.
- **SECTOR** es entidad débil: PK compuesta `(EstadioID, LetraSector)`.
- La relación N:N **EVENTO habilita SECTOR** se materializa en `EVENTO_SECTOR`.
- **ENTRADA** referencia `VENTA` (Genera), `EVENTO_SECTOR` (CorrespondeA),
  `USUARIO_GENERAL` (Posee = propietario actual). El comprador original se obtiene por
  `ENTRADA → VENTA → Mail_Comprador`.
- **TRANSFERENCIA** referencia `ENTRADA`, `USUARIO` origen y `USUARIO` destino.
- **TOKEN_QR** referencia `ENTRADA` y tiene vigencia (`GeneradoEn`, `ExpiraEn`, `Activo`).
- **VALIDACION** (ternaria) se materializa con PK = `TokenID` y FKs a `FUNCIONARIO` y
  `DISPOSITIVO`; así un token se valida a lo sumo una vez (evita doble validación).
- **DISPOSITIVO** tiene relación estable con `FUNCIONARIO` (`Mail_Funcionario NOT NULL`),
  no aparece solo en el momento de validar.

## 3. Modelo físico (MySQL / InnoDB)

Definido en `sql/schema.sql`. Resumen de restricciones:

- **PK**: en todas las tablas (simples o compuestas).
- **FK**: integridad referencial completa, con `ON DELETE RESTRICT` y `ON UPDATE CASCADE`.
- **UNIQUE**: `uq_usuario_documento`, `uq_funcionario_legajo`, `uq_token_codigo`.
- **CHECK**: `CapacidadMax > 0`, `CostoEntrada > 0`, `Porcentaje > 0`,
  `Costo_Historico > 0`, `ExpiraEn > GeneradoEn`.
- **Índices**: por FK y por campos de consulta frecuente (propietario, comprador,
  fecha, `(EntradaID, Activo)` para localizar el token activo).
- **Triggers / SP / vistas**: ver `sql/triggers.sql` y [06_REGLAS_NEGOCIO.md](06_REGLAS_NEGOCIO.md).

## 4. Justificación de normalización

- **1FN**: no hay atributos multivaluados en columnas. Los teléfonos están en `TELEFONO`;
  el documento y la dirección están descompuestos en columnas atómicas.
- **2FN**: todas las tablas con PK compuesta tienen atributos que dependen de la clave
  completa (p.ej. `SECTOR.CapacidadMax` depende de `(EstadioID, LetraSector)`).
- **3FN**: no hay dependencias transitivas. `MontoTotal` de `VENTA` no se persiste porque
  es un atributo derivado (depende de `ENTRADA.Costo_Historico` y `COMISION.Porcentaje`);
  se calcula con la vista `v_monto_total_venta` o en el backend (DEC-03).
- `ENTRADA.Costo_Historico` es un *snapshot* del precio del sector al momento de la
  compra: no es redundancia, es un dato histórico necesario (el precio del sector puede
  cambiar después y la venta debe conservar el valor cobrado).

## 5. Respeto del MER entregado por el equipo

Esta sección documenta cómo el modelo implementado respeta el MER
`BDIIFifaSegundaVersion.drawio.png`.

### Entidades del MER y su implementación

| Entidad MER | Tabla | Clase JPA |
|---|---|---|
| USUARIO | `USUARIO` | `entity/Usuario.java` |
| ADMINISTRADOR | `ADMINISTRADOR` | `entity/Administrador.java` |
| FUNCIONARIO | `FUNCIONARIO` | `entity/Funcionario.java` |
| USUARIO_GRAL | `USUARIO_GENERAL` | `entity/UsuarioGeneral.java` |
| ESTADIO | `ESTADIO` | `entity/Estadio.java` |
| SECTOR | `SECTOR` | `entity/Sector.java` |
| EVENTO | `EVENTO` | `entity/Evento.java` |
| COMISION | `COMISION` | `entity/Comision.java` |
| VENTA | `VENTA` | `entity/Venta.java` |
| ENTRADA | `ENTRADA` | `entity/Entrada.java` |
| TRANSFERENCIA | `TRANSFERENCIA` | `entity/Transferencia.java` |
| TOKEN_QR | `TOKEN_QR` | `entity/TokenQr.java` |
| DISPOSITIVO | `DISPOSITIVO` | `entity/Dispositivo.java` |
| VALIDACION | `VALIDACION` | `entity/Validacion.java` |
| (multivaluado) Teléfonos | `TELEFONO` | `entity/Telefono.java` |
| (N:N) Evento habilita Sector | `EVENTO_SECTOR` | `entity/EventoSector.java` |
| (N:N) Funcionario asignado | `ASIGNACION_FUNCIONARIO` | `entity/AsignacionFuncionario.java` |

### Relaciones del MER y su implementación

| Relación MER | Implementación |
|---|---|
| USUARIO se especializa en 3 subtipos | Tabla por subclase (PK = FK hacia USUARIO). |
| ADMINISTRADOR da de alta EVENTO | `EVENTO.Mail_Administrador` (FK). |
| ESTADIO tiene SECTOR | `SECTOR.EstadioID` (FK, entidad débil). |
| EVENTO se realiza en ESTADIO | `EVENTO.EstadioID` (FK). |
| EVENTO habilita SECTOR | Tabla `EVENTO_SECTOR`. |
| USUARIO_GRAL realiza VENTA | `VENTA.Mail_Comprador` (FK). |
| VENTA aplica COMISION | `VENTA.ComisionID` (FK). |
| VENTA genera ENTRADA | `ENTRADA.VentaID` (FK). |
| USUARIO_GRAL posee ENTRADA | `ENTRADA.Mail_Propietario` (FK). |
| USUARIO_GRAL origen/destino de TRANSFERENCIA | `TRANSFERENCIA.Mail_Origen` / `Mail_Destino`. |
| TRANSFERENCIA tiene ENTRADA | `TRANSFERENCIA.EntradaID` (FK). |
| ENTRADA corresponde a EVENTO/SECTOR | FK compuesta a `EVENTO_SECTOR`. |
| ENTRADA genera TOKEN_QR | `TOKEN_QR.EntradaID` (FK). |
| FUNC + DISP + TOKEN en VALIDACION | `VALIDACION` (PK TokenID, FK a funcionario y dispositivo). |
| FUNCIONARIO asignado a sectores | `ASIGNACION_FUNCIONARIO`. |

### Ajustes realizados al pasar a modelo lógico/físico

1. **TOKEN_QR.ExpiraEn (vigencia)**
   - *Qué decía el MER*: TOKEN_QR con TokenID, CodigoQR, GeneradoEn, Activo; el propio
     enunciado indica “debe agregarse o implementarse también vencimiento/expiración o
     ventana temporal”.
   - *Problema*: sin un campo de vencimiento no se puede implementar la Entrada Dinámica
     (token válido solo 30s).
   - *Decisión*: agregar la columna `ExpiraEn DATETIME NOT NULL` con `CHECK (ExpiraEn > GeneradoEn)`.
   - *Por qué era necesaria*: es la base de la regla “token válido 30 segundos”.
   - *Implementación*: `sql/schema.sql` (columna), `TokenService` (genera `ExpiraEn = GeneradoEn + 30s`),
     `ValidacionService` (rechaza vencidos).

2. **SECTOR con PK compuesta `(EstadioID, LetraSector)`**
   - *Qué decía el MER*: SECTOR con LetraSector como parte de su identidad, dependiente
     del estadio.
   - *Problema*: `LetraSector` se repite entre estadios; no puede ser PK por sí sola.
   - *Decisión*: PK compuesta `(EstadioID, LetraSector)` (entidad débil), como sugiere
     el propio enunciado.
   - *Implementación*: `pk_sector` en `sql/schema.sql`.

3. **MontoTotal de VENTA como atributo derivado (no persistido)**
   - *Qué decía el MER*: VENTA con MontoTotal.
   - *Problema*: persistirlo viola 3FN (depende de entradas y comisión).
   - *Decisión*: calcularlo con la vista `v_monto_total_venta` / en el backend (DEC-03).
   - *Implementación*: vista en `sql/schema.sql`, cálculo en `VentaService`.

4. **VALIDACION como entidad (materialización de la relación ternaria)**
   - *Qué decía el MER*: relación ternaria FUNCIONARIO–DISPOSITIVO–TOKEN_QR.
   - *Decisión*: tabla `VALIDACION` con PK = TokenID (un token se valida a lo sumo una vez).
   - *Por qué*: la PK por TokenID implementa directamente “no doble validación”.

Ninguno de estos ajustes cambia el sentido del modelo, elimina entidades o relaciones,
ni convierte el sistema en una única tabla de tickets.
