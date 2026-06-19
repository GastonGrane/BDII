package com.grupo4.ticketing.dto;

import java.util.List;

// Estado de cumplimiento de RNE 5 para un evento completo: 'cumple' es true cuando no queda
// ningún sector asignado sin validar. 'pendientes' enumera los incumplimientos restantes.
public record CoberturaEstadoResponse(
        Long eventoId,
        boolean cumple,
        List<CoberturaPendienteResponse> pendientes
) {}
