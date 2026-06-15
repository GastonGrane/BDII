package com.grupo4.ticketing.entity;

import com.grupo4.ticketing.entity.enums.EstadoTransferencia;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TRANSFERENCIA")
public class Transferencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransfID")
    private Long transfId;

    @Column(name = "FechaSol", nullable = false)
    private LocalDateTime fechaSol;

    @Enumerated(EnumType.STRING)
    @Column(name = "Estado", nullable = false)
    private EstadoTransferencia estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EntradaID", nullable = false)
    private Entrada entrada;

    // FK a USUARIO (no a USUARIO_GENERAL), igual que en el schema
    @Column(name = "Mail_Origen", nullable = false, length = 254)
    private String mailOrigen;

    @Column(name = "Mail_Destino", nullable = false, length = 254)
    private String mailDestino;

    public Transferencia() {}

    public Long getTransfId() { return transfId; }
    public void setTransfId(Long transfId) { this.transfId = transfId; }
    public LocalDateTime getFechaSol() { return fechaSol; }
    public void setFechaSol(LocalDateTime fechaSol) { this.fechaSol = fechaSol; }
    public EstadoTransferencia getEstado() { return estado; }
    public void setEstado(EstadoTransferencia estado) { this.estado = estado; }
    public Entrada getEntrada() { return entrada; }
    public void setEntrada(Entrada entrada) { this.entrada = entrada; }
    public String getMailOrigen() { return mailOrigen; }
    public void setMailOrigen(String mailOrigen) { this.mailOrigen = mailOrigen; }
    public String getMailDestino() { return mailDestino; }
    public void setMailDestino(String mailDestino) { this.mailDestino = mailDestino; }
}
