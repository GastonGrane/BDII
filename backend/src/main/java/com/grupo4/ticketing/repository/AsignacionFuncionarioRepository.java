package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.AsignacionFuncionario;
import com.grupo4.ticketing.entity.AsignacionFuncionarioId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AsignacionFuncionarioRepository extends JpaRepository<AsignacionFuncionario, AsignacionFuncionarioId> {
    List<AsignacionFuncionario> findByIdMailFuncionario(String mailFuncionario);
}
