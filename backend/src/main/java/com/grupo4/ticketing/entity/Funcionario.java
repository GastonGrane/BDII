package com.grupo4.ticketing.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "FUNCIONARIO")
public class Funcionario {

    @Id
    @Column(name = "Mail_Usuario", length = 254)
    private String mailUsuario;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Mail_Usuario", insertable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "NroLegajo", nullable = false, unique = true, length = 20)
    private String nroLegajo;

    public Funcionario() {}

    public String getMailUsuario() { return mailUsuario; }
    public void setMailUsuario(String mailUsuario) { this.mailUsuario = mailUsuario; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getNroLegajo() { return nroLegajo; }
    public void setNroLegajo(String nroLegajo) { this.nroLegajo = nroLegajo; }
}
