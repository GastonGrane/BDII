package com.grupo4.ticketing.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "EVENTO_SECTOR")
public class EventoSector {

    @EmbeddedId
    private EventoSectorId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("eventoId")
    @JoinColumn(name = "EventoID")
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "EstadioID",  referencedColumnName = "EstadioID",  insertable = false, updatable = false),
        @JoinColumn(name = "LetraSector", referencedColumnName = "LetraSector", insertable = false, updatable = false)
    })
    private Sector sector;

    public EventoSector() {}

    public EventoSectorId getId() { return id; }
    public void setId(EventoSectorId id) { this.id = id; }
    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }
    public Sector getSector() { return sector; }
    public void setSector(Sector sector) { this.sector = sector; }
}
