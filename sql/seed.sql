-- =============================================================================
-- seed.sql — Datos base para demostración y desarrollo
-- Sistema de Ticketing Mundial 2026 — Grupo 4
--
-- Requisito: ejecutar sobre esquema limpio (schema.sql + triggers.sql previos).
-- Solo contiene datos representativos del dominio — sin lógica de prueba.
-- Para ejecutar los tests de triggers: ver sql/test_triggers.sql
-- =============================================================================

USE CD_Grupo4;

-- COMISION inicial: 5 % vigente desde el 1/1/2026
CALL sp_nueva_comision(5.00, '2026-01-01 00:00:00');

-- ---- USUARIOS ---------------------------------------------------------------
INSERT INTO USUARIO (Mail, Contrasena, PaisDoc, TipoDoc, NroDoc, PaisDir, Localidad, Calle, NroPuerta, CodPostal)
VALUES
  ('admin@ticketing.com', 'admin123', 'Uruguay', 'CI', '12345678', 'Uruguay', 'Montevideo', 'Av. 18 de Julio', '1234', '11100'),
  ('func@ticketing.com',  'func123',  'Uruguay', 'CI', '23456789', 'Uruguay', 'Montevideo', 'Av. Brasil',       '456',  '11300'),
  ('user1@test.com',      'user123',  'Uruguay', 'CI', '34567890', 'Uruguay', 'Montevideo', 'Colonia',          '789',  '11200'),
  ('user2@test.com',      'user123',  'Uruguay', 'CI', '45678901', 'Uruguay', 'Montevideo', 'Rivera',           '321',  '11400'),
  ('user3@test.com',      'user123',  'Uruguay', 'CI', '56789012', 'Uruguay', 'Montevideo', 'Libertad',         '654',  '11500');

INSERT INTO ADMINISTRADOR (Mail_Usuario, PaisSede, FechaAsignacion)
VALUES ('admin@ticketing.com', 'Uruguay', '2026-01-15');

INSERT INTO FUNCIONARIO (Mail_Usuario, NroLegajo)
VALUES ('func@ticketing.com', 'LEG-001');

INSERT INTO USUARIO_GENERAL (Mail_Usuario, FechaRegistro, EstadoVerificacion)
VALUES
  ('user1@test.com', '2026-01-01', 'Verificado'),
  ('user2@test.com', '2026-01-02', 'Verificado'),
  ('user3@test.com', '2026-01-03', 'Verificado');

INSERT INTO TELEFONO (Mail_Usuario, Telefono)
VALUES ('user1@test.com', '+59899111111');

-- ---- INFRAESTRUCTURA --------------------------------------------------------
INSERT INTO ESTADIO (EstadioID, Nombre, Pais, Ciudad)
VALUES (1, 'Estadio Centenario', 'Uruguay', 'Montevideo');

INSERT INTO SECTOR (EstadioID, LetraSector, CapacidadMax, CostoEntrada)
VALUES
  (1, 'A', 10000, 150.00),
  (1, 'B',  8000, 120.00),
  (1, 'C',  6000, 200.00),
  (1, 'D',  4000, 300.00);

-- EVENTO 1 — Uruguay vs Brasil (20/6/2026 18:00, sectores A y B habilitados)
INSERT INTO EVENTO (EventoID, EquipoLocal, EquipoVisitante, FechaHora, EstadioID, Mail_Administrador)
VALUES (1, 'Uruguay', 'Brasil', '2026-06-20 18:00:00', 1, 'admin@ticketing.com');

INSERT INTO EVENTO_SECTOR (EventoID, EstadioID, LetraSector)
VALUES (1, 1, 'A'), (1, 1, 'B');

INSERT INTO DISPOSITIVO (DispositivoID, Mail_Funcionario)
VALUES (1, 'func@ticketing.com');

INSERT INTO ASIGNACION_FUNCIONARIO (Mail_Funcionario, EventoID, EstadioID, LetraSector)
VALUES ('func@ticketing.com', 1, 1, 'A');

-- ---- VENTAS Y ENTRADAS ------------------------------------------------------
-- VENTA 1: 5 entradas para user1 (el máximo por RNE 1)
INSERT INTO VENTA (VentaID, Fecha, Estado, Mail_Comprador, ComisionID)
VALUES (1, '2026-06-10 10:00:00', 'Confirmada', 'user1@test.com', 1);

INSERT INTO ENTRADA (EntradaID, EstadoEntrada, Costo_Historico, VentaID, EventoID, EstadioID, LetraSector, Mail_Propietario)
VALUES
  (1, 'Activa', 150.00, 1, 1, 1, 'A', 'user1@test.com'),
  (2, 'Activa', 150.00, 1, 1, 1, 'A', 'user1@test.com'),
  (3, 'Activa', 150.00, 1, 1, 1, 'A', 'user1@test.com'),
  (4, 'Activa', 150.00, 1, 1, 1, 'A', 'user1@test.com'),
  (5, 'Activa', 150.00, 1, 1, 1, 'A', 'user1@test.com');

-- Un token activo por entrada
INSERT INTO TOKEN_QR (TokenID, CodigoQR, GeneradoEn, Activo, EntradaID)
VALUES
  (1, 'QR-E1-001', '2026-06-10 10:05:00', TRUE, 1),
  (2, 'QR-E2-001', '2026-06-10 10:05:00', TRUE, 2),
  (3, 'QR-E3-001', '2026-06-10 10:05:00', TRUE, 3),
  (4, 'QR-E4-001', '2026-06-10 10:05:00', TRUE, 4),
  (5, 'QR-E5-001', '2026-06-10 10:05:00', TRUE, 5);
