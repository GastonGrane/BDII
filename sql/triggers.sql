-- =============================================================================
-- triggers.sql — Dónde defendemos cada RNE desde la base
-- =============================================================================
-- Mapa rápido de lo que hay en este archivo:
--   Módulo 1 — ENTRADA       → RNE 1 (límite por venta), aforo del sector, RNE 7 (consumida irreversible)
--   Módulo 2 — TRANSFERENCIA → RNE 2 (límite de transferencias), RNE 6 (solo Activas) + máquina de estados
--   Módulo 3 — TOKEN_QR      → un solo token activo por entrada
--   Módulo 4 — VALIDACION    → RNE 9 y RNE 10 (token activo y vigente), RNE 7 (complemento), cierre atómico
--   Módulo 5 — EVENTO        → RNE 4 (eventos sin solaparse) + admin solo en su país sede
--   Módulo 6 — COMISION      → RNE 12 (guard) + SP de alta
--   Módulo 7 — RNE 5         → cobertura por funcionario (vista + SP) y consistencia sector-estadio
--
-- RNE 8 (entrada "al portador"): a propósito NO validamos quién presenta el QR.
--   Cualquiera que tenga un token vigente (activo y dentro de los 30s) puede entrar.
--   No es un control que falte: es una decisión de diseño, por eso acá no hay trigger.
-- =============================================================================

USE CD_Grupo4;
-- =====================================================
-- MÓDULO 1: ENTRADA
-- =====================================================

DELIMITER //

-- =====================================================
-- RNE 1 — máximo 5 entradas por venta
-- =====================================================
-- Acá controlamos que una venta no junte más de 5 entradas.
-- Antes de insertar contamos cuántas ya tiene esa VentaID; si llega a 5, cortamos.
--
-- Ojo con la concurrencia: dos transacciones a la vez sobre la misma venta podrían pasar
-- las dos este chequeo si leen antes de que la otra confirme. Por eso el backend crea la
-- VENTA y sus ENTRADAs en una sola transacción con SELECT ... FOR UPDATE sobre la venta,
-- así las inserciones del mismo comprador se serializan.
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


-- =====================================================
-- Aforo del sector — no se puede sobrevender
-- =====================================================
-- Acá cuidamos que no se vendan más entradas que la capacidad del sector. Antes de
-- insertar contamos las entradas ya emitidas para ese evento+sector y las comparamos
-- contra SECTOR.CapacidadMax.
--
-- Esto NO es RNE 3. RNE 3 ("un evento debe habilitar al menos un sector") no vive en la
-- base: lo resuelve el backend (AdminService.crearEvento), porque EVENTO_SECTOR se inserta
-- después del EVENTO y MySQL no tiene constraints diferidas para chequearlo al vuelo.
--
-- Además del trigger, el backend toma un lock sobre la fila del SECTOR (VentaService) para
-- serializar compras simultáneas del mismo sector. Defensa en dos capas.
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
            SET MESSAGE_TEXT = 'Aforo: capacidad del sector agotada para el evento (sobre-aforo)';
    END IF;
END //


-- =====================================================
-- RNE 7 — una entrada consumida no vuelve atrás
-- =====================================================
-- Esta parte evita que una entrada ya Consumida vuelva a un estado anterior.
-- En cada UPDATE, si estaba Consumida y la quieren cambiar, lo rechazamos.
-- También frena que una transferencia posterior la "reviva" (aunque RNE 6 ya corta antes).
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

-- =====================================================
-- MÓDULO 2: TRANSFERENCIA
-- =====================================================
-- Acá están las dos reglas de transferencia y la máquina de estados de la ENTRADA: los
-- triggers AFTER mantienen EstadoEntrada y Mail_Propietario al día con el ciclo de la
-- transferencia.

DELIMITER //

-- =====================================================
-- RNE 2 — máximo 3 transferencias por entrada
-- =====================================================
-- Acá controlamos que una entrada no se transfiera más de 3 veces.
-- Contamos las Pendiente + Aceptada; las Rechazadas no cuentan, así nadie hace trampa
-- creando y rechazando transferencias para resetear el contador.
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


-- =====================================================
-- RNE 6 — solo se transfieren entradas Activas
-- =====================================================
-- Esta parte evita transferir una entrada que esté PendienteTransferencia o Consumida.
-- Si la entrada no está Activa, la transferencia se rechaza.
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


-- =====================================================
-- Máquina de estados (1/2) — al pedir la transferencia
-- =====================================================
-- Apenas se crea la solicitud, dejamos la ENTRADA en PendienteTransferencia.
-- Ese estado bloquea que entren otras transferencias en paralelo (apoya RNE 6).
CREATE TRIGGER tr_transferencia_marcar_pendiente
AFTER INSERT ON TRANSFERENCIA
FOR EACH ROW
BEGIN
    UPDATE ENTRADA
    SET EstadoEntrada = 'PendienteTransferencia'
    WHERE EntradaID = NEW.EntradaID;
END //


-- =====================================================
-- Máquina de estados (2/2) — al resolver la transferencia
-- =====================================================
-- Si la transferencia pasa de Pendiente a Aceptada, cambiamos el dueño y la entrada
-- vuelve a Activa. Si pasa a Rechazada, solo la volvemos a Activa.
-- Solo actúa cuando venía de Pendiente.
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

-- =====================================================
-- MÓDULO 3: TOKEN_QR — un solo token activo por entrada
-- =====================================================
-- MySQL no nos deja hacer un UNIQUE solo para los tokens activos (no hay índices
-- parciales). Por eso lo defendemos con triggers: una entrada no puede tener dos QR
-- activos a la vez. Hay uno para el INSERT y otro para el UPDATE.

DELIMITER //

-- =====================================================
-- Al insertar un token activo
-- =====================================================
-- Si el token nuevo entra como activo, primero miramos que la entrada no tenga ya otro activo.
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


-- =====================================================
-- Al reactivar un token (UPDATE de FALSE a TRUE)
-- =====================================================
-- Mismo cuidado que arriba: si pasamos un token a activo, ningún otro de esa entrada
-- puede estar activo en ese momento.
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

-- =====================================================
-- MÓDULO 4: VALIDACION — el escaneo en la puerta
-- =====================================================
-- Acá entran RNE 9 y RNE 10 (token activo y vigente), el complemento de RNE 7 y el cierre
-- atómico de la entrada. La PK de VALIDACION (TokenID) es una barrera natural contra la
-- doble validación: InnoDB deja pasar un solo INSERT con el mismo TokenID, el segundo
-- falla por clave duplicada.

DELIMITER //

-- =====================================================
-- RNE 9 y RNE 10 — el token tiene que estar activo y vigente
-- =====================================================
-- Acá controlamos las dos cosas al validar:
--   RNE 9 : si el token no está Activo, lo rechazamos.
--   RNE 10: si el token venció (ExpiraEn <= NOW()), lo rechazamos aunque siga marcado activo.
-- Un token puede quedar activo pero vencido si nadie pidió regenerarlo; este chequeo es la
-- red de seguridad de la ventana de 30s que también controla el backend (TokenService).
-- No molesta a la regeneración: el TokenService apaga el viejo y emite uno nuevo con 30s más.
CREATE TRIGGER tr_validacion_token_activo
BEFORE INSERT ON VALIDACION
FOR EACH ROW
BEGIN
    DECLARE v_activo BOOLEAN;
    DECLARE v_expira DATETIME;
    SELECT Activo, ExpiraEn INTO v_activo, v_expira FROM TOKEN_QR WHERE TokenID = NEW.TokenID;
    IF v_activo != TRUE THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE 9: el token QR no está activo al momento de la validación';
    END IF;
    IF v_expira <= NOW() THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE 10: el token QR está vencido (ventana de 30 segundos)';
    END IF;
END //


-- =====================================================
-- RNE 7 (complemento) — no validar una entrada ya consumida
-- =====================================================
-- Defensa extra: bloquea validar una entrada que ya está Consumida.
-- La PK de VALIDACION ya evita la doble inserción; esto cubre estados inconsistentes.
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


-- =====================================================
-- Cierre atómico de la validación
-- =====================================================
-- Apenas se registra la validación, marcamos la ENTRADA como Consumida y apagamos todos
-- sus tokens. Pasa en la misma transacción que el INSERT, así el backend no tiene que
-- hacer UPDATEs extra ni queda nada a medias.
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

-- =====================================================
-- MÓDULO 5: EVENTO — RNE 4 (eventos sin solaparse)
-- =====================================================
-- Dos eventos no pueden pisarse en el mismo estadio. Suponemos 4 horas de duración por
-- evento (DEC-05). Hay control al crear y, más abajo, también al modificar.

DELIMITER //

-- =====================================================
-- RNE 4 — al crear un evento
-- =====================================================
-- Acá cuidamos que el evento nuevo no se solape con otro del mismo estadio.
-- Dos franjas [A, A+4h) y [B, B+4h) se pisan si A < B+4h y B < A+4h.
CREATE TRIGGER tr_evento_sin_solapamiento
BEFORE INSERT ON EVENTO
FOR EACH ROW
BEGIN
    DECLARE v_count INT;
    SELECT COUNT(*) INTO v_count
    FROM EVENTO
    WHERE EstadioID = NEW.EstadioID
      AND NEW.FechaHora < DATE_ADD(FechaHora, INTERVAL 4 HOUR)
      AND FechaHora     < DATE_ADD(NEW.FechaHora, INTERVAL 4 HOUR);
    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'RNE 4: existe un evento solapado en el mismo estadio (ventana de 4 horas)';
    END IF;
END //


DELIMITER ;

-- =====================================================
-- MÓDULO 6: COMISION — RNE 12
-- =====================================================
-- La regla es que no haya dos comisiones vigentes a la vez. Lo resolvemos en dos partes:
--   a) Trigger BEFORE INSERT: la red de seguridad contra cualquier INSERT que se solape.
--   b) SP sp_nueva_comision: el camino correcto, que cierra la anterior y abre la nueva.
--
-- El cierre automático de la anterior va en el SP y no en el trigger porque MySQL no deja
-- hacer UPDATE sobre la misma tabla dentro de un trigger.
--
-- Concurrencia: sp_nueva_comision se llama dentro de una transacción explícita para que
-- dos llamadas a la vez no terminen dejando dos comisiones activas.

DELIMITER //

-- =====================================================
-- RNE 12 — comisión vigente sin pisarse
-- =====================================================
-- Acá cuidamos que no queden dos comisiones activas al mismo tiempo, ni que una nueva se
-- solape con una ya cerrada. Vale tanto para INSERTs directos como para los que hace el SP.
-- El SP es la forma correcta de dar de alta; este trigger es la red de seguridad.
CREATE TRIGGER tr_comision_sin_solapamiento
BEFORE INSERT ON COMISION
FOR EACH ROW
BEGIN
    DECLARE v_count INT;

    -- Caso 1: ya hay una vigente (F_Hasta NULL) y quieren meter otra vigente
    IF NEW.F_Hasta IS NULL THEN
        SELECT COUNT(*) INTO v_count FROM COMISION WHERE F_Hasta IS NULL;
        IF v_count > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'RNE 12: ya existe una comisión activa. Usar sp_nueva_comision.';
        END IF;
    END IF;

    -- Caso 2: la nueva se pisaría con una comisión ya cerrada
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

-- =====================================================
-- SP sp_nueva_comision — alta correcta de una comisión
-- =====================================================
-- Cierra la comisión vigente (si hay) y abre la nueva, todo en una transacción.
-- El EXIT HANDLER hace ROLLBACK y reenvía el error, así nunca queda el cierre aplicado sin
-- la nueva comisión (PENDIENTE-02 resuelto).
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
        -- Cierra la vigente fijándole F_Hasta = inicio de la nueva
        UPDATE COMISION SET F_Hasta = p_desde WHERE F_Hasta IS NULL;
        -- Abre la nueva (el trigger tr_comision_sin_solapamiento valida el estado)
        INSERT INTO COMISION (Porcentaje, F_Desde, F_Hasta)
        VALUES (p_porcentaje, p_desde, NULL);
    COMMIT;
END //

DELIMITER ;


-- =====================================================
-- MÓDULO 7: RNE 5 — cobertura de sectores por funcionario
-- =====================================================
-- Esto no se puede hacer con trigger: la regla es sobre algo que falta (un sector asignado
-- que nadie validó), no sobre una fila que se inserta. Por eso lo armamos como vista de
-- auditoría + SP de consulta (DEC-07).

-- =====================================================
-- Vista v_cobertura_funcionario — quién cubrió qué
-- =====================================================
-- Por cada sector asignado a un funcionario, Cubierto = 1 si hizo al menos una validación
-- en ese sector del evento.
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


-- =====================================================
-- SP sp_verificar_cobertura — sectores sin cubrir de un evento
-- =====================================================
-- Devuelve los funcionarios que tenían un sector asignado y todavía no validaron nada ahí.
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

-- =====================================================
-- Un admin solo crea eventos en su país sede
-- =====================================================
-- Acá cuidamos que el administrador no dé de alta eventos en estadios de otro país:
-- comparamos su PaisSede contra el Pais del estadio del evento.

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

-- =====================================================
-- Lo mismo al modificar un evento
-- =====================================================
-- Si cambian el estadio o el administrador de un evento ya creado, vuelve a chequear que
-- el admin siga siendo del país del estadio.
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

-- =====================================================
-- RNE 3 (la parte que sí vive en la base) — sectores del mismo estadio
-- =====================================================
-- Acá cuidamos que los sectores que se habilitan para un evento sean del estadio donde se
-- juega, y no de otro por error. Comparamos el EstadioID del sector contra el del evento.
-- Hay versión para INSERT y para UPDATE.
--
-- Aclaración sobre RNE 3: la otra parte ("un evento debe habilitar al menos un sector") no
-- está acá, se resuelve en backend (no se puede con trigger porque EVENTO_SECTOR se inserta
-- después del EVENTO). En la base solo garantizamos la consistencia sector-estadio.

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

-- =====================================================
-- Lo mismo al modificar un sector habilitado
-- =====================================================
-- Si cambian un EVENTO_SECTOR ya existente, vuelve a chequear que el sector pertenezca al
-- estadio del evento.
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

-- =====================================================
-- RNE 4 — también al modificar un evento
-- =====================================================
-- El control de solapamiento al insertar ya existe; este lo repite al modificar.
-- Si cambian la fecha o el estadio de un evento, no puede quedar pisado con otro.
-- Mismo supuesto: 4 horas de duración por evento.

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
