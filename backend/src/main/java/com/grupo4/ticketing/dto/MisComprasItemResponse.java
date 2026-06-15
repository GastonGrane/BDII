package com.grupo4.ticketing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MisComprasItemResponse(
        Long ventaId,
        LocalDateTime fecha,
        String estado,
        BigDecimal montoTotal,
        long cantidadEntradas
) {}
