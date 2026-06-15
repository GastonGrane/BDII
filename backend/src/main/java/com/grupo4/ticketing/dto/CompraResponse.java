package com.grupo4.ticketing.dto;

import java.math.BigDecimal;
import java.util.List;

public record CompraResponse(
    Long ventaId,
    List<Long> entradaIds,
    BigDecimal montoTotal
) {}
