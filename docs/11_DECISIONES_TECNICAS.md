# Decisiones técnicas

## Motor de base de datos: MySQL 8 (InnoDB)

- Soporta SQL estándar, transacciones, claves foráneas, triggers, procedimientos y vistas.
- Corre en Linux (requisito del obligatorio).
- InnoDB da integridad referencial y bloqueo a nivel de fila (necesario para el control de
  aforo con lock pesimista).

## Lenguaje y framework: Java 21 + Spring Boot 3.3.5

- Java es uno de los lenguajes permitidos por la letra.
- Spring Boot con Spring Data JPA agiliza el acceso a datos sin perder control del SQL.
- **`spring.jpa.hibernate.ddl-auto=none`**: el esquema lo crean los scripts SQL
  (`schema.sql`/`triggers.sql`), no Hibernate. Así el modelo físico es explícito,
  revisable y versionado, y la cátedra puede leer exactamente las tablas y restricciones.

## Reglas en SQL vs. en servicio vs. en consulta

- **En SQL (triggers/constraints)**: reglas sobre filas existentes que deben cumplirse
  siempre, incluso si se accede por fuera de la app (máx. 5 entradas, máx. 3 transferencias,
  no solapamiento, aforo, irreversibilidad, no doble validación). La BD es la última línea
  de defensa.
- **En servicio**: reglas que dependen del usuario autenticado o del tiempo (jurisdicción
  del admin, vigencia del token de 30s, propietario que transfiere). También se hace una
  pre-validación de las reglas SQL para devolver mensajes claros y rápidos.
- **En consulta/reporte**: reglas sobre *ausencia* de registros (cobertura RNE 5), que
  ningún INSERT dispara y por eso no pueden ser triggers.

## Transacciones

- `@Transactional` en compra, transferencia, aceptación y validación: cada operación es
  atómica. Si algo falla (p.ej. un trigger rechaza una entrada), se hace rollback y no
  queda estado a medias (p.ej. una venta sin entradas).

## Concurrencia y aforo

- En la compra se hace `SELECT ... FOR UPDATE` sobre la fila del `SECTOR`
  (`SectorRepository.findByIdForUpdate`). Esto serializa las compras concurrentes del mismo
  sector, de modo que el conteo de aforo no sufra condición de carrera.
- El trigger `tr_entrada_capacidad` cuenta las entradas del par (evento, sector) contra
  `CapacidadMax` y es la garantía final.

## Token dinámico: regeneración bajo demanda

- En vez de un proceso en segundo plano que regenere todos los tokens cada 30s (millones
  de filas en un evento masivo, DEC-02), el token se regenera **cuando el cliente lo pide**
  y la ventana venció. Mismo efecto funcional, costo acotado. Las filas anteriores quedan
  como histórico de tokens de la entrada.

## Índices

- Por claves foráneas y por campos de consulta frecuente: propietario de la entrada,
  comprador de la venta, fecha, y `(EntradaID, Activo)` en `TOKEN_QR` para localizar el
  token activo en O(log n) sin escanear la tabla.

## Extensibilidad

- Los reportes usan consultas nativas con proyecciones (`repository/projection/`), fáciles
  de extender con nuevos rankings.
- Las altas de actores (`GestionService`) reutilizan `UsuarioService.crearUsuarioBase`, de
  modo que agregar un nuevo subtipo de usuario es directo.
- El QR visual y el escaneo por cámara pueden agregarse en el frontend sin tocar la lógica
  de seguridad (el backend ya entrega el `codigoQR` vigente).

## Simplicidad del código

- Controladores livianos que delegan en servicios; DTOs como `record`; repositorios con
  métodos derivados o `@Query` claras.
- Pocos comentarios, solo donde hay una regla del obligatorio.
- Sin frameworks extra ni abstracciones innecesarias; nombres en español consistentes con
  el dominio.
