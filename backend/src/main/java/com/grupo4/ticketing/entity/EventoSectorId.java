package com.grupo4.ticketing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import com.grupo4.ticketing.entity.enums.LetraSector;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EventoSectorId implements Serializable {

    @Column(name = "EventoID")
    private Long eventoId;

    @Column(name = "EstadioID")
    private Long estadioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "LetraSector")
    private LetraSector letraSector;

    public EventoSectorId() {}

    public EventoSectorId(Long eventoId, Long estadioId, LetraSector letraSector) {
        this.eventoId = eventoId;
        this.estadioId = estadioId;
        this.letraSector = letraSector;
    }

    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long eventoId) { this.eventoId = eventoId; }
    public Long getEstadioId() { return estadioId; }
    public void setEstadioId(Long estadioId) { this.estadioId = estadioId; }
    public LetraSector getLetraSector() { return letraSector; }
    public void setLetraSector(LetraSector letraSector) { this.letraSector = letraSector; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventoSectorId e)) return false;
        return Objects.equals(eventoId, e.eventoId) && Objects.equals(estadioId, e.estadioId)
                && letraSector == e.letraSector;
    }

    @Override
    public int hashCode() { return Objects.hash(eventoId, estadioId, letraSector); }
}
