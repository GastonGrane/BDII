package com.grupo4.ticketing.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ESTADIO")
public class Estadio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EstadioID")
    private Long estadioId;

    @Column(name = "Nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "Pais", nullable = false, length = 50)
    private String pais;

    @Column(name = "Ciudad", nullable = false, length = 100)
    private String ciudad;

    public Estadio() {}

    public Long getEstadioId() { return estadioId; }
    public void setEstadioId(Long estadioId) { this.estadioId = estadioId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
}
