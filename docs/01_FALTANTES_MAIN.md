# Faltantes en `main` (auditoría previa a la corrección)

Este documento registra, con evidencia, lo que faltaba en la rama `main` respecto a la
letra del obligatorio. Se usó como base para el trabajo de la rama
`correccion-bdii-ticketing-100`. La evaluación es honesta: `main` ya tenía una base
SQL sólida (esquema, triggers, transacciones), pero faltaban funcionalidades centrales.

## Resumen

`main` cumplía bien la parte de **modelo y restricciones SQL** (PK, FK, UNIQUE, CHECK,
13 triggers, 2 procedimientos, transacciones, comisión con histórico, máximo 5 entradas,
máximo 3 transferencias, no superposición de eventos, consumo irreversible). Lo que
faltaba estaba sobre todo en **funcionalidades de aplicación** y en una **pieza central
del enunciado: la Entrada Dinámica (token de 30 segundos)**, descripta en la
documentación pero no implementada en el código.

## Tabla de faltantes

| Requisito de la letra | Qué pedía | Estado en main | Qué faltaba | Severidad | Zona afectada |
|---|---|---|---|---|---|
| Token dinámico (Entrada Dinámica) | Token que muta cada 30s, con vencimiento; no aceptar vencidos | No cumplía | El token se generaba una sola vez en la compra (UUID estático). No había columna de expiración ni regeneración. La doc (DEC-02) lo describía como implementado. | Crítica | `TOKEN_QR`, `VentaService`, `ValidacionService` |
| Control de capacidad / sobre-aforo | Capacidad del sector como límite duro de venta | No cumplía | `VentaService` no contaba entradas vendidas contra `CapacidadMax`; no había trigger de aforo. | Crítica | `VentaService`, `triggers.sql` |
| Eventos con más entradas vendidas | Reporte de ranking de eventos | No cumplía | No existía consulta ni endpoint. | Alta | (no existía) |
| Ranking de mayores compradores | Reporte de ranking de compradores | No cumplía | No existía consulta ni endpoint. | Alta | (no existía) |
| Jurisdicción del administrador | Admin gestiona solo estadios/eventos de su país sede | No cumplía | `AdminService.crearEstadio/crearEvento` no comparaba el país con `ADMINISTRADOR.PaisSede`. | Alta | `AdminService` |
| Teléfonos múltiples | Varios teléfonos por usuario | Parcial | La tabla `TELEFONO` existía, pero el registro no capturaba teléfonos ni había endpoint. | Media | `RegistroRequest`, `AuthService` |
| Cadena de custodia consultable | Reconstruir emisión → propietarios → validación | Parcial | Los datos estaban en `TRANSFERENCIA`, pero no había consulta/endpoint que reconstruyera la cadena. | Media | (no existía) |
| Alta de admin/funcionario/dispositivo desde la app | Crear estos actores para la demo | No cumplía | Solo se creaban por `seed.sql`. El registro solo creaba `USUARIO_GENERAL`. | Media | `AuthService` (no existía) |
| Asignación funcionario–sector y cobertura (RNE 5) | Modelo y control de cobertura | Parcial | El modelo (`ASIGNACION_FUNCIONARIO`) y la vista `v_cobertura_funcionario` existían, pero no se exponían en la app. | Media | (no existía) |
| Token vencido en validación | No aceptar tokens vencidos | No cumplía | `ValidacionService` solo verificaba `Activo`, no la ventana temporal. | Alta | `ValidacionService` |
| Maven Wrapper | `./mvnw` debe funcionar | No cumplía | Faltaba `.mvn/wrapper/maven-wrapper.properties`; `.gitignore` impedía versionarlo. El build no arrancaba. | Alta | `backend/.mvn/`, `.gitignore` |
| Documentación de informe | Docs de modelo, reglas, ejecución, defensa | Parcial | Había `decisiones.md` y explicaciones, pero faltaba la documentación estructurada del obligatorio. | Media | `docs/` |

## Lo que main SÍ cumplía (no se tocó el sentido)

- Esquema relacional completo con las 13 entidades del MER y `EVENTO_SECTOR` / `ASIGNACION_FUNCIONARIO` / `VALIDACION` / `TELEFONO`.
- Restricciones: PK, FK, `UNIQUE(PaisDoc, TipoDoc, NroDoc)`, `UNIQUE(NroLegajo)`, `CHECK` de capacidad/costo/porcentaje.
- Máximo 5 entradas por venta (trigger + servicio).
- Máximo 3 transferencias por entrada (trigger + servicio).
- Transferencia con aceptación del destinatario y máquina de estados por triggers.
- No superposición de eventos en el mismo estadio (trigger, ventana de 4h).
- Comisión con histórico temporal (`COMISION` + `sp_nueva_comision`).
- Consumo irreversible y no doble validación (triggers + PK de `VALIDACION`).
- Dispositivo vinculado obligatoriamente a funcionario; solo dispositivos autorizados validan.
- Listados “mis compras”, “mis transferencias”, “mis entradas”.
- Control de acceso por rol en cada endpoint.
