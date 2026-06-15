package com.grupo4.ticketing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TelefonoId implements Serializable {

    @Column(name = "Mail_Usuario")
    private String mailUsuario;

    @Column(name = "Telefono")
    private String telefono;

    public TelefonoId() {}

    public TelefonoId(String mailUsuario, String telefono) {
        this.mailUsuario = mailUsuario;
        this.telefono = telefono;
    }

    public String getMailUsuario() { return mailUsuario; }
    public void setMailUsuario(String mailUsuario) { this.mailUsuario = mailUsuario; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TelefonoId t)) return false;
        return Objects.equals(mailUsuario, t.mailUsuario) && Objects.equals(telefono, t.telefono);
    }

    @Override
    public int hashCode() { return Objects.hash(mailUsuario, telefono); }
}
