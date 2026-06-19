package com.grupo4.ticketing.dto;

import java.time.LocalDateTime;

// Token dinámico vigente de una entrada (Entrada Dinámica, RNE 10).
public record TokenVigenteResponse(
        Long entradaId,
        String codigoQR,
        LocalDateTime generadoEn,
        LocalDateTime expiraEn,
        int ventanaSegundos
) {}
