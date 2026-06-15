package com.grupo4.ticketing.entity;

import com.grupo4.ticketing.entity.enums.EstadoVenta;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "VENTA")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VentaID")
    private Long ventaId;

    @Column(name = "Fecha", nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "Estado", nullable = false)
    private EstadoVenta estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Mail_Comprador", nullable = false)
    private UsuarioGeneral comprador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComisionID", nullable = false)
    private Comision comision;

    public Venta() {}

    public Long getVentaId() { return ventaId; }
    public void setVentaId(Long ventaId) { this.ventaId = ventaId; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public EstadoVenta getEstado() { return estado; }
    public void setEstado(EstadoVenta estado) { this.estado = estado; }
    public UsuarioGeneral getComprador() { return comprador; }
    public void setComprador(UsuarioGeneral comprador) { this.comprador = comprador; }
    public Comision getComision() { return comision; }
    public void setComision(Comision comision) { this.comision = comision; }
}
