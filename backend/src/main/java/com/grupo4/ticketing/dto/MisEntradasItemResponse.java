package com.grupo4.ticketing.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record MisEntradasItemResponse(
        Long entradaId,
        Long eventoId,
        String equipoLocal,
        String equipoVisitante,
        LocalDateTime fechaHoraEvento,
        String letraSector,
        String estadoEntrada,
        BigDecimal costoHistorico,
        String codigoQR,        // token dinámico vigente; null si el estado no es Activa
        Instant tokenExpiraEn   // fin de la ventana de 30s del token, en UTC (se serializa con 'Z')
) {}
