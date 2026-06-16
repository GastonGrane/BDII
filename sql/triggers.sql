-- =============================================================================
-- Triggers, procedimientos y vistas de negocio — Sistema de Ticketing Mundial 2026
-- Ejecutar DESPUÉS de schema.sql.
--
-- Estructura:
--   Módulo 1 — ENTRADA       (RNE 1, RNE 7)
--   Módulo 2 — TRANSFERENCIA (RNE 2, RNE 6 + máquina de estados de ENTRADA)
--   Módulo 3 — TOKEN_QR      (restricción: un solo token activo por entrada)
--   Módulo 4 — VALIDACION    (RNE 9, RNE 7 complemento + post-validación atómica)
--   Módulo 5 — EVENTO        (RNE 4)
--   Módulo 6 — COMISION      (RNE 12: guard + SP de alta)
--   Módulo 7 — RNE 5         (vista de cobertura + SP de verificación)
--
-- RNE 8 (propietario al validar): enforceado en capa de aplicación — ver DEC-06.
-- =============================================================================

USE CD_Grupo4;

-- =============================================================================
-- MÓDULO 1: ENTRADA
-- =============================================================================

DELIMITER //

-- RNE 1: una venta no puede tener más de 5 entradas.
-- Disparador: BEFORE INSERT ON ENTRADA — cuenta las entradas existentes para esa VentaID.
-- Nota de concurrencia: dos transacciones concurrentes para la misma VentaID podrían
-- pasar ambas este check si leen antes de que la otra haga commit. La aplicación debe
-- envolver la creación de VENTA + ENTRADAs en una sola transacción con SELECT ... FOR UPDATE
-- sobre la fila de VENTA para serializar inserciones concurrentes del mismo comprador.
CREATE TRIGGER tr_entrada_limite_venta
BEFORE INSERT ON ENTRADA
FOR EACH ROW
BEGIN
    DECLARE v_count INT;
    SELECT COUNT(*) INTO v_count FROM ENTRADA WHERE VentaID = NEW.VentaID;
    IF v_count >= 5 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE 1: una venta no puede contener más de 5 entradas';
    END IF;
END //


-- RNE 3: la capacidad del sector es un límite duro — no se puede sobre-vender (sobre-aforo).
-- Disparador: BEFORE INSERT ON ENTRADA — cuenta las entradas ya emitidas para el mismo
-- (EventoID, EstadioID, LetraSector) y las compara con SECTOR.CapacidadMax.
-- Defensa en profundidad: la aplicación además toma un lock pesimista sobre la fila de SECTOR
-- (ver VentaService) para serializar compras concurrentes del mismo sector y evitar la carrera.
CREATE TRIGGER tr_entrada_capacidad
BEFORE INSERT ON ENTRADA
FOR EACH ROW
BEGIN
    DECLARE v_emitidas INT;
    DECLARE v_capacidad INT;
    SELECT COUNT(*) INTO v_emitidas
    FROM ENTRADA
    WHERE EventoID = NEW.EventoID
      AND EstadioID = NEW.EstadioID
      AND LetraSector = NEW.LetraSector;
    SELECT CapacidadMax INTO v_capacidad
    FROM SECTOR
    WHERE EstadioID = NEW.EstadioID AND LetraSector = NEW.LetraSector;
    IF v_emitidas >= v_capacidad THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE 3: capacidad del sector agotada para el evento (sobre-aforo)';
    END IF;
END //


-- RNE 7: el estado Consumida es irreversible.
-- Disparador: BEFORE UPDATE ON ENTRADA — rechaza cualquier cambio que saque una entrada de Consumida.
-- También previene reactivar una entrada consumida a través de una transferencia posterior
-- (aunque RNE 6 ya lo bloquea antes de que llegue aquí).
CREATE TRIGGER tr_entrada_consumida_irreversible
BEFORE UPDATE ON ENTRADA
FOR EACH ROW
BEGIN
    IF OLD.EstadoEntrada = 'Consumida' AND NEW.EstadoEntrada != 'Consumida' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE 7: una entrada consumida no puede volver a un estado anterior';
    END IF;
END //


DELIMITER ;

-- =============================================================================
-- MÓDULO 2: TRANSFERENCIA
-- Incluye la máquina de estados de ENTRADA: los AFTER triggers mantienen
-- EstadoEntrada y Mail_Propietario consistentes con el ciclo de transferencia.
-- =============================================================================

DELIMITER //

-- RNE 2: una entrada no puede tener más de 3 transferencias no rechazadas.
-- Disparador: BEFORE INSERT ON TRANSFERENCIA — cuenta transferencias Pendiente + Aceptada.
-- Se excluyen las Rechazadas para impedir gaming (crear y rechazar para resetear el contador).
CREATE TRIGGER tr_transferencia_limite
BEFORE INSERT ON TRANSFERENCIA
FOR EACH ROW
BEGIN
    DECLARE v_count INT;
    SELECT COUNT(*) INTO v_count
    FROM TRANSFERENCIA
    WHERE EntradaID = NEW.EntradaID AND Estado != 'Rechazada';
    IF v_count >= 3 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE 2: una entrada no puede tener más de 3 transferencias';
    END IF;
END //


-- RNE 6: solo pueden transferirse entradas en estado Activa.
-- Disparador: BEFORE INSERT ON TRANSFERENCIA — rechaza si la entrada está PendienteTransferencia o Consumida.
CREATE TRIGGER tr_transferencia_entrada_activa
BEFORE INSERT ON TRANSFERENCIA
FOR EACH ROW
BEGIN
    DECLARE v_estado VARCHAR(30);
    SELECT EstadoEntrada INTO v_estado FROM ENTRADA WHERE EntradaID = NEW.EntradaID;
    IF v_estado != 'Activa' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE 6: solo pueden transferirse entradas con estado Activa';
    END IF;
END //


-- Máquina de estados (1/2): marca la ENTRADA como PendienteTransferencia al crear la solicitud.
-- Disparador: AFTER INSERT ON TRANSFERENCIA — el estado bloquea nuevas transferencias concurrentes (RNE 6).
CREATE TRIGGER tr_transferencia_marcar_pendiente
AFTER INSERT ON TRANSFERENCIA
FOR EACH ROW
BEGIN
    UPDATE ENTRADA
    SET EstadoEntrada = 'PendienteTransferencia'
    WHERE EntradaID = NEW.EntradaID;
END //


-- Máquina de estados (2/2): resuelve la transferencia actualizando propietario y/o estado de ENTRADA.
-- Disparador: AFTER UPDATE ON TRANSFERENCIA — si Pendiente→Aceptada cambia Mail_Propietario y restaura Activa;
-- si Pendiente→Rechazada solo restaura Activa. Solo actúa en la transición desde Pendiente.
CREATE TRIGGER tr_transferencia_resolver
AFTER UPDATE ON TRANSFERENCIA
FOR EACH ROW
BEGIN
    IF OLD.Estado = 'Pendiente' AND NEW.Estado = 'Aceptada' THEN
        UPDATE ENTRADA
        SET Mail_Propietario = NEW.Mail_Destino,
            EstadoEntrada    = 'Activa'
        WHERE EntradaID = NEW.EntradaID;
    ELSEIF OLD.Estado = 'Pendiente' AND NEW.Estado = 'Rechazada' THEN
        UPDATE ENTRADA
        SET EstadoEntrada = 'Activa'
        WHERE EntradaID = NEW.EntradaID;
    END IF;
END //


DELIMITER ;

-- =============================================================================
-- MÓDULO 3: TOKEN_QR
-- Garantiza que a lo sumo un token por entrada esté activo en cualquier momento.
-- MySQL no soporta índices parciales (WHERE Activo = TRUE), por lo que se usan triggers.
-- =============================================================================

DELIMITER //

CREATE TRIGGER tr_token_unico_activo_insert
BEFORE INSERT ON TOKEN_QR
FOR EACH ROW
BEGIN
    DECLARE v_count INT;
    IF NEW.Activo = TRUE THEN
        SELECT COUNT(*) INTO v_count
        FROM TOKEN_QR
        WHERE EntradaID = NEW.EntradaID AND Activo = TRUE;
        IF v_count > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'RNE Activo: ya existe un token activo para esta entrada';
        END IF;
    END IF;
END //


CREATE TRIGGER tr_token_unico_activo_update
BEFORE UPDATE ON TOKEN_QR
FOR EACH ROW
BEGIN
    DECLARE v_count INT;
    IF NEW.Activo = TRUE AND OLD.Activo = FALSE THEN
        SELECT COUNT(*) INTO v_count
        FROM TOKEN_QR
        WHERE EntradaID = NEW.EntradaID
          AND Activo = TRUE
          AND TokenID != NEW.TokenID;
        IF v_count > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'RNE Activo: ya existe un token activo para esta entrada';
        END IF;
    END IF;
END //


DELIMITER ;

-- =============================================================================
-- MÓDULO 4: VALIDACION
-- RNE 9, RNE 7 (complemento) y post-validación atómica.
-- Nota de concurrencia: la PK de VALIDACION (TokenID) actúa como barrera natural
-- contra la doble validación concurrente — InnoDB garantiza que solo un INSERT
-- con el mismo TokenID puede tener éxito; el segundo falla con duplicate key.
-- =============================================================================

DELIMITER //

-- RNE 9: el token debe estar activo al momento de validar.
-- Disparador: BEFORE INSERT ON VALIDACION — rechaza si TOKEN_QR.Activo != TRUE.
CREATE TRIGGER tr_validacion_token_activo
BEFORE INSERT ON VALIDACION
FOR EACH ROW
BEGIN
    DECLARE v_activo BOOLEAN;
    SELECT Activo INTO v_activo FROM TOKEN_QR WHERE TokenID = NEW.TokenID;
    IF v_activo != TRUE THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE 9: el token QR no está activo al momento de la validación';
    END IF;
END //


-- RNE 7 (complemento, defensa en profundidad): bloquea validar una entrada ya Consumida.
-- Disparador: BEFORE INSERT ON VALIDACION — verifica EstadoEntrada del token a insertar.
-- La PK de VALIDACION (TokenID) ya evita la doble inserción; este trigger cubre inconsistencias de estado.
CREATE TRIGGER tr_validacion_entrada_no_consumida
BEFORE INSERT ON VALIDACION
FOR EACH ROW
BEGIN
    DECLARE v_estado VARCHAR(30);
    SELECT e.EstadoEntrada INTO v_estado
    FROM TOKEN_QR t
    JOIN ENTRADA e ON t.EntradaID = e.EntradaID
    WHERE t.TokenID = NEW.TokenID;
    IF v_estado = 'Consumida' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE 7: la entrada ya fue consumida';
    END IF;
END //


-- Post-validación atómica: marca la ENTRADA como Consumida y desactiva todos sus TOKEN_QR.
-- Disparador: AFTER INSERT ON VALIDACION — garantiza que el marcado ocurre en la misma transacción
-- que el INSERT, sin necesidad de que el backend haga UPDATEs adicionales.
CREATE TRIGGER tr_validacion_post_insert
AFTER INSERT ON VALIDACION
FOR EACH ROW
BEGIN
    DECLARE v_entrada_id INT UNSIGNED;
    SELECT EntradaID INTO v_entrada_id FROM TOKEN_QR WHERE TokenID = NEW.TokenID;
    UPDATE ENTRADA  SET EstadoEntrada = 'Consumida' WHERE EntradaID = v_entrada_id;
    UPDATE TOKEN_QR SET Activo = FALSE              WHERE EntradaID = v_entrada_id;
END //


DELIMITER ;

-- =============================================================================
-- MÓDULO 5: EVENTO
-- RNE 4: no pueden coexistir dos eventos en el mismo estadio con horarios solapados.
-- Supuesto: duración fija de 4 horas por evento (ver DEC-05 en docs/decisiones.md).
-- =============================================================================

DELIMITER //

-- RNE 4: no pueden coexistir dos eventos en el mismo estadio con horarios solapados (ventana de 4 horas).
-- Disparador: BEFORE INSERT ON EVENTO — usa la condición de solapamiento de intervalos [A, A+4h) ∩ [B, B+4h).
CREATE TRIGGER tr_evento_sin_solapamiento
BEFORE INSERT ON EVENTO
FOR EACH ROW
BEGIN
    DECLARE v_count INT;
    SELECT COUNT(*) INTO v_count
    FROM EVENTO
    WHERE EstadioID = NEW.EstadioID
      -- Dos intervalos [A, A+4h) y [B, B+4h) se solapan si A < B+4h AND B < A+4h
      AND NEW.FechaHora < DATE_ADD(FechaHora, INTERVAL 4 HOUR)
      AND FechaHora     < DATE_ADD(NEW.FechaHora, INTERVAL 4 HOUR);
    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE 4: existe un evento solapado en el mismo estadio (ventana de 4 horas)';
    END IF;
END //


DELIMITER ;

-- =============================================================================
-- MÓDULO 6: COMISION — RNE 12
-- Dos partes:
--   a) Trigger BEFORE INSERT: guard de solapamiento sobre INSERTs directos.
--   b) SP sp_nueva_comision: workflow correcto (cierra la anterior, abre la nueva).
--
-- MySQL no permite UPDATE sobre la misma tabla dentro de un trigger, por eso
-- el cierre automático de la comisión anterior vive en el SP, no en el trigger.
--
-- Nota de concurrencia: sp_nueva_comision debe llamarse desde la aplicación dentro
-- de una transacción explícita (BEGIN / COMMIT) para evitar que dos llamadas
-- concurrentes inserten dos comisiones activas simultáneamente.
-- =============================================================================

DELIMITER //

-- RNE 12 (guard): rechaza cualquier INSERT en COMISION que solape con una vigente o existente.
-- Disparador: BEFORE INSERT ON COMISION — cubre INSERTs directos Y llamadas al SP sp_nueva_comision.
-- El SP es el camino correcto para dar de alta una comisión; este trigger es la red de seguridad.
CREATE TRIGGER tr_comision_sin_solapamiento
BEFORE INSERT ON COMISION
FOR EACH ROW
BEGIN
    DECLARE v_count INT;

    -- Caso 1: ya existe una comisión activa (F_Hasta NULL) y se intenta insertar otra activa
    IF NEW.F_Hasta IS NULL THEN
        SELECT COUNT(*) INTO v_count FROM COMISION WHERE F_Hasta IS NULL;
        IF v_count > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'RNE 12: ya existe una comisión activa. Usar sp_nueva_comision.';
        END IF;
    END IF;

    -- Caso 2: solapamiento con comisiones cerradas
    SELECT COUNT(*) INTO v_count
    FROM COMISION
    WHERE F_Hasta IS NOT NULL
      AND F_Hasta > NEW.F_Desde
      AND F_Desde < COALESCE(NEW.F_Hasta, '9999-12-31 23:59:59');
    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE 12: la nueva comisión se solaparía con una comisión existente';
    END IF;
END //


DELIMITER ;


-- SP: alta de nueva comisión.
-- Cierra la comisión vigente (si existe) y abre la nueva en una transacción atómica.
-- PENDIENTE-02 resuelto: el EXIT HANDLER hace ROLLBACK antes de resignalar,
-- garantizando que el UPDATE de cierre nunca quede aplicado sin el INSERT de la nueva.
-- Uso: CALL sp_nueva_comision(5.00, '2026-06-15 00:00:00');
DELIMITER //

CREATE PROCEDURE sp_nueva_comision(
    IN p_porcentaje DECIMAL(5,2),
    IN p_desde      DATETIME
)
BEGIN
    -- Si algo falla después del START TRANSACTION, deshace todo y propaga el error.
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF p_porcentaje <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE 12: el porcentaje debe ser mayor a 0';
    END IF;

    START TRANSACTION;
        -- Cerrar comisión vigente fijando F_Hasta = inicio de la nueva
        UPDATE COMISION SET F_Hasta = p_desde WHERE F_Hasta IS NULL;
        -- Insertar nueva comisión (el trigger tr_comision_sin_solapamiento valida el estado)
        INSERT INTO COMISION (Porcentaje, F_Desde, F_Hasta)
        VALUES (p_porcentaje, p_desde, NULL);
    COMMIT;
END //

DELIMITER ;


-- =============================================================================
-- MÓDULO 7: RNE 5 — Cobertura de sectores por funcionario
-- No puede implementarse como trigger (es una restricción sobre ausencia de registros).
-- Se implementa como vista de auditoría + SP de verificación. Ver DEC-07.
-- =============================================================================

-- Vista: estado de cobertura por funcionario/evento/sector.
-- Cubierto = 1 si el funcionario realizó al menos una validación en ese sector del evento.
CREATE OR REPLACE VIEW v_cobertura_funcionario AS
SELECT
    af.Mail_Funcionario,
    af.EventoID,
    af.EstadioID,
    af.LetraSector,
    (
        SELECT COUNT(1)
        FROM VALIDACION val
        JOIN TOKEN_QR t ON val.TokenID   = t.TokenID
        JOIN ENTRADA  e ON t.EntradaID   = e.EntradaID
        WHERE val.Mail_Funcionario = af.Mail_Funcionario
          AND e.EventoID           = af.EventoID
          AND e.EstadioID          = af.EstadioID
          AND e.LetraSector        = af.LetraSector
    ) > 0 AS Cubierto
FROM ASIGNACION_FUNCIONARIO af;


-- SP: retorna los funcionarios que no cubrieron algún sector asignado en el evento.
-- Uso: CALL sp_verificar_cobertura(1);
DELIMITER //

CREATE PROCEDURE sp_verificar_cobertura(IN p_EventoID INT UNSIGNED)
BEGIN
    SELECT
        Mail_Funcionario,
        EstadioID,
        LetraSector
    FROM v_cobertura_funcionario
    WHERE EventoID = p_EventoID
      AND Cubierto = FALSE
    ORDER BY Mail_Funcionario, LetraSector;
END //

DELIMITER ;

-- RNE: un administrador por país sede solo puede dar de alta eventos
-- en estadios pertenecientes a su jurisdicción geográfica.
-- Disparador: BEFORE INSERT ON EVENTO.
-- Verifica que PaisSede del administrador coincida con Pais del estadio del evento.

DELIMITER //

CREATE TRIGGER tr_evento_admin_pais_insert
BEFORE INSERT ON EVENTO
FOR EACH ROW
BEGIN
    DECLARE v_pais_admin VARCHAR(100);
    DECLARE v_pais_estadio VARCHAR(100);

    SELECT PaisSede
    INTO v_pais_admin
    FROM ADMINISTRADOR
    WHERE Mail_Usuario = NEW.Mail_Administrador;

    SELECT Pais
    INTO v_pais_estadio
    FROM ESTADIO
    WHERE EstadioID = NEW.EstadioID;

    IF v_pais_admin <> v_pais_estadio THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE: el administrador no puede dar de alta eventos fuera de su pais sede';
    END IF;
END //

-- RNE: la misma regla debe cumplirse si se modifica el estadio
-- o el administrador de un evento ya creado.
-- Disparador: BEFORE UPDATE ON EVENTO.

CREATE TRIGGER tr_evento_admin_pais_update
BEFORE UPDATE ON EVENTO
FOR EACH ROW
BEGIN
    DECLARE v_pais_admin VARCHAR(100);
    DECLARE v_pais_estadio VARCHAR(100);

    SELECT PaisSede
    INTO v_pais_admin
    FROM ADMINISTRADOR
    WHERE Mail_Usuario = NEW.Mail_Administrador;

    SELECT Pais
    INTO v_pais_estadio
    FROM ESTADIO
    WHERE EstadioID = NEW.EstadioID;

    IF v_pais_admin <> v_pais_estadio THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE: el administrador no puede modificar eventos fuera de su pais sede';
    END IF;
END //

DELIMITER ;

-- RNE: los sectores habilitados para un evento deben pertenecer
-- al mismo estadio en el que se realiza dicho evento.
-- Disparador: BEFORE INSERT ON EVENTO_SECTOR.
-- Evita habilitar, por error, sectores de otro estadio.

DELIMITER //

CREATE TRIGGER tr_evento_sector_estadio_insert
BEFORE INSERT ON EVENTO_SECTOR
FOR EACH ROW
BEGIN
    DECLARE v_estadio_evento INT UNSIGNED;

    SELECT EstadioID
    INTO v_estadio_evento
    FROM EVENTO
    WHERE EventoID = NEW.EventoID;

    IF NEW.EstadioID <> v_estadio_evento THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE: el sector habilitado no pertenece al estadio del evento';
    END IF;
END //

-- RNE: la misma validación aplica si se modifica un sector habilitado
-- de un evento existente.
-- Disparador: BEFORE UPDATE ON EVENTO_SECTOR.

CREATE TRIGGER tr_evento_sector_estadio_update
BEFORE UPDATE ON EVENTO_SECTOR
FOR EACH ROW
BEGIN
    DECLARE v_estadio_evento INT UNSIGNED;

    SELECT EstadioID
    INTO v_estadio_evento
    FROM EVENTO
    WHERE EventoID = NEW.EventoID;

    IF NEW.EstadioID <> v_estadio_evento THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE: el sector habilitado no pertenece al estadio del evento';
    END IF;
END //

DELIMITER ;

-- RNE: no pueden existir eventos superpuestos en un mismo estadio.
-- Disparador: BEFORE UPDATE ON EVENTO.
-- Nota: ya existe control al insertar; este agrega el mismo control al modificar.
-- Supuesto: se considera una duración estimada de 4 horas por evento.

DELIMITER //

CREATE TRIGGER tr_evento_sin_solapamiento_update
BEFORE UPDATE ON EVENTO
FOR EACH ROW
BEGIN
    DECLARE v_count INT;

    SELECT COUNT(*)
    INTO v_count
    FROM EVENTO
    WHERE EventoID <> OLD.EventoID
      AND EstadioID = NEW.EstadioID
      AND NEW.FechaHora < DATE_ADD(FechaHora, INTERVAL 4 HOUR)
      AND FechaHora < DATE_ADD(NEW.FechaHora, INTERVAL 4 HOUR);

    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE: existe un evento superpuesto en el mismo estadio';
    END IF;
END //

DELIMITER ;