package com.grupo4.ticketing.dto;

// Sector asignado a un funcionario en el que todavía no validó ninguna entrada (RNE 5).
public record CoberturaPendienteResponse(
        String mailFuncionario,
        Long estadioId,
        String letraSector
) {}
