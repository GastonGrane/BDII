package com.grupo4.ticketing.dto;

import java.math.BigDecimal;

// Fila del reporte "ranking de mayores compradores".
public record CompradorRankingResponse(
        String mail,
        long cantidadEntradas,
        BigDecimal montoGastado
) {}
