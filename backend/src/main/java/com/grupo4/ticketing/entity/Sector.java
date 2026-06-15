package com.grupo4.ticketing.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "SECTOR")
public class Sector {

    @EmbeddedId
    private SectorId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("estadioId")
    @JoinColumn(name = "EstadioID")
    private Estadio estadio;

    @Column(name = "CapacidadMax", nullable = false)
    private Integer capacidadMax;

    @Column(name = "CostoEntrada", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoEntrada;

    public Sector() {}

    public SectorId getId() { return id; }
    public void setId(SectorId id) { this.id = id; }
    public Estadio getEstadio() { return estadio; }
    public void setEstadio(Estadio estadio) { this.estadio = estadio; }
    public Integer getCapacidadMax() { return capacidadMax; }
    public void setCapacidadMax(Integer capacidadMax) { this.capacidadMax = capacidadMax; }
    public BigDecimal getCostoEntrada() { return costoEntrada; }
    public void setCostoEntrada(BigDecimal costoEntrada) { this.costoEntrada = costoEntrada; }
}
