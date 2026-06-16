package com.grupo4.ticketing.dto;

import java.time.LocalDateTime;

// Fila del reporte "eventos con más entradas vendidas".
public record EventoVendidoResponse(
        Long eventoId,
        String equipoLocal,
        String equipoVisitante,
        LocalDateTime fechaHora,
        long entradasVendidas
) {}
