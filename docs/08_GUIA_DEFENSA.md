# Guía de defensa

Cómo explicar el proyecto oralmente y responder preguntas de la cátedra.

## 1. Qué problema resuelve

Un sistema de ticketing para el Mundial 2026 que permite **comercializar, transferir y
validar** entradas. La entrada no es una imagen estática: es un activo digital con dueño
y con un **token que muta cada 30 segundos** para evitar fraude por captura de pantalla.

## 2. Cómo está diseñado el modelo (y por qué)

- Partimos del **MER del equipo** (`BDIIFifaSegundaVersion`), lo pasamos a modelo lógico
  relacional y aplicamos normalización (1FN–3FN).
- USUARIO se especializa en **administrador / funcionario / usuario general** (tabla por
  subclase) para conservar los datos propios de cada rol.
- Atributos compuestos (documento, dirección) descompuestos en columnas; teléfonos en
  tabla aparte (1FN). Documento con `UNIQUE(PaisDoc,TipoDoc,NroDoc)`.
- Sector es entidad débil del estadio (PK compuesta). “Evento habilita sector” es N:N →
  `EVENTO_SECTOR`.

## 3. Cómo se separa compra de tenencia

- La **VENTA** registra al **comprador original** (`Mail_Comprador`).
- La **ENTRADA** registra al **propietario actual** (`Mail_Propietario`), que puede
  cambiar por transferencias. Por eso una entrada puede pertenecer hoy a alguien que no
  la compró.

## 4. Cómo funciona la transferencia

1. El propietario actual crea una `TRANSFERENCIA` (estado **Pendiente**); la entrada pasa
   a `PendienteTransferencia` (trigger).
2. El destinatario **acepta** o **rechaza**.
3. Solo al **aceptar** cambia `Mail_Propietario` y la entrada vuelve a `Activa` (trigger).
4. Máximo 3 transferencias por entrada; no se puede transferir una entrada consumida.

## 5. Cómo funciona el token dinámico

- Cada entrada activa tiene un `TOKEN_QR` con `GeneradoEn` y `ExpiraEn = GeneradoEn + 30s`.
- El frontend pide el token (`/mis-entradas` o `/entradas/{id}/token`); si el vigente
  venció, el backend **desactiva el anterior y genera uno nuevo** (`TokenService`).
- Al validar, el token debe estar **activo y dentro de su ventana de 30s**; si venció, se
  rechaza (410). Esto hace que una captura de pantalla quede inservible en segundos.

## 6. Cómo se evita el sobre-aforo

- En la compra se toma un **lock pesimista** sobre la fila del `SECTOR` (serializa compras
  concurrentes del mismo sector) y se cuenta lo vendido contra `CapacidadMax`.
- Además, el trigger `tr_entrada_capacidad` rechaza cualquier inserción que supere la
  capacidad, aunque se intente por fuera de la app.

## 7. Cómo se asegura la integridad

- PK/FK en todo el modelo; `UNIQUE` y `CHECK` para reglas estructurales.
- Triggers para reglas sobre filas (máx. 5/3, solapamiento, irreversibilidad).
- Transacciones (`@Transactional`) en compra, transferencia, aceptación y validación.
- PK de `VALIDACION` por TokenID → no doble validación.

## 8. Cómo se aplican conceptos de BDII

- Diseño conceptual (MER) → lógico (MR con PK/FK) → físico (índices, triggers, vistas, SP).
- Normalización 1FN–3FN; atributos derivados como vista (MontoTotal).
- Restricciones no estructurales llevadas a SQL/servicio/consulta según corresponda.
- Concurrencia con locks y transacciones.

## 9. Preguntas frecuentes y cómo responderlas

- **“¿El token realmente cambia?”** Sí: cada vez que se pide y la ventana de 30s venció,
  se genera uno nuevo y se desactiva el anterior. Las filas viejas quedan como histórico.
- **“¿Por qué regeneran bajo demanda y no con un job cada 30s?”** Por volumen: un job para
  miles de entradas generaría millones de filas. Bajo demanda da el mismo efecto funcional
  con costo acotado (DEC-02).
- **“¿Dónde está el comprador original si la entrada se transfirió?”** En `ENTRADA → VENTA
  → Mail_Comprador`. El propietario actual es `ENTRADA.Mail_Propietario`.
- **“¿Cómo evitan vender dos veces el último lugar?”** Lock pesimista del sector + trigger
  de capacidad; las dos transacciones se serializan sobre la fila del sector.
- **“¿Por qué MontoTotal no es columna?”** Es derivado (entradas × comisión); persistirlo
  viola 3FN. Lo calculamos con la vista `v_monto_total_venta`.
- **“¿Por qué VALIDACION tiene PK = TokenID?”** Porque un token se valida a lo sumo una
  vez; la PK garantiza “no doble validación” a nivel de BD.
- **“¿El admin puede tocar otros países?”** No: `AdminService` verifica que el país del
  estadio/evento sea su `PaisSede`.
- **“¿Y el QR visual?”** Es opcional en el enunciado. Implementamos toda la lógica de
  seguridad (token dinámico, validación, auditoría, consumo); el código se muestra como
  texto y la generación visual queda como extensión.
