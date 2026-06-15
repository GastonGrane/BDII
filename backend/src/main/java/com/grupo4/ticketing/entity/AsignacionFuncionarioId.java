package com.grupo4.ticketing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import com.grupo4.ticketing.entity.enums.LetraSector;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AsignacionFuncionarioId implements Serializable {

    @Column(name = "Mail_Funcionario")
    private String mailFuncionario;

    @Column(name = "EventoID")
    private Long eventoId;

    @Column(name = "EstadioID")
    private Long estadioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "LetraSector")
    private LetraSector letraSector;

    public AsignacionFuncionarioId() {}

    public AsignacionFuncionarioId(String mailFuncionario, Long eventoId, Long estadioId, LetraSector letraSector) {
        this.mailFuncionario = mailFuncionario;
        this.eventoId = eventoId;
        this.estadioId = estadioId;
        this.letraSector = letraSector;
    }

    public String getMailFuncionario() { return mailFuncionario; }
    public void setMailFuncionario(String v) { this.mailFuncionario = v; }
    public Long getEventoId() { return eventoId; }
    public void setEventoId(Long v) { this.eventoId = v; }
    public Long getEstadioId() { return estadioId; }
    public void setEstadioId(Long v) { this.estadioId = v; }
    public LetraSector getLetraSector() { return letraSector; }
    public void setLetraSector(LetraSector v) { this.letraSector = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AsignacionFuncionarioId a)) return false;
        return Objects.equals(mailFuncionario, a.mailFuncionario) && Objects.equals(eventoId, a.eventoId)
                && Objects.equals(estadioId, a.estadioId) && letraSector == a.letraSector;
    }

    @Override
    public int hashCode() { return Objects.hash(mailFuncionario, eventoId, estadioId, letraSector); }
}
