package com.grupo4.ticketing.dto;

import java.util.List;

public record EstadioDetalleResponse(
        Long estadioId,
        String nombre,
        String pais,
        String ciudad,
        List<SectorListItem> sectores
) {}
