package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.AsignacionFuncionario;
import com.grupo4.ticketing.entity.AsignacionFuncionarioId;
import com.grupo4.ticketing.repository.projection.CoberturaView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AsignacionFuncionarioRepository extends JpaRepository<AsignacionFuncionario, AsignacionFuncionarioId> {
    List<AsignacionFuncionario> findByIdMailFuncionario(String mailFuncionario);

    // RNE 5: sectores asignados a algún funcionario en un evento donde aún no validó
    // ninguna entrada. Usa la vista v_cobertura_funcionario (ver triggers.sql).
    @Query(value = """
            SELECT Mail_Funcionario AS mailFuncionario,
                   EstadioID        AS estadioId,
                   LetraSector      AS letraSector
            FROM v_cobertura_funcionario
            WHERE EventoID = :eventoId AND Cubierto = FALSE
            ORDER BY Mail_Funcionario, LetraSector
            """, nativeQuery = true)
    List<CoberturaView> coberturaPendiente(@Param("eventoId") Long eventoId);
}
