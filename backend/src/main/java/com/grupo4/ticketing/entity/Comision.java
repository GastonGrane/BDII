package com.grupo4.ticketing.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "COMISION")
public class Comision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ComisionID")
    private Long comisionId;

    @Column(name = "Porcentaje", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @Column(name = "F_Desde", nullable = false)
    private LocalDateTime fDesde;

    @Column(name = "F_Hasta")
    private LocalDateTime fHasta;

    public Comision() {}

    public Long getComisionId() { return comisionId; }
    public void setComisionId(Long comisionId) { this.comisionId = comisionId; }
    public BigDecimal getPorcentaje() { return porcentaje; }
    public void setPorcentaje(BigDecimal porcentaje) { this.porcentaje = porcentaje; }
    public LocalDateTime getFDesde() { return fDesde; }
    public void setFDesde(LocalDateTime fDesde) { this.fDesde = fDesde; }
    public LocalDateTime getFHasta() { return fHasta; }
    public void setFHasta(LocalDateTime fHasta) { this.fHasta = fHasta; }
}
