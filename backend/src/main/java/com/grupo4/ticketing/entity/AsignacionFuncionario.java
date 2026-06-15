package com.grupo4.ticketing.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ASIGNACION_FUNCIONARIO")
public class AsignacionFuncionario {

    @EmbeddedId
    private AsignacionFuncionarioId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("mailFuncionario")
    @JoinColumn(name = "Mail_Funcionario")
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "EventoID",    referencedColumnName = "EventoID",    insertable = false, updatable = false),
        @JoinColumn(name = "EstadioID",   referencedColumnName = "EstadioID",   insertable = false, updatable = false),
        @JoinColumn(name = "LetraSector", referencedColumnName = "LetraSector", insertable = false, updatable = false)
    })
    private EventoSector eventoSector;

    public AsignacionFuncionario() {}

    public AsignacionFuncionarioId getId() { return id; }
    public void setId(AsignacionFuncionarioId id) { this.id = id; }
    public Funcionario getFuncionario() { return funcionario; }
    public void setFuncionario(Funcionario funcionario) { this.funcionario = funcionario; }
    public EventoSector getEventoSector() { return eventoSector; }
    public void setEventoSector(EventoSector eventoSector) { this.eventoSector = eventoSector; }
}
