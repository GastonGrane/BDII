package com.grupo4.ticketing.entity;

import com.grupo4.ticketing.entity.enums.EstadoVerificacion;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "USUARIO_GENERAL")
public class UsuarioGeneral {

    @Id
    @Column(name = "Mail_Usuario", length = 254)
    private String mailUsuario;

    // insertable/updatable=false: la FK se establece vía mailUsuario (la PK)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Mail_Usuario", insertable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "FechaRegistro", nullable = false)
    private LocalDate fechaRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "EstadoVerificacion", nullable = false)
    private EstadoVerificacion estadoVerificacion = EstadoVerificacion.Pendiente;

    public UsuarioGeneral() {}

    public String getMailUsuario() { return mailUsuario; }
    public void setMailUsuario(String mailUsuario) { this.mailUsuario = mailUsuario; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public EstadoVerificacion getEstadoVerificacion() { return estadoVerificacion; }
    public void setEstadoVerificacion(EstadoVerificacion v) { this.estadoVerificacion = v; }
}
