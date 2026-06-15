package com.grupo4.ticketing.dto;

public record ValidacionResponse(
        Long entradaId,
        Long eventoId,
        String letraSector,
        String mailPropietario
) {}
