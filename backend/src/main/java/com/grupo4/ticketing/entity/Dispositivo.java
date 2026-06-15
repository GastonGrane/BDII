package com.grupo4.ticketing.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "DISPOSITIVO")
public class Dispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DispositivoID")
    private Long dispositivoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Mail_Funcionario", nullable = false)
    private Funcionario funcionario;

    public Dispositivo() {}

    public Long getDispositivoId() { return dispositivoId; }
    public void setDispositivoId(Long dispositivoId) { this.dispositivoId = dispositivoId; }
    public Funcionario getFuncionario() { return funcionario; }
    public void setFuncionario(Funcionario funcionario) { this.funcionario = funcionario; }
}
