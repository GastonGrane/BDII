package com.grupo4.ticketing.repository.projection;

import java.time.LocalDateTime;

// Proyección del estado de cobertura por sector asignado a un funcionario (RNE 5).
// 'cubierto' = el funcionario ya validó al menos una entrada en ese sector del evento.
public interface CoberturaSectorView {
    Long getEventoId();
    String getEquipoLocal();
    String getEquipoVisitante();
    LocalDateTime getFechaHora();
    Long getEstadioId();
    String getLetraSector();
    // La vista devuelve Cubierto como entero 0/1 (MariaDB no tiene boolean nativo),
    // por eso se proyecta como Integer y se interpreta en el servicio.
    Integer getCubierto();
}
