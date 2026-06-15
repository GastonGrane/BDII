package com.grupo4.ticketing.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "VALIDACION")
public class Validacion {

    // PK = TokenID (misma columna que FK a TOKEN_QR, sin AUTO_INCREMENT)
    @Id
    @Column(name = "TokenID")
    private Long tokenId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "TokenID")
    private TokenQr tokenQr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Mail_Funcionario", nullable = false)
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DispositivoID", nullable = false)
    private Dispositivo dispositivo;

    @Column(name = "FechaHora", nullable = false)
    private LocalDateTime fechaHora;

    public Validacion() {}

    public Long getTokenId() { return tokenId; }
    public void setTokenId(Long tokenId) { this.tokenId = tokenId; }
    public TokenQr getTokenQr() { return tokenQr; }
    public void setTokenQr(TokenQr tokenQr) { this.tokenQr = tokenQr; }
    public Funcionario getFuncionario() { return funcionario; }
    public void setFuncionario(Funcionario funcionario) { this.funcionario = funcionario; }
    public Dispositivo getDispositivo() { return dispositivo; }
    public void setDispositivo(Dispositivo dispositivo) { this.dispositivo = dispositivo; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
}
