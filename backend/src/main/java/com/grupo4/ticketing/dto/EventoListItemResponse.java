package com.grupo4.ticketing.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EventoListItemResponse(
        Long eventoId,
        String equipoLocal,
        String equipoVisitante,
        LocalDateTime fechaHora,
        EstadioResponse estadio,
        List<SectorListItem> sectores
) {}
