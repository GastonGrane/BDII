package com.grupo4.ticketing.repository.projection;

import java.time.LocalDateTime;

// Proyección para el reporte de eventos con más entradas vendidas.
public interface EventoVendidoView {
    Long getEventoId();
    String getEquipoLocal();
    String getEquipoVisitante();
    LocalDateTime getFechaHora();
    long getVendidas();
}
