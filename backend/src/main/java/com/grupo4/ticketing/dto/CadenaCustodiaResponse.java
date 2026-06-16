package com.grupo4.ticketing.dto;

import java.time.LocalDateTime;
import java.util.List;

// Cadena de custodia de una entrada: comprador original, propietario actual e
// historial de transferencias (emisión → propietarios sucesivos → validación final).
public record CadenaCustodiaResponse(
        Long entradaId,
        String estadoEntrada,
        String compradorOriginal,
        LocalDateTime fechaCompra,
        String propietarioActual,
        boolean validada,
        List<TransferenciaCustodia> transferencias
) {
    public record TransferenciaCustodia(
            Long transfId,
            String origen,
            String destino,
            String estado,
            LocalDateTime fechaSolicitud
    ) {}
}
