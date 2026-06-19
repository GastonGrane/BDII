package com.grupo4.ticketing.dto;

import java.util.List;

// Alta de estadio. RNE 3: un estadio debe poder tener sectores cargados y el alta inicial
// exige al menos uno (ver AdminService.crearEstadio). 'sectores' puede traer 1..4 sectores (A–D).
public record EstadioRequest(
        String nombre,
        String pais,
        String ciudad,
        List<SectorRequest> sectores
) {}
