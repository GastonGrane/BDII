package com.grupo4.ticketing.dto;

import java.time.Instant;

// Token dinámico vigente de una entrada (Entrada Dinámica, RNE 10).
// generadoEn/expiraEn van en UTC (Instant → se serializan con 'Z'), así el navegador
// calcula bien la ventana de 30s sin importar su zona horaria.
public record TokenVigenteResponse(
        Long entradaId,
        String codigoQR,
        Instant generadoEn,
        Instant expiraEn,
        int ventanaSegundos
) {}
