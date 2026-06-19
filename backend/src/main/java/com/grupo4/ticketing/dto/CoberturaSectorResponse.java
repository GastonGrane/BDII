package com.grupo4.ticketing.dto;

import java.time.LocalDateTime;

// Checklist de cobertura del funcionario (RNE 5): un sector asignado en un evento y si ya
// fue cubierto (validó al menos una entrada) o sigue pendiente.
public record CoberturaSectorResponse(
        Long eventoId,
        String equipoLocal,
        String equipoVisitante,
        LocalDateTime fechaHora,
        Long estadioId,
        String letraSector,
        boolean cumplido
) {}
