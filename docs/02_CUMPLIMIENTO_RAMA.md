# Cumplimiento de la rama `correccion-bdii-ticketing-100`

## Rama
- **Nombre:** `correccion-bdii-ticketing-100`
- **Base:** `main`
- **Objetivo:** completar los requisitos obligatorios faltantes detectados en
  [01_FALTANTES_MAIN.md](01_FALTANTES_MAIN.md), respetando el MER del equipo y el
  modelo existente, con código simple y documentación defendible.

## Resumen de cambios

1. **Entrada Dinámica (token de 30s)** implementada de punta a punta: columna de
   vencimiento, regeneración por ventana temporal y rechazo de tokens vencidos.
2. **Control de aforo** en la compra (lock pesimista de sector + validación) y trigger
   de capacidad como red de seguridad.
3. **Reportes**: eventos más vendidos y ranking de mayores compradores.
4. **Jurisdicción del administrador**: solo gestiona estadios/eventos de su país sede.
5. **Teléfonos múltiples** en el registro.
6. **Cadena de custodia** consultable por entrada.
7. **Altas de gestión** desde la app: funcionarios, administradores, dispositivos y
   asignaciones funcionario–sector.
8. **Cobertura de sectores (RNE 5)** expuesta como reporte.
9. **Fix del Maven Wrapper** para que `./mvnw` funcione.
10. **Documentación** completa en `docs/`.

## Tabla detallada

| Falta detectada en main | Corrección realizada | Archivos | Cómo probarlo | Estado |
|---|---|---|---|---|
| Token estático | Columna `TOKEN_QR.ExpiraEn`; `TokenService` genera/regenera token con ventana de 30s; `mis-entradas` devuelve siempre el token vigente; endpoint `GET /entradas/{id}/token`. | `sql/schema.sql`, `entity/TokenQr.java`, `service/TokenService.java`, `service/EntradaService.java`, `service/VentaService.java` | Abrir “Mis entradas” dos veces con >30s: el código QR cambia. | Cumple |
| Token vencido aceptado | `ValidacionService` rechaza con 410 si el token no está dentro de la ventana de 30s. | `service/ValidacionService.java` | Validar un token de hace >30s → error “Token vencido”. | Cumple |
| Sin control de aforo | Lock pesimista `SECTOR` + conteo de entradas vs `CapacidadMax` en `VentaService`; trigger `tr_entrada_capacidad`. | `service/VentaService.java`, `repository/SectorRepository.java`, `repository/EntradaRepository.java`, `sql/triggers.sql` | Vender más entradas que la capacidad → error “capacidad insuficiente / sobre-aforo”. | Cumple |
| Sin eventos más vendidos | Query nativa + `ReporteService` + endpoint `GET /reportes/eventos-mas-vendidos` + pantalla. | `repository/EventoRepository.java`, `service/ReporteService.java`, `controller/ReporteController.java`, `frontend/.../AdminReportes.jsx` | Login admin → “Reportes”. | Cumple |
| Sin ranking de compradores | Query nativa + endpoint `GET /reportes/ranking-compradores` + pantalla. | `repository/VentaRepository.java`, `ReporteService`, `ReporteController`, `AdminReportes.jsx` | Login admin → “Reportes”. | Cumple |
| Admin sin jurisdicción | `AdminService` verifica que el país del estadio/evento sea el `PaisSede` del admin; `listarEstadios` filtra por país. | `service/AdminService.java`, `controller/EstadioController.java` | Admin de Uruguay intenta crear estadio en Brasil → 403. | Cumple |
| Teléfonos no capturados | `RegistroRequest.telefonos`; `UsuarioService` guarda filas en `TELEFONO`; formulario de registro con campo de teléfonos. | `dto/RegistroRequest.java`, `service/UsuarioService.java`, `service/AuthService.java`, `frontend/.../Register.jsx` | Registrarse con teléfonos separados por coma. | Cumple |
| Cadena de custodia no consultable | `EntradaService.cadenaCustodia` + endpoint `GET /entradas/{id}/custodia`. | `service/EntradaService.java`, `dto/CadenaCustodiaResponse.java`, `controller/EntradaController.java` | `GET /api/entradas/1/custodia`. | Cumple |
| Altas solo por seed | `GestionService` + `GestionController`: crear funcionario, administrador, dispositivo y asignación. | `service/GestionService.java`, `controller/GestionController.java`, `dto/*Request.java` | `POST /api/gestion/funcionarios`, etc. | Cumple |
| Cobertura no expuesta (RNE 5) | Reporte `GET /reportes/cobertura/{eventoId}` sobre la vista `v_cobertura_funcionario`. | `repository/AsignacionFuncionarioRepository.java`, `ReporteService`, `ReporteController` | `GET /api/reportes/cobertura/1`. | Cumple |
| `./mvnw` no arrancaba | Se agregó `.mvn/wrapper/maven-wrapper.properties` y se corrigió `.gitignore`. | `backend/.mvn/wrapper/maven-wrapper.properties`, `.gitignore` | `cd backend && ./mvnw -v`. | Cumple |
| Datos de prueba escasos para reportes | Segundo evento y ventas de user2/user3 en el seed. | `sql/seed.sql` | Reportes muestran datos. | Cumple |

## Ajuste de modelo (documentado)

El único cambio de esquema es la columna **`TOKEN_QR.ExpiraEn`**, necesaria para la
Entrada Dinámica. El propio enunciado del MER pide “agregarse o implementarse también
vencimiento/expiración o ventana temporal”. No se eliminó ni cambió ninguna entidad ni
relación. Ver justificación en [04_MODELO_DATOS.md](04_MODELO_DATOS.md) y
[05_BITACORA_MODELADO.md](05_BITACORA_MODELADO.md).

## Verificación realizada

- **Backend:** `./mvnw -DskipTests compile` → compila correctamente con JDK 21.
- **Frontend:** `npm run build` → build correcto.
- (Las pruebas de triggers se ejecutan con `sql/test_triggers.sql` contra una BD MySQL;
  ver [07_GUIA_EJECUCION.md](07_GUIA_EJECUCION.md).)
