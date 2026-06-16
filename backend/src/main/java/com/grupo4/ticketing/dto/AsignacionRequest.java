package com.grupo4.ticketing.dto;

// Asignación de un funcionario a un sector habilitado de un evento (RNE 5).
public record AsignacionRequest(
        String mailFuncionario,
        Long eventoId,
        Long estadioId,
        String letraSector
) {}
