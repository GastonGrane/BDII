package com.grupo4.ticketing.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TOKEN_QR")
public class TokenQr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TokenID")
    private Long tokenId;

    @Column(name = "CodigoQR", nullable = false, unique = true, length = 500)
    private String codigoQR;

    @Column(name = "GeneradoEn", nullable = false)
    private LocalDateTime generadoEn;

    // Entrada Dinámica (RNE 10): el token vence a los 30 segundos de generado.
    @Column(name = "ExpiraEn", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "Activo", nullable = false)
    private Boolean activo = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EntradaID", nullable = false)
    private Entrada entrada;

    public TokenQr() {}

    public Long getTokenId() { return tokenId; }
    public void setTokenId(Long tokenId) { this.tokenId = tokenId; }
    public String getCodigoQR() { return codigoQR; }
    public void setCodigoQR(String codigoQR) { this.codigoQR = codigoQR; }
    public LocalDateTime getGeneradoEn() { return generadoEn; }
    public void setGeneradoEn(LocalDateTime generadoEn) { this.generadoEn = generadoEn; }
    public LocalDateTime getExpiraEn() { return expiraEn; }
    public void setExpiraEn(LocalDateTime expiraEn) { this.expiraEn = expiraEn; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public Entrada getEntrada() { return entrada; }
    public void setEntrada(Entrada entrada) { this.entrada = entrada; }
}
