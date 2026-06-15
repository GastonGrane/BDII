package com.grupo4.ticketing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import com.grupo4.ticketing.entity.enums.LetraSector;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SectorId implements Serializable {

    @Column(name = "EstadioID")
    private Long estadioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "LetraSector")
    private LetraSector letraSector;

    public SectorId() {}

    public SectorId(Long estadioId, LetraSector letraSector) {
        this.estadioId = estadioId;
        this.letraSector = letraSector;
    }

    public Long getEstadioId() { return estadioId; }
    public void setEstadioId(Long estadioId) { this.estadioId = estadioId; }
    public LetraSector getLetraSector() { return letraSector; }
    public void setLetraSector(LetraSector letraSector) { this.letraSector = letraSector; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SectorId s)) return false;
        return Objects.equals(estadioId, s.estadioId) && letraSector == s.letraSector;
    }

    @Override
    public int hashCode() { return Objects.hash(estadioId, letraSector); }
}
