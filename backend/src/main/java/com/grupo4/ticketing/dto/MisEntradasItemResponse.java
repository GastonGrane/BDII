package com.grupo4.ticketing.dto;

import java.math.BigDecimal;
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
        String codigoQR          // null si el estado no es Activa
) {}
