package com.grupo4.ticketing.entity;

import com.grupo4.ticketing.entity.enums.EstadoEntrada;
import com.grupo4.ticketing.entity.enums.LetraSector;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ENTRADA")
public class Entrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EntradaID")
    private Long entradaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "EstadoEntrada", nullable = false)
    private EstadoEntrada estadoEntrada;

    @Column(name = "Costo_Historico", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoHistorico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VentaID", nullable = false)
    private Venta venta;

    // FK compuesta → EVENTO_SECTOR (EventoID, EstadioID, LetraSector)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "EventoID",    referencedColumnName = "EventoID"),
        @JoinColumn(name = "EstadioID",   referencedColumnName = "EstadioID"),
        @JoinColumn(name = "LetraSector", referencedColumnName = "LetraSector")
    })
    private EventoSector eventoSector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Mail_Propietario", nullable = false)
    private UsuarioGeneral propietario;

    public Entrada() {}

    public Long getEntradaId() { return entradaId; }
    public void setEntradaId(Long entradaId) { this.entradaId = entradaId; }
    public EstadoEntrada getEstadoEntrada() { return estadoEntrada; }
    public void setEstadoEntrada(EstadoEntrada estadoEntrada) { this.estadoEntrada = estadoEntrada; }
    public BigDecimal getCostoHistorico() { return costoHistorico; }
    public void setCostoHistorico(BigDecimal costoHistorico) { this.costoHistorico = costoHistorico; }
    public Venta getVenta() { return venta; }
    public void setVenta(Venta venta) { this.venta = venta; }
    public EventoSector getEventoSector() { return eventoSector; }
    public void setEventoSector(EventoSector eventoSector) { this.eventoSector = eventoSector; }
    public UsuarioGeneral getPropietario() { return propietario; }
    public void setPropietario(UsuarioGeneral propietario) { this.propietario = propietario; }

    public Long getEventoId()    { return eventoSector != null ? eventoSector.getId().getEventoId()  : null; }
    public Long getEstadioId()   { return eventoSector != null ? eventoSector.getId().getEstadioId() : null; }
    public LetraSector getLetraSector() { return eventoSector != null ? eventoSector.getId().getLetraSector() : null; }
}
