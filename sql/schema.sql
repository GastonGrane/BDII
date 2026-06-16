-- =============================================================================
-- Sistema de Ticketing — Mundial 2026
-- Grupo 4: Sharon Bentos, Gaston Grané, Axel Hernández
-- Base de datos: IC_Grupo4 (MySQL, host mysql.reto-ucu.net:50006)
--
-- Ejecutar en orden: cada bloque depende de los anteriores.
-- Motor: InnoDB (soporte de FK y transacciones).
-- Juego de caracteres: utf8mb4 (soporta emojis y caracteres internacionales).
-- =============================================================================

USE CD_Grupo4;

-- -----------------------------------------------------------------------------
-- MÓDULO 1: USUARIOS
-- Orden: USUARIO → subtipos → TELEFONO
-- -----------------------------------------------------------------------------

-- USUARIO: superclase del sistema de herencia. Contiene datos comunes a todos los roles
-- (credenciales, documento de identidad, dirección). Su PK (Mail) es también PK de los subtipos.
CREATE TABLE IF NOT EXISTS USUARIO (
    Mail              VARCHAR(254)                     NOT NULL,
    Contrasena        VARCHAR(255)                     NOT NULL,
    PaisDoc           VARCHAR(50)                      NOT NULL,
    -- RNE TipoDoc: dominio fijo {CI, Pasaporte, Otro}
    TipoDoc           ENUM('CI', 'Pasaporte', 'Otro') NOT NULL,
    NroDoc            VARCHAR(20)                      NOT NULL,
    PaisDir           VARCHAR(50)                      NOT NULL,
    Localidad         VARCHAR(100)                     NOT NULL,
    Calle             VARCHAR(150)                     NOT NULL,
    NroPuerta         VARCHAR(20)                      NOT NULL,
    -- CodPostal nullable: no todos los países lo requieren
    CodPostal         VARCHAR(10)                      NULL,

    CONSTRAINT pk_usuario PRIMARY KEY (Mail),
    -- Clave alternativa: un documento pertenece a una sola persona
    CONSTRAINT uq_usuario_documento UNIQUE (PaisDoc, TipoDoc, NroDoc)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- Patrón "tabla por subclase": PK de cada subtipo es también FK hacia USUARIO.
-- Garantiza la especialización disjunta y total del MER.

-- ADMINISTRADOR: subtipo de USUARIO que da de alta estadios y eventos. PK = FK hacia USUARIO.
CREATE TABLE IF NOT EXISTS ADMINISTRADOR (
    Mail_Usuario      VARCHAR(254)  NOT NULL,
    PaisSede          VARCHAR(100)  NOT NULL,
    FechaAsignacion   DATE          NOT NULL,

    CONSTRAINT pk_administrador PRIMARY KEY (Mail_Usuario),
    CONSTRAINT fk_admin_usuario  FOREIGN KEY (Mail_Usuario)
        REFERENCES USUARIO(Mail)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- FUNCIONARIO: subtipo de USUARIO que valida entradas en el estadio con un dispositivo asignado.
CREATE TABLE IF NOT EXISTS FUNCIONARIO (
    Mail_Usuario  VARCHAR(254)  NOT NULL,
    NroLegajo     VARCHAR(20)   NOT NULL,

    CONSTRAINT pk_funcionario   PRIMARY KEY (Mail_Usuario),
    CONSTRAINT uq_funcionario_legajo UNIQUE (NroLegajo),
    CONSTRAINT fk_func_usuario  FOREIGN KEY (Mail_Usuario)
        REFERENCES USUARIO(Mail)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS USUARIO_GENERAL (
    Mail_Usuario        VARCHAR(254)                             NOT NULL,
    FechaRegistro       DATE                                     NOT NULL,
    -- DEC-01: dominio definido como supuesto en el modelo lógico (ver docs/decisiones.md)
    EstadoVerificacion  ENUM('Pendiente', 'Verificado', 'Rechazado')
                                                                 NOT NULL
                                                                 DEFAULT 'Pendiente',

    CONSTRAINT pk_usuario_general PRIMARY KEY (Mail_Usuario),
    CONSTRAINT fk_ug_usuario      FOREIGN KEY (Mail_Usuario)
        REFERENCES USUARIO(Mail)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- TELEFONO: atributo multivaluado de USUARIO → tabla 1:N
CREATE TABLE IF NOT EXISTS TELEFONO (
    Mail_Usuario  VARCHAR(254)  NOT NULL,
    Telefono      VARCHAR(20)   NOT NULL,

    CONSTRAINT pk_telefono       PRIMARY KEY (Mail_Usuario, Telefono),
    CONSTRAINT fk_tel_usuario    FOREIGN KEY (Mail_Usuario)
        REFERENCES USUARIO(Mail)
        ON DELETE CASCADE ON UPDATE CASCADE
        -- CASCADE: si se borra un usuario, se borran sus teléfonos
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- -----------------------------------------------------------------------------
-- MÓDULO 2: INFRAESTRUCTURA Y EVENTOS
-- Orden: ESTADIO → SECTOR → EVENTO → EVENTO_SECTOR
-- -----------------------------------------------------------------------------

-- ESTADIO: sede física de los partidos. Sus sectores (A-D) son entidades débiles con PK compuesta.
CREATE TABLE IF NOT EXISTS ESTADIO (
    EstadioID  INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    Nombre     VARCHAR(100)   NOT NULL,
    Pais       VARCHAR(50)    NOT NULL,
    Ciudad     VARCHAR(100)   NOT NULL,

    CONSTRAINT pk_estadio PRIMARY KEY (EstadioID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- SECTOR: entidad débil de ESTADIO — PK compuesta (EstadioID, LetraSector)
CREATE TABLE IF NOT EXISTS SECTOR (
    EstadioID     INT UNSIGNED          NOT NULL,
    -- RNE LetraSector: dominio fijo {A, B, C, D}
    LetraSector   ENUM('A','B','C','D') NOT NULL,
    -- RNE CapacidadMax: entero mayor a 0
    CapacidadMax  INT                   NOT NULL,
    -- RNE CostoEntrada: valor numérico mayor a 0
    CostoEntrada  DECIMAL(10,2)         NOT NULL,

    CONSTRAINT pk_sector        PRIMARY KEY (EstadioID, LetraSector),
    CONSTRAINT fk_sector_estadio FOREIGN KEY (EstadioID)
        REFERENCES ESTADIO(EstadioID)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_capacidad    CHECK (CapacidadMax > 0),
    CONSTRAINT chk_costo_sector CHECK (CostoEntrada > 0.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- EVENTO: FK a ESTADIO (SeRealizaEn) y a ADMINISTRADOR (DaDeAlta)
CREATE TABLE IF NOT EXISTS EVENTO (
    EventoID          INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    EquipoLocal       VARCHAR(100)   NOT NULL,
    EquipoVisitante   VARCHAR(100)   NOT NULL,
    -- DATETIME en lugar de TIMESTAMP: evita conversiones de zona horaria en sistema internacional
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
    -- RNE 4 (no superposición de eventos en mismo estadio): no expresable como CHECK,
    -- se implementa con trigger tr_evento_no_solapamiento (ver triggers.sql)
    INDEX idx_evento_estadio_fecha (EstadioID, FechaHora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- EVENTO_SECTOR: agregación de la relación N:N Habilita (EVENTO × SECTOR)
-- Necesaria porque ENTRADA y ASIGNACION_FUNCIONARIO referencian el par evento+sector como unidad.
-- Restricción implícita: EstadioID de EVENTO_SECTOR debe coincidir con el de EVENTO.
-- No expresable como FK simple → se valida en trigger tr_evento_sector_estadio_consistente.
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
-- Orden: COMISION → VENTA → ENTRADA → TRANSFERENCIA
-- -----------------------------------------------------------------------------

-- COMISION: Corrección 3 del MER — entidad propia con vigencia temporal
CREATE TABLE IF NOT EXISTS COMISION (
    ComisionID  INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    -- RNE Porcentaje: valor numérico mayor a 0
    Porcentaje  DECIMAL(5,2)   NOT NULL,
    F_Desde     DATETIME       NOT NULL,
    -- F_Hasta NULL indica comisión actualmente vigente (RNE 12)
    F_Hasta     DATETIME       NULL,

    CONSTRAINT pk_comision    PRIMARY KEY (ComisionID),
    CONSTRAINT chk_porcentaje CHECK (Porcentaje > 0.00),
    -- RNE 12 (sin solapamiento de vigencias): no expresable como CHECK,
    -- se implementa con trigger tr_comision_sin_solapamiento (ver triggers.sql)
    INDEX idx_comision_vigencia (F_Desde, F_Hasta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- VENTA: sin columna MontoTotal (atributo calculable, ver DEC-03 en decisiones.md)
CREATE TABLE IF NOT EXISTS VENTA (
    VentaID        INT UNSIGNED                          NOT NULL AUTO_INCREMENT,
    Fecha          DATETIME                              NOT NULL,
    -- RNE Estado de VENTA: dominio fijo {Pendiente, Confirmada, Paga}
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
    -- RNE 1 (máx. 5 entradas por venta): no expresable como CHECK aquí,
    -- se implementa con trigger tr_entrada_limite_por_venta (ver triggers.sql)
    INDEX idx_venta_comprador (Mail_Comprador),
    INDEX idx_venta_fecha (Fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ENTRADA: entidad central del sistema
-- Reúne 3 FK (Genera→VENTA, CorrespondeA→EVENTO_SECTOR, Posee→USUARIO_GENERAL)
-- más el snapshot Costo_Historico (Ajuste 3 del MER final).
CREATE TABLE IF NOT EXISTS ENTRADA (
    EntradaID       INT UNSIGNED                                           NOT NULL AUTO_INCREMENT,
    -- RNE Estado de ENTRADA: dominio fijo {Activa, PendienteTransferencia, Consumida}
    EstadoEntrada   ENUM('Activa', 'PendienteTransferencia', 'Consumida') NOT NULL,
    -- Ajuste 3 MER: snapshot del costo del sector al momento de la compra
    Costo_Historico DECIMAL(10,2)                                         NOT NULL,
    VentaID         INT UNSIGNED                                          NOT NULL,
    EventoID        INT UNSIGNED                                          NOT NULL,
    EstadioID       INT UNSIGNED                                          NOT NULL,
    LetraSector     ENUM('A','B','C','D')                                 NOT NULL,
    -- Posee: propietario actual (puede diferir del comprador tras transferencias)
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


-- TRANSFERENCIA: log histórico de cambios de propietario
-- EsOrigen y EsDestino referencian USUARIO (no USUARIO_GENERAL) porque el MER
-- modela ambas relaciones desde la superclase USUARIO.
CREATE TABLE IF NOT EXISTS TRANSFERENCIA (
    TransfID     INT UNSIGNED                              NOT NULL AUTO_INCREMENT,
    FechaSol     DATETIME                                  NOT NULL,
    -- RNE Estado de TRANSFERENCIA: dominio fijo {Pendiente, Aceptada, Rechazada}
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
    -- RNE 2 (máx. 3 transferencias por entrada): trigger tr_transferencia_limite
    -- RNE 6 (solo entradas Activas): trigger tr_transferencia_estado_entrada
    INDEX idx_transf_entrada (EntradaID),
    INDEX idx_transf_origen  (Mail_Origen),
    INDEX idx_transf_destino (Mail_Destino)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- -----------------------------------------------------------------------------
-- MÓDULO 4: SEGURIDAD Y VALIDACIÓN
-- Orden: TOKEN_QR → DISPOSITIVO → VALIDACION → ASIGNACION_FUNCIONARIO
-- -----------------------------------------------------------------------------

-- TOKEN_QR: Entrada Dinámica (RNE 10). Cada entrada activa tiene un token vigente
-- que vence a los 30 segundos. Cuando el cliente lo solicita y ya venció, el backend
-- desactiva el anterior y genera uno nuevo (ver TokenService). Las filas anteriores
-- quedan como histórico (cadena de tokens). DEC-02: limpieza de tokens viejos = mejora futura.
CREATE TABLE IF NOT EXISTS TOKEN_QR (
    TokenID     INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    CodigoQR    VARCHAR(500)   NOT NULL,
    GeneradoEn  DATETIME       NOT NULL,
    -- Ventana de validez de 30 segundos: ExpiraEn = GeneradoEn + 30s.
    -- Un token vencido no puede validarse (RNE 10).
    ExpiraEn    DATETIME       NOT NULL,
    -- RNE Activo: booleano; solo un token activo por entrada a la vez
    Activo      BOOLEAN        NOT NULL DEFAULT FALSE,
    EntradaID   INT UNSIGNED   NOT NULL,

    CONSTRAINT pk_token_qr       PRIMARY KEY (TokenID),
    CONSTRAINT uq_token_codigo   UNIQUE (CodigoQR),
    CONSTRAINT chk_token_ventana CHECK (ExpiraEn > GeneradoEn),
    CONSTRAINT fk_token_entrada  FOREIGN KEY (EntradaID)
        REFERENCES ENTRADA(EntradaID)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    -- RNE Activo (un solo token activo por entrada): no expresable como UNIQUE parcial en MySQL,
    -- se implementa con trigger tr_token_unico_activo (ver triggers.sql)
    INDEX idx_token_entrada_activo (EntradaID, Activo)  -- crítico para RNE 9
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- DISPOSITIVO: FK NOT NULL a FUNCIONARIO implementa RNE 11 (TieneAsig, Ajuste 2 MER)
CREATE TABLE IF NOT EXISTS DISPOSITIVO (
    DispositivoID    INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    -- RNE 11: todo dispositivo debe estar vinculado a un funcionario antes de usarse
    Mail_Funcionario VARCHAR(254)   NOT NULL,

    CONSTRAINT pk_dispositivo     PRIMARY KEY (DispositivoID),
    CONSTRAINT fk_disp_funcionario FOREIGN KEY (Mail_Funcionario)
        REFERENCES FUNCIONARIO(Mail_Usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_disp_funcionario (Mail_Funcionario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- VALIDACION: relación ternaria Valida (Ajuste 1 MER)
-- PK = TokenID porque TOKEN_QR participa (0,1) en la ternaria → un token, a lo sumo una validación
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
    -- RNE 7 (validación irreversible): trigger tr_validacion_irreversible
    -- RNE 8 (propietario al validar): trigger tr_validacion_propietario
    -- RNE 9 (token activo al validar): trigger tr_validacion_token_activo
    INDEX idx_val_funcionario  (Mail_Funcionario),
    INDEX idx_val_dispositivo  (DispositivoID),
    INDEX idx_val_fechahora    (FechaHora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ASIGNACION_FUNCIONARIO: tabla intermedia de la relación N:N AsignadoA
-- (FUNCIONARIO × EVENTO_SECTOR)
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
-- VISTA: MontoTotal por venta
-- Calcula el monto total de una venta dinámicamente (ver Parte III §3.4 del informe).
-- Fórmula: SUM(Costo_Historico de las entradas) × (1 + Porcentaje/100)
-- =============================================================================

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
