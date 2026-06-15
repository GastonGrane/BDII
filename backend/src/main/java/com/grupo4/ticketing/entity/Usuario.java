package com.grupo4.ticketing.entity;

import com.grupo4.ticketing.entity.enums.TipoDoc;
import jakarta.persistence.*;

@Entity
@Table(name = "USUARIO")
public class Usuario {

    @Id
    @Column(name = "Mail", length = 254)
    private String mail;

    @Column(name = "Contrasena", nullable = false)
    private String contrasena;

    @Enumerated(EnumType.STRING)
    @Column(name = "TipoDoc", nullable = false)
    private TipoDoc tipoDoc;

    @Column(name = "PaisDoc", nullable = false, length = 50)
    private String paisDoc;

    @Column(name = "NroDoc", nullable = false, length = 20)
    private String nroDoc;

    @Column(name = "PaisDir", nullable = false, length = 50)
    private String paisDir;

    @Column(name = "Localidad", nullable = false, length = 100)
    private String localidad;

    @Column(name = "Calle", nullable = false, length = 150)
    private String calle;

    @Column(name = "NroPuerta", nullable = false, length = 20)
    private String nroPuerta;

    @Column(name = "CodPostal", length = 10)
    private String codPostal;

    public Usuario() {}

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    public TipoDoc getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(TipoDoc tipoDoc) { this.tipoDoc = tipoDoc; }
    public String getPaisDoc() { return paisDoc; }
    public void setPaisDoc(String paisDoc) { this.paisDoc = paisDoc; }
    public String getNroDoc() { return nroDoc; }
    public void setNroDoc(String nroDoc) { this.nroDoc = nroDoc; }
    public String getPaisDir() { return paisDir; }
    public void setPaisDir(String paisDir) { this.paisDir = paisDir; }
    public String getLocalidad() { return localidad; }
    public void setLocalidad(String localidad) { this.localidad = localidad; }
    public String getCalle() { return calle; }
    public void setCalle(String calle) { this.calle = calle; }
    public String getNroPuerta() { return nroPuerta; }
    public void setNroPuerta(String nroPuerta) { this.nroPuerta = nroPuerta; }
    public String getCodPostal() { return codPostal; }
    public void setCodPostal(String codPostal) { this.codPostal = codPostal; }
}
