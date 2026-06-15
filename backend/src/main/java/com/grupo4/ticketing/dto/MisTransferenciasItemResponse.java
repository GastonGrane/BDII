package com.grupo4.ticketing.dto;

import java.time.LocalDateTime;

public record MisTransferenciasItemResponse(
        Long transfId,
        Long entradaId,
        String estado,
        LocalDateTime fechaSol,
        String rol,          // "ORIGEN" o "DESTINO"
        String otroUsuario   // el otro extremo de la transferencia
) {}
