package com.grupo4.ticketing.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EventoRequest(
        String equipoLocal,
        String equipoVisitante,
        LocalDateTime fechaHora,
        Long estadioId,
        List<String> sectores
) {}
