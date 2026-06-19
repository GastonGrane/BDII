-- =============================================================================
-- Sistema de Ticketing — Mundial 2026
-- Grupo 4: Sharon Bentos, Gaston Grané, Axel Hernández
-- Base de datos: CD_Grupo4 (MySQL, host mysql.reto-ucu.net:50006)
--
-- Importante: correr los bloques en orden, cada uno se apoya en los de arriba.
-- Usamos InnoDB para tener FK y transacciones, y utf8mb4 para acentos y emojis.
-- =============================================================================

USE CD_Grupo4;

-- -----------------------------------------------------------------------------
-- MÓDULO 1: USUARIOS
-- Orden de creación: USUARIO → subtipos → TELEFONO
-- -----------------------------------------------------------------------------

-- =====================================================
-- USUARIO: la tabla padre de todos los roles
-- =====================================================
-- Acá guardamos lo común a cualquier usuario: login, documento y dirección.
-- El Mail es la PK y también la van a usar los subtipos, así armamos la herencia.
-- El UNIQUE de (PaisDoc, TipoDoc, NroDoc) defiende que un mismo documento no
-- aparezca en dos personas distintas. CodPostal queda opcional porque no todos
-- los países lo usan.
CREATE TABLE IF NOT EXISTS USUARIO (
    Mail              VARCHAR(254)                     NOT NULL,
    Contrasena        VARCHAR(255)                     NOT NULL,
    PaisDoc           VARCHAR(50)                      NOT NULL,
    TipoDoc           ENUM('CI', 'Pasaporte', 'Otro') NOT NULL,
    NroDoc            VARCHAR(20)                      NOT NULL,
    PaisDir           VARCHAR(50)                      NOT NULL,
    Localidad         VARCHAR(100)                     NOT NULL,
    Calle             VARCHAR(150)                     NOT NULL,
    NroPuerta         VARCHAR(20)                      NOT NULL,
    CodPostal         VARCHAR(10)                      NULL,

    CONSTRAINT pk_usuario PRIMARY KEY (Mail),
    CONSTRAINT uq_usuario_documento UNIQUE (PaisDoc, TipoDoc, NroDoc)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================
-- Subtipos de USUARIO (herencia "tabla por subclase")
-- =====================================================
-- Cada subtipo usa el mismo Mail como PK y, a la vez, como FK hacia USUARIO.
-- Así la especialización queda disjunta y total: una fila de subtipo siempre
-- apunta a un USUARIO que existe.

-- ADMINISTRADOR: el rol que da de alta estadios y eventos.
CREATE TABLE IF NOT EXISTS ADMINISTRADOR (
    Mail_Usuario      VARCHAR(254)  NOT NULL,
    PaisSede          VARCHAR(100)  NOT NULL,
    FechaAsignacion   DATE          NOT NULL,

    CONSTRAINT pk_administrador PRIMARY KEY (Mail_Usuario),
    CONSTRAINT fk_admin_usuario  FOREIGN KEY (Mail_Usuario)
        REFERENCES USUARIO(Mail)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- FUNCIONARIO: el rol que valida entradas en el estadio con un dispositivo asignado.
CREATE TABLE IF NOT EXISTS FUNCIONARIO (
    Mail_Usuario  VARCHAR(254)  NOT NULL,
    NroLegajo     VARCHAR(20)   NOT NULL,

    CONSTRAINT pk_funcionario   PRIMARY KEY (Mail_Usuario),
    CONSTRAINT uq_funcionario_legajo UNIQUE (NroLegajo),
    CONSTRAINT fk_func_usuario  FOREIGN KEY (Mail_Usuario)
        REFERENCES USUARIO(Mail)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- USUARIO_GENERAL: el comprador final. EstadoVerificacion arranca en 'Pendiente'.
-- (El dominio de ese estado quedó como supuesto del modelo lógico — DEC-01.)
CREATE TABLE IF NOT EXISTS USUARIO_GENERAL (
    Mail_Usuario        VARCHAR(254)                             NOT NULL,
    FechaRegistro       DATE                                     NOT NULL,
    EstadoVerificacion  ENUM('Pendiente', 'Verificado', 'Rechazado')
                                                                 NOT NULL
                                                                 DEFAULT 'Pendiente',

    CONSTRAINT pk_usuario_general PRIMARY KEY (Mail_Usuario),
    CONSTRAINT fk_ug_usuario      FOREIGN KEY (Mail_Usuario)
        REFERENCES USUARIO(Mail)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================
-- TELEFONO: los teléfonos de un usuario
-- =====================================================
-- Como un usuario puede tener varios, va en una tabla aparte 1:N.
-- El borrado es CASCADE: si se elimina el usuario, se van también sus teléfonos.
CREATE TABLE IF NOT EXISTS TELEFONO (
    Mail_Usuario  VARCHAR(254)  NOT NULL,
    Telefono      VARCHAR(20)   NOT NULL,

    CONSTRAINT pk_telefono       PRIMARY KEY (Mail_Usuario, Telefono),
    CONSTRAINT fk_tel_usuario    FOREIGN KEY (Mail_Usuario)
        REFERENCES USUARIO(Mail)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- -----------------------------------------------------------------------------
-- MÓDULO 2: INFRAESTRUCTURA Y EVENTOS
-- Orden de creación: ESTADIO → SECTOR → EVENTO → EVENTO_SECTOR
-- -----------------------------------------------------------------------------

-- =====================================================
-- ESTADIO y sus SECTOR
-- =====================================================
-- El estadio es la sede física. Sus sectores (A a D) son entidad débil: dependen
-- del estadio y tienen PK compuesta (EstadioID, LetraSector).
-- Los CHECK defienden que la capacidad y el costo siempre sean mayores a 0.
CREATE TABLE IF NOT EXISTS ESTADIO (
    EstadioID  INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    Nombre     VARCHAR(100)   NOT NULL,
    Pais       VARCHAR(50)    NOT NULL,
    Ciudad     VARCHAR(100)   NOT NULL,

    CONSTRAINT pk_estadio PRIMARY KEY (EstadioID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS SECTOR (
    EstadioID     INT UNSIGNED          NOT NULL,
    LetraSector   ENUM('A','B','C','D') NOT NULL,
    CapacidadMax  INT                   NOT NULL,
    CostoEntrada  DECIMAL(10,2)         NOT NULL,

    CONSTRAINT pk_sector        PRIMARY KEY (EstadioID, LetraSector),
    CONSTRAINT fk_sector_estadio FOREIGN KEY (EstadioID)
        REFERENCES ESTADIO(EstadioID)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_capacidad    CHECK (CapacidadMax > 0),
    CONSTRAINT chk_costo_sector CHECK (CostoEntrada > 0.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================
-- EVENTO: un partido en un estadio
-- =====================================================
-- Cada evento apunta al estadio donde se juega y al administrador que lo creó.
-- Usamos DATETIME (no TIMESTAMP) para no pelearnos con zonas horarias.
--
-- RNE 4 (dos eventos no pueden solaparse en el mismo estadio): no se puede escribir
-- como CHECK, así que la defiende el trigger tr_evento_sin_solapamiento (ver triggers.sql).
-- El índice por estadio+fecha es para que ese chequeo sea rápido.
CREATE TABLE IF NOT EXISTS EVENTO (
    EventoID          INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    EquipoLocal       VARCHAR(100)   NOT NULL,
    EquipoVisitante   VARCHAR(100)   NOT NULL,
    FechaHora         DATETIME       NOT NULL,
    EstadioID         INT UNSIGNED   NOT NULL,
    Mail_Administrador VARCHAR(254)  NOT NULL,

    CONSTRAINT pk_evento          PRIMARY KEY (EventoID),
    CONSTRAINT fk_evento_estadio  FOREIGN KEY (EstadioID)
        REFERENCES ESTADIO(EstadioID)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_evento_admin    FOREIGN KEY (Mail_Administrador)
        REFERENCES ADMINISTRADOR(Mail_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_evento_estadio_fecha (EstadioID, FechaHora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================
-- EVENTO_SECTOR: qué sectores se habilitan para cada evento
-- =====================================================
-- Es la relación N:N entre EVENTO y SECTOR. La necesitamos como tabla propia porque
-- ENTRADA y ASIGNACION_FUNCIONARIO apuntan al par evento+sector como una unidad.
--
-- Cuidado: el EstadioID de acá tiene que ser el mismo que el del EVENTO. Eso no se
-- puede atar con una FK simple, así que lo defiende el trigger
-- tr_evento_sector_estadio_insert (ver triggers.sql).
CREATE TABLE IF NOT EXISTS EVENTO_SECTOR (
    EventoID     INT UNSIGNED          NOT NULL,
    EstadioID    INT UNSIGNED          NOT NULL,
    LetraSector  ENUM('A','B','C','D') NOT NULL,

    CONSTRAINT pk_evento_sector       PRIMARY KEY (EventoID, EstadioID, LetraSector),
    CONSTRAINT fk_es_evento           FOREIGN KEY (EventoID)
        REFERENCES EVENTO(EventoID)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_es_sector           FOREIGN KEY (EstadioID, LetraSector)
        REFERENCES SECTOR(EstadioID, LetraSector)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- -----------------------------------------------------------------------------
-- MÓDULO 3: VENTAS Y ENTRADAS
-- Orden de creación: COMISION → VENTA → ENTRADA → TRANSFERENCIA
-- -----------------------------------------------------------------------------

-- =====================================================
-- COMISION: el porcentaje que se cobra, con vigencia en el tiempo
-- =====================================================
-- Es una entidad propia (Corrección 3 del MER) porque la comisión cambia con el tiempo.
-- F_Hasta en NULL marca la comisión que está vigente hoy.
--
-- RNE 12 (no puede haber dos comisiones pisándose): no se puede como CHECK, la defiende
-- el trigger tr_comision_sin_solapamiento (ver triggers.sql). El índice por vigencia
-- ayuda a ese chequeo.
CREATE TABLE IF NOT EXISTS COMISION (
    ComisionID  INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    Porcentaje  DECIMAL(5,2)   NOT NULL,
    F_Desde     DATETIME       NOT NULL,
    F_Hasta     DATETIME       NULL,

    CONSTRAINT pk_comision    PRIMARY KEY (ComisionID),
    CONSTRAINT chk_porcentaje CHECK (Porcentaje > 0.00),
    INDEX idx_comision_vigencia (F_Desde, F_Hasta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================
-- VENTA: la compra que hace un usuario general
-- =====================================================
-- No guardamos el MontoTotal: es calculable, lo resuelve la vista
-- v_monto_total_venta (DEC-03).
--
-- RNE 1 (máximo 5 entradas por venta): no se puede como CHECK acá, la defiende el
-- trigger tr_entrada_limite_venta cuando se insertan las entradas (ver triggers.sql).
CREATE TABLE IF NOT EXISTS VENTA (
    VentaID        INT UNSIGNED                          NOT NULL AUTO_INCREMENT,
    Fecha          DATETIME                              NOT NULL,
    Estado         ENUM('Pendiente', 'Confirmada', 'Paga') NOT NULL,
    Mail_Comprador VARCHAR(254)                          NOT NULL,
    ComisionID     INT UNSIGNED                          NOT NULL,

    CONSTRAINT pk_venta           PRIMARY KEY (VentaID),
    CONSTRAINT fk_venta_comprador FOREIGN KEY (Mail_Comprador)
        REFERENCES USUARIO_GENERAL(Mail_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_venta_comision  FOREIGN KEY (ComisionID)
        REFERENCES COMISION(ComisionID)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_venta_comprador (Mail_Comprador),
    INDEX idx_venta_fecha (Fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================
-- ENTRADA: el corazón del sistema
-- =====================================================
-- Junta tres relaciones: de qué VENTA salió, a qué EVENTO_SECTOR corresponde y quién
-- es su propietario actual. Mail_Propietario puede cambiar tras una transferencia, por
-- eso a veces no coincide con el comprador original.
-- Costo_Historico es una foto del precio del sector al momento de comprar (Ajuste 3 del
-- MER), así un cambio de precio futuro no altera las ventas viejas.
CREATE TABLE IF NOT EXISTS ENTRADA (
    EntradaID       INT UNSIGNED                                           NOT NULL AUTO_INCREMENT,
    EstadoEntrada   ENUM('Activa', 'PendienteTransferencia', 'Consumida') NOT NULL,
    Costo_Historico DECIMAL(10,2)                                         NOT NULL,
    VentaID         INT UNSIGNED                                          NOT NULL,
    EventoID        INT UNSIGNED                                          NOT NULL,
    EstadioID       INT UNSIGNED                                          NOT NULL,
    LetraSector     ENUM('A','B','C','D')                                 NOT NULL,
    Mail_Propietario VARCHAR(254)                                         NOT NULL,

    CONSTRAINT pk_entrada            PRIMARY KEY (EntradaID),
    CONSTRAINT chk_costo_historico   CHECK (Costo_Historico > 0.00),
    CONSTRAINT fk_entrada_venta      FOREIGN KEY (VentaID)
        REFERENCES VENTA(VentaID)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_entrada_es         FOREIGN KEY (EventoID, EstadioID, LetraSector)
        REFERENCES EVENTO_SECTOR(EventoID, EstadioID, LetraSector)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_entrada_propietario FOREIGN KEY (Mail_Propietario)
        REFERENCES USUARIO_GENERAL(Mail_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    INDEX idx_entrada_venta      (VentaID),
    INDEX idx_entrada_propietario (Mail_Propietario),
    INDEX idx_entrada_es          (EventoID, EstadioID, LetraSector)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================
-- TRANSFERENCIA: el historial de cambios de dueño de una entrada
-- =====================================================
-- Origen y destino apuntan a USUARIO (la superclase), porque así lo modela el MER.
--
-- Dos reglas que se defienden con triggers al insertar (ver triggers.sql):
--   RNE 2: una entrada no puede tener más de 3 transferencias → tr_transferencia_limite
--   RNE 6: solo se transfieren entradas Activas               → tr_transferencia_entrada_activa
CREATE TABLE IF NOT EXISTS TRANSFERENCIA (
    TransfID     INT UNSIGNED                              NOT NULL AUTO_INCREMENT,
    FechaSol     DATETIME                                  NOT NULL,
    Estado       ENUM('Pendiente', 'Aceptada', 'Rechazada') NOT NULL,
    EntradaID    INT UNSIGNED                              NOT NULL,
    Mail_Origen  VARCHAR(254)                              NOT NULL,
    Mail_Destino VARCHAR(254)                              NOT NULL,

    CONSTRAINT pk_transferencia       PRIMARY KEY (TransfID),
    CONSTRAINT fk_transf_entrada      FOREIGN KEY (EntradaID)
        REFERENCES ENTRADA(EntradaID)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_transf_origen       FOREIGN KEY (Mail_Origen)
        REFERENCES USUARIO(Mail)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_transf_destino      FOREIGN KEY (Mail_Destino)
        REFERENCES USUARIO(Mail)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_transf_entrada (EntradaID),
    INDEX idx_transf_origen  (Mail_Origen),
    INDEX idx_transf_destino (Mail_Destino)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- -----------------------------------------------------------------------------
-- MÓDULO 4: SEGURIDAD Y VALIDACIÓN
-- Orden de creación: TOKEN_QR → DISPOSITIVO → VALIDACION → ASIGNACION_FUNCIONARIO
-- -----------------------------------------------------------------------------

-- =====================================================
-- TOKEN_QR: el QR dinámico de cada entrada (RNE 10)
-- =====================================================
-- Cada entrada activa tiene un token que vive 30 segundos (ExpiraEn = GeneradoEn + 30s).
-- Cuando el cliente lo pide y ya venció, el backend apaga el viejo y genera uno nuevo
-- (ver TokenService); los anteriores quedan como historial. La limpieza de tokens
-- viejos quedó como mejora futura (DEC-02).
--
-- El CHECK chk_token_ventana defiende que ExpiraEn sea posterior a GeneradoEn.
-- Que haya un solo token activo por entrada no se puede con un UNIQUE (MySQL no tiene
-- índices parciales), así que lo defienden los triggers tr_token_unico_activo_insert
-- y tr_token_unico_activo_update. El índice (EntradaID, Activo) hace rápido encontrar
-- el token activo al validar (RNE 9).
CREATE TABLE IF NOT EXISTS TOKEN_QR (
    TokenID     INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    CodigoQR    VARCHAR(500)   NOT NULL,
    GeneradoEn  DATETIME       NOT NULL,
    ExpiraEn    DATETIME       NOT NULL,
    Activo      BOOLEAN        NOT NULL DEFAULT FALSE,
    EntradaID   INT UNSIGNED   NOT NULL,

    CONSTRAINT pk_token_qr       PRIMARY KEY (TokenID),
    CONSTRAINT uq_token_codigo   UNIQUE (CodigoQR),
    CONSTRAINT chk_token_ventana CHECK (ExpiraEn > GeneradoEn),
    CONSTRAINT fk_token_entrada  FOREIGN KEY (EntradaID)
        REFERENCES ENTRADA(EntradaID)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_token_entrada_activo (EntradaID, Activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================
-- DISPOSITIVO: el lector que usa el funcionario (RNE 11)
-- =====================================================
-- La FK Mail_Funcionario es NOT NULL: un dispositivo nunca queda suelto, siempre
-- pertenece a un funcionario antes de poder usarse (RNE 11, Ajuste 2 del MER).
CREATE TABLE IF NOT EXISTS DISPOSITIVO (
    DispositivoID    INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    Mail_Funcionario VARCHAR(254)   NOT NULL,

    CONSTRAINT pk_dispositivo     PRIMARY KEY (DispositivoID),
    CONSTRAINT fk_disp_funcionario FOREIGN KEY (Mail_Funcionario)
        REFERENCES FUNCIONARIO(Mail_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_disp_funcionario (Mail_Funcionario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================
-- VALIDACION: el escaneo del QR en la puerta (relación ternaria Valida)
-- =====================================================
-- Junta token + funcionario + dispositivo (Ajuste 1 del MER). La PK es solo TokenID
-- porque un token se valida una sola vez: eso ya frena la doble validación.
--
-- Reglas que se defienden con triggers al insertar (ver triggers.sql):
--   RNE 9: el token tiene que estar activo y vigente → tr_validacion_token_activo
--   RNE 7: no se valida una entrada ya consumida     → tr_validacion_entrada_no_consumida
CREATE TABLE IF NOT EXISTS VALIDACION (
    TokenID          INT UNSIGNED   NOT NULL,
    Mail_Funcionario VARCHAR(254)   NOT NULL,
    DispositivoID    INT UNSIGNED   NOT NULL,
    FechaHora        DATETIME       NOT NULL,

    CONSTRAINT pk_validacion        PRIMARY KEY (TokenID),
    CONSTRAINT fk_val_token         FOREIGN KEY (TokenID)
        REFERENCES TOKEN_QR(TokenID)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_val_funcionario   FOREIGN KEY (Mail_Funcionario)
        REFERENCES FUNCIONARIO(Mail_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_val_dispositivo   FOREIGN KEY (DispositivoID)
        REFERENCES DISPOSITIVO(DispositivoID)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_val_funcionario  (Mail_Funcionario),
    INDEX idx_val_dispositivo  (DispositivoID),
    INDEX idx_val_fechahora    (FechaHora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================
-- ASIGNACION_FUNCIONARIO: qué funcionario cubre qué sector de qué evento
-- =====================================================
-- Es la relación N:N entre FUNCIONARIO y EVENTO_SECTOR. Es la base para controlar la
-- cobertura (RNE 5, ver la vista v_cobertura_funcionario en triggers.sql).
CREATE TABLE IF NOT EXISTS ASIGNACION_FUNCIONARIO (
    Mail_Funcionario  VARCHAR(254)          NOT NULL,
    EventoID          INT UNSIGNED          NOT NULL,
    EstadioID         INT UNSIGNED          NOT NULL,
    LetraSector       ENUM('A','B','C','D') NOT NULL,

    CONSTRAINT pk_asignacion           PRIMARY KEY (Mail_Funcionario, EventoID, EstadioID, LetraSector),
    CONSTRAINT fk_asig_funcionario     FOREIGN KEY (Mail_Funcionario)
        REFERENCES FUNCIONARIO(Mail_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_asig_evento_sector   FOREIGN KEY (EventoID, EstadioID, LetraSector)
        REFERENCES EVENTO_SECTOR(EventoID, EstadioID, LetraSector)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_asig_funcionario (Mail_Funcionario),
    INDEX idx_asig_evento      (EventoID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =============================================================================
-- VISTA v_monto_total_venta: el total de cada venta calculado al vuelo
-- =============================================================================
-- No guardamos el monto en VENTA; lo calculamos acá sumando el Costo_Historico de las
-- entradas y aplicándole la comisión: SUM(costo) * (1 + Porcentaje/100).
-- (Ver Parte III §3.4 del informe.)

CREATE OR REPLACE VIEW v_monto_total_venta AS
SELECT
    v.VentaID,
    v.Fecha,
    v.Estado,
    v.Mail_Comprador,
    v.ComisionID,
    c.Porcentaje                                 AS Comision_Pct,
    SUM(e.Costo_Historico)                       AS Subtotal,
    SUM(e.Costo_Historico) * (1 + c.Porcentaje / 100) AS MontoTotal
FROM VENTA v
JOIN COMISION c  ON v.ComisionID   = c.ComisionID
JOIN ENTRADA  e  ON e.VentaID      = v.VentaID
GROUP BY v.VentaID, v.Fecha, v.Estado, v.Mail_Comprador, v.ComisionID, c.Porcentaje;
