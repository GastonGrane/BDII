package com.grupo4.ticketing.dto;

import java.time.LocalDateTime;

public record EventoResponse(
        Long eventoId,
        String equipoLocal,
        String equipoVisitante,
        LocalDateTime fechaHora,
        Long estadioId
) {}
