package com.grupo4.ticketing.dto;

public record CompraItemRequest(
    Long eventoId,
    Long estadioId,
    String letraSector,
    Integer cantidad
) {}
