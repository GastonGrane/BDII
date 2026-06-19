package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.AsignacionFuncionario;
import com.grupo4.ticketing.entity.AsignacionFuncionarioId;
import com.grupo4.ticketing.repository.projection.CoberturaSectorView;
import com.grupo4.ticketing.repository.projection.CoberturaView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AsignacionFuncionarioRepository extends JpaRepository<AsignacionFuncionario, AsignacionFuncionarioId> {

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

    // RNE 5: checklist completo de un funcionario — todos sus sectores asignados con la marca
    // de cubierto/pendiente, junto a los datos del evento para mostrarlos agrupados.
    @Query(value = """
            SELECT vc.EventoID        AS eventoId,
                   ev.EquipoLocal     AS equipoLocal,
                   ev.EquipoVisitante AS equipoVisitante,
                   ev.FechaHora       AS fechaHora,
                   vc.EstadioID       AS estadioId,
                   vc.LetraSector     AS letraSector,
                   vc.Cubierto        AS cubierto
            FROM v_cobertura_funcionario vc
            JOIN EVENTO ev ON ev.EventoID = vc.EventoID
            WHERE vc.Mail_Funcionario = :mailFuncionario
            ORDER BY ev.FechaHora, vc.LetraSector
            """, nativeQuery = true)
    List<CoberturaSectorView> coberturaPorFuncionario(@Param("mailFuncionario") String mailFuncionario);
}
