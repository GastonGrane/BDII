package com.grupo4.ticketing.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "ADMINISTRADOR")
public class Administrador {

    @Id
    @Column(name = "Mail_Usuario", length = 254)
    private String mailUsuario;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Mail_Usuario", insertable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "PaisSede", nullable = false, length = 100)
    private String paisSede;

    @Column(name = "FechaAsignacion", nullable = false)
    private LocalDate fechaAsignacion;

    public Administrador() {}

    public String getMailUsuario() { return mailUsuario; }
    public void setMailUsuario(String mailUsuario) { this.mailUsuario = mailUsuario; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getPaisSede() { return paisSede; }
    public void setPaisSede(String paisSede) { this.paisSede = paisSede; }
    public LocalDate getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(LocalDate fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }
}
