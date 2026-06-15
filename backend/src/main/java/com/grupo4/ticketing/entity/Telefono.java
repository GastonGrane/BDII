package com.grupo4.ticketing.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "TELEFONO")
public class Telefono {

    @EmbeddedId
    private TelefonoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("mailUsuario")
    @JoinColumn(name = "Mail_Usuario")
    private Usuario usuario;

    public Telefono() {}

    public Telefono(Usuario usuario, String telefono) {
        this.usuario = usuario;
        this.id = new TelefonoId(usuario.getMail(), telefono);
    }

    public TelefonoId getId() { return id; }
    public void setId(TelefonoId id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getTelefono() { return id != null ? id.getTelefono() : null; }
}
