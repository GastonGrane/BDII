-- =============================================================================
-- test_triggers.sql — Suite de pruebas de triggers y stored procedures
-- Sistema de Ticketing Mundial 2026 — Grupo 4
--
-- Requisito: ejecutar DESPUÉS de schema.sql + triggers.sql + seed.sql.
-- Prueba los 13 triggers y los 2 SPs contra los datos de seed.sql.
-- Los objetos de prueba (_test_resultados, sp_test_triggers) se eliminan al final.
--
-- Cobertura:
--   T01  RNE 1  — límite de 5 entradas por venta
--   T02         — un solo token activo por entrada
--   T03  RNE 6  — transferencia crea estado PendienteTransferencia
--   T04         — aceptar transferencia actualiza propietario y estado
--   T05  RNE 6  — no transferir entrada PendienteTransferencia
--   T06         — validación deja entrada Consumida y token inactivo
--   T07  RNE 7  — Consumida es irreversible
--   T08  RNE 6  — no transferir entrada Consumida
--   T09  RNE 9  — validar con token inactivo rechazado
--   T10  RNE 2  — límite de 3 transferencias por entrada
--   T11a RNE 4  — evento solapado rechazado
--   T11b RNE 4  — evento fuera de ventana aceptado
--   T12  RNE 12 — INSERT directo en COMISION con vigente activa rechazado
--   T13  RNE 12 — sp_nueva_comision cierra anterior y abre nueva (con transacción)
--   T14  PEN-01 — aceptar transferencia de entrada ya consumida rechazado
-- =============================================================================

USE CD_Grupo4;

CREATE TABLE IF NOT EXISTS _test_resultados (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    prueba    VARCHAR(120)           NOT NULL,
    resultado ENUM('PASS', 'FAIL')  NOT NULL,
    detalle   VARCHAR(500)
) ENGINE=InnoDB;

DROP PROCEDURE IF EXISTS sp_test_triggers;

DELIMITER //
CREATE PROCEDURE sp_test_triggers()
BEGIN
    DECLARE v_msg    VARCHAR(500);
    DECLARE v_estado VARCHAR(30);
    DECLARE v_prop   VARCHAR(254);
    DECLARE v_activo TINYINT;
    DECLARE v_count  INT;
    DECLARE v_id     INT;

    -- =========================================================================
    -- T01 — RNE 1: la 6ta entrada en VENTA 1 debe ser rechazada
    -- =========================================================================
    SET v_msg = NULL;
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
            GET DIAGNOSTICS CONDITION 1 v_msg = MESSAGE_TEXT;
        INSERT INTO ENTRADA (EstadoEntrada, Costo_Historico, VentaID, EventoID, EstadioID, LetraSector, Mail_Propietario)
        VALUES ('Activa', 150.00, 1, 1, 1, 'A', 'user1@test.com');
    END;
    INSERT INTO _test_resultados (prueba, resultado, detalle) VALUES (
        'T01 RNE1: 6ta entrada rechazada',
        IF(v_msg LIKE '%RNE 1%', 'PASS', 'FAIL'),
        COALESCE(v_msg, 'Sin error — INSERT debía fallar')
    );

    -- =========================================================================
    -- T02 — Token único activo: 2do token activo para ENTRADA 1 debe rechazarse
    -- =========================================================================
    SET v_msg = NULL;
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
            GET DIAGNOSTICS CONDITION 1 v_msg = MESSAGE_TEXT;
        INSERT INTO TOKEN_QR (CodigoQR, GeneradoEn, Activo, EntradaID)
        VALUES ('QR-E1-DUP', '2026-06-10 10:06:00', TRUE, 1);
    END;
    INSERT INTO _test_resultados (prueba, resultado, detalle) VALUES (
        'T02 Token único activo: 2do activo rechazado',
        IF(v_msg LIKE '%RNE Activo%', 'PASS', 'FAIL'),
        COALESCE(v_msg, 'Sin error — INSERT debía fallar')
    );

    -- =========================================================================
    -- T03 — Transferencia ENTRADA 1 (user1→user2)
    --        Trigger AFTER INSERT debe poner ENTRADA 1 en PendienteTransferencia
    -- =========================================================================
    SET v_msg = NULL;
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
            GET DIAGNOSTICS CONDITION 1 v_msg = MESSAGE_TEXT;
        INSERT INTO TRANSFERENCIA (FechaSol, Estado, EntradaID, Mail_Origen, Mail_Destino)
        VALUES ('2026-06-11 09:00:00', 'Pendiente', 1, 'user1@test.com', 'user2@test.com');
        SET v_id = LAST_INSERT_ID();
    END;
    SELECT EstadoEntrada INTO v_estado FROM ENTRADA WHERE EntradaID = 1;
    INSERT INTO _test_resultados (prueba, resultado, detalle) VALUES (
        'T03 Transferencia: ENTRADA 1 → PendienteTransferencia',
        IF(v_msg IS NULL AND v_estado = 'PendienteTransferencia', 'PASS', 'FAIL'),
        CONCAT('Estado: ', IFNULL(v_estado, '?'), IFNULL(CONCAT(' | Error: ', v_msg), ''))
    );

    -- =========================================================================
    -- T04 — Aceptar transferencia: ENTRADA 1 → Activa, propietario = user2
    -- =========================================================================
    SET v_msg = NULL;
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
            GET DIAGNOSTICS CONDITION 1 v_msg = MESSAGE_TEXT;
        UPDATE TRANSFERENCIA SET Estado = 'Aceptada' WHERE TransfID = v_id;
    END;
    SELECT EstadoEntrada, Mail_Propietario INTO v_estado, v_prop FROM ENTRADA WHERE EntradaID = 1;
    INSERT INTO _test_resultados (prueba, resultado, detalle) VALUES (
        'T04 Transfer aceptada: ENTRADA 1 → Activa, propietario=user2',
        IF(v_msg IS NULL AND v_estado = 'Activa' AND v_prop = 'user2@test.com', 'PASS', 'FAIL'),
        CONCAT('Estado: ', IFNULL(v_estado, '?'), ', Prop: ', IFNULL(v_prop, '?'), IFNULL(CONCAT(' | Error: ', v_msg), ''))
    );

    -- =========================================================================
    -- T05 — RNE 6: no se puede transferir ENTRADA en PendienteTransferencia
    --        Setup: dejar ENTRADA 4 pendiente, luego intentar una segunda transferencia
    -- =========================================================================
    INSERT INTO TRANSFERENCIA (FechaSol, Estado, EntradaID, Mail_Origen, Mail_Destino)
    VALUES ('2026-06-11 10:00:00', 'Pendiente', 4, 'user1@test.com', 'user2@test.com');

    SET v_msg = NULL;
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
            GET DIAGNOSTICS CONDITION 1 v_msg = MESSAGE_TEXT;
        INSERT INTO TRANSFERENCIA (FechaSol, Estado, EntradaID, Mail_Origen, Mail_Destino)
        VALUES ('2026-06-11 10:01:00', 'Pendiente', 4, 'user1@test.com', 'user3@test.com');
    END;
    INSERT INTO _test_resultados (prueba, resultado, detalle) VALUES (
        'T05 RNE6: transfer sobre ENTRADA PendienteTransferencia rechazada',
        IF(v_msg LIKE '%RNE 6%', 'PASS', 'FAIL'),
        COALESCE(v_msg, 'Sin error — INSERT debía fallar')
    );

    -- =========================================================================
    -- T06 — Validación: INSERT en VALIDACION debe marcar ENTRADA 2 Consumida
    --        y TOKEN 2 con Activo = FALSE (trigger AFTER INSERT en VALIDACION)
    -- =========================================================================
    SET v_msg = NULL;
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
            GET DIAGNOSTICS CONDITION 1 v_msg = MESSAGE_TEXT;
        INSERT INTO VALIDACION (TokenID, Mail_Funcionario, DispositivoID, FechaHora)
        VALUES (2, 'func@ticketing.com', 1, '2026-06-20 18:30:00');
    END;
    SELECT EstadoEntrada INTO v_estado FROM ENTRADA  WHERE EntradaID = 2;
    SELECT Activo        INTO v_activo FROM TOKEN_QR WHERE TokenID   = 2;
    INSERT INTO _test_resultados (prueba, resultado, detalle) VALUES (
        'T06 Validación: ENTRADA 2 → Consumida, TOKEN 2 → inactivo',
        IF(v_msg IS NULL AND v_estado = 'Consumida' AND v_activo = FALSE, 'PASS', 'FAIL'),
        CONCAT('Estado: ', IFNULL(v_estado, '?'), ', Activo token: ', IFNULL(v_activo, '?'), IFNULL(CONCAT(' | Error: ', v_msg), ''))
    );

    -- =========================================================================
    -- T07 — RNE 7: revertir ENTRADA 2 (Consumida → Activa) debe ser rechazado
    -- =========================================================================
    SET v_msg = NULL;
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
            GET DIAGNOSTICS CONDITION 1 v_msg = MESSAGE_TEXT;
        UPDATE ENTRADA SET EstadoEntrada = 'Activa' WHERE EntradaID = 2;
    END;
    INSERT INTO _test_resultados (prueba, resultado, detalle) VALUES (
        'T07 RNE7: revertir Consumida rechazado',
        IF(v_msg LIKE '%RNE 7%', 'PASS', 'FAIL'),
        COALESCE(v_msg, 'Sin error — UPDATE debía fallar')
    );

    -- =========================================================================
    -- T08 — RNE 6 (Consumida): transferir ENTRADA 2 debe rechazarse
    -- =========================================================================
    SET v_msg = NULL;
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
            GET DIAGNOSTICS CONDITION 1 v_msg = MESSAGE_TEXT;
        INSERT INTO TRANSFERENCIA (FechaSol, Estado, EntradaID, Mail_Origen, Mail_Destino)
        VALUES ('2026-06-20 19:00:00', 'Pendiente', 2, 'user1@test.com', 'user2@test.com');
    END;
    INSERT INTO _test_resultados (prueba, resultado, detalle) VALUES (
        'T08 RNE6: transfer de ENTRADA Consumida rechazada',
        IF(v_msg LIKE '%RNE 6%', 'PASS', 'FAIL'),
        COALESCE(v_msg, 'Sin error — INSERT debía fallar')
    );

    -- =========================================================================
    -- T09 — RNE 9: validar con TOKEN 2 (inactivo tras T06) debe rechazarse
    -- =========================================================================
    SET v_msg = NULL;
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
            GET DIAGNOSTICS CONDITION 1 v_msg = MESSAGE_TEXT;
        INSERT INTO VALIDACION (TokenID, Mail_Funcionario, DispositivoID, FechaHora)
        VALUES (2, 'func@ticketing.com', 1, '2026-06-20 19:00:00');
    END;
    INSERT INTO _test_resultados (prueba, resultado, detalle) VALUES (
        'T09 RNE9: validar con token inactivo rechazado',
        IF(v_msg LIKE '%RNE 9%', 'PASS', 'FAIL'),
        COALESCE(v_msg, 'Sin error — INSERT debía fallar')
    );

    -- =========================================================================
    -- T10 — RNE 2: límite de 3 transferencias por ENTRADA (usamos ENTRADA 3)
    --        3 ciclos aceptados (user1→user2→user3→user1), la 4ta debe fallar
    -- =========================================================================
    INSERT INTO TRANSFERENCIA (FechaSol, Estado, EntradaID, Mail_Origen, Mail_Destino)
    VALUES ('2026-06-12 10:00:00', 'Pendiente', 3, 'user1@test.com', 'user2@test.com');
    SET v_id = LAST_INSERT_ID();
    UPDATE TRANSFERENCIA SET Estado = 'Aceptada' WHERE TransfID = v_id;

    INSERT INTO TRANSFERENCIA (FechaSol, Estado, EntradaID, Mail_Origen, Mail_Destino)
    VALUES ('2026-06-13 10:00:00', 'Pendiente', 3, 'user2@test.com', 'user3@test.com');
    SET v_id = LAST_INSERT_ID();
    UPDATE TRANSFERENCIA SET Estado = 'Aceptada' WHERE TransfID = v_id;

    INSERT INTO TRANSFERENCIA (FechaSol, Estado, EntradaID, Mail_Origen, Mail_Destino)
    VALUES ('2026-06-14 10:00:00', 'Pendiente', 3, 'user3@test.com', 'user1@test.com');
    SET v_id = LAST_INSERT_ID();
    UPDATE TRANSFERENCIA SET Estado = 'Aceptada' WHERE TransfID = v_id;

    SET v_msg = NULL;
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
            GET DIAGNOSTICS CONDITION 1 v_msg = MESSAGE_TEXT;
        INSERT INTO TRANSFERENCIA (FechaSol, Estado, EntradaID, Mail_Origen, Mail_Destino)
        VALUES ('2026-06-15 10:00:00', 'Pendiente', 3, 'user1@test.com', 'user2@test.com');
    END;
    SELECT COUNT(*) INTO v_count FROM TRANSFERENCIA WHERE EntradaID = 3 AND Estado = 'Aceptada';
    INSERT INTO _test_resultados (prueba, resultado, detalle) VALUES (
        'T10 RNE2: 4ta transferencia rechazada (3 aceptadas previas)',
        IF(v_msg LIKE '%RNE 2%', 'PASS', 'FAIL'),
        CONCAT('Aceptadas: ', v_count, IFNULL(CONCAT(' | Error: ', v_msg), ' | Sin error — debía fallar'))
    );

    -- =========================================================================
    -- T11a — RNE 4: evento solapado en mismo estadio rechazado (19:00 dentro de 18:00+4h)
    -- T11b — RNE 4: evento fuera de ventana aceptado (22:01, después del límite de 4h)
    -- =========================================================================
    SET v_msg = NULL;
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
            GET DIAGNOSTICS CONDITION 1 v_msg = MESSAGE_TEXT;
        INSERT INTO EVENTO (EquipoLocal, EquipoVisitante, FechaHora, EstadioID, Mail_Administrador)
        VALUES ('Argentina', 'Francia', '2026-06-20 19:00:00', 1, 'admin@ticketing.com');
    END;
    INSERT INTO _test_resultados (prueba, resultado, detalle) VALUES (
        'T11a RNE4: evento solapado (19:00) rechazado',
        IF(v_msg LIKE '%RNE 4%', 'PASS', 'FAIL'),
        COALESCE(v_msg, 'Sin error — INSERT debía fallar')
    );

    SET v_msg = NULL;
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
            GET DIAGNOSTICS CONDITION 1 v_msg = MESSAGE_TEXT;
        INSERT INTO EVENTO (EquipoLocal, EquipoVisitante, FechaHora, EstadioID, Mail_Administrador)
        VALUES ('Argentina', 'Francia', '2026-06-20 22:01:00', 1, 'admin@ticketing.com');
    END;
    INSERT INTO _test_resultados (prueba, resultado, detalle) VALUES (
        'T11b RNE4: evento no solapado (22:01) aceptado',
        IF(v_msg IS NULL, 'PASS', 'FAIL'),
        COALESCE(v_msg, 'OK — evento creado fuera de ventana de 4h')
    );

    -- =========================================================================
    -- T12 — RNE 12: INSERT directo en COMISION con vigente activa debe rechazarse
    -- =========================================================================
    SET v_msg = NULL;
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
            GET DIAGNOSTICS CONDITION 1 v_msg = MESSAGE_TEXT;
        INSERT INTO COMISION (Porcentaje, F_Desde, F_Hasta)
        VALUES (7.00, '2026-07-01 00:00:00', NULL);
    END;
    INSERT INTO _test_resultados (prueba, resultado, detalle) VALUES (
        'T12 RNE12: INSERT directo con activa existente rechazado',
        IF(v_msg LIKE '%RNE 12%', 'PASS', 'FAIL'),
        COALESCE(v_msg, 'Sin error — INSERT debía fallar')
    );

    -- =========================================================================
    -- T13 — sp_nueva_comision: cierra la comisión anterior (5%) y abre una nueva (6%)
    --        Verifica que el EXIT HANDLER + transacción funcionen (PENDIENTE-02)
    -- =========================================================================
    SET v_msg = NULL;
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
            GET DIAGNOSTICS CONDITION 1 v_msg = MESSAGE_TEXT;
        CALL sp_nueva_comision(6.00, '2026-07-01 00:00:00');
    END;
    SELECT COUNT(*) INTO v_count FROM COMISION WHERE F_Hasta IS NULL;
    INSERT INTO _test_resultados (prueba, resultado, detalle) VALUES (
        'T13 sp_nueva_comision: cierra 5%, abre 6% (con transacción)',
        IF(v_msg IS NULL AND v_count = 1, 'PASS', 'FAIL'),
        CONCAT('Comisiones activas: ', v_count, IFNULL(CONCAT(' | Error: ', v_msg), ''))
    );

    -- =========================================================================
    -- T14 — PENDIENTE-01: aceptar transferencia de ENTRADA ya Consumida debe fallar
    --        RNE 7 bloquea el UPDATE interno que tr_transferencia_resolver intenta hacer
    -- =========================================================================
    INSERT INTO TRANSFERENCIA (FechaSol, Estado, EntradaID, Mail_Origen, Mail_Destino)
    VALUES ('2026-06-15 12:00:00', 'Pendiente', 5, 'user1@test.com', 'user2@test.com');
    SET v_id = LAST_INSERT_ID();

    INSERT INTO VALIDACION (TokenID, Mail_Funcionario, DispositivoID, FechaHora)
    VALUES (5, 'func@ticketing.com', 1, '2026-06-20 18:45:00');

    SET v_msg = NULL;
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
            GET DIAGNOSTICS CONDITION 1 v_msg = MESSAGE_TEXT;
        UPDATE TRANSFERENCIA SET Estado = 'Aceptada' WHERE TransfID = v_id;
    END;
    SELECT EstadoEntrada INTO v_estado FROM ENTRADA WHERE EntradaID = 5;
    INSERT INTO _test_resultados (prueba, resultado, detalle) VALUES (
        'T14 PENDIENTE-01: aceptar transfer de ENTRADA Consumida rechazado',
        IF(v_msg LIKE '%RNE 7%' AND v_estado = 'Consumida', 'PASS', 'FAIL'),
        CONCAT('Estado ENTRADA 5: ', IFNULL(v_estado, '?'), IFNULL(CONCAT(' | Error: ', v_msg), ' | Sin error — debía fallar'))
    );

END //
DELIMITER ;

CALL sp_test_triggers();

-- =============================================================================
-- RESULTADOS
-- =============================================================================
SELECT id, prueba, resultado, detalle FROM _test_resultados ORDER BY id;

SELECT
    SUM(resultado = 'PASS')                        AS tests_ok,
    SUM(resultado = 'FAIL')                        AS tests_fail,
    CONCAT(SUM(resultado = 'PASS'), '/', COUNT(*)) AS resumen
FROM _test_resultados;

-- Limpieza de objetos de prueba
DROP PROCEDURE IF EXISTS sp_test_triggers;
DROP TABLE     IF EXISTS _test_resultados;
