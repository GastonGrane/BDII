package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.Entrada;
import com.grupo4.ticketing.entity.enums.LetraSector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {
    List<Entrada> findByPropietarioMailUsuario(String mailPropietario);
    List<Entrada> findByVentaVentaId(Long ventaId);
    long countByVentaVentaId(Long ventaId);

    // Entradas ya emitidas para un (evento, sector): base del control de aforo (RNE 3).
    @Query("""
            SELECT COUNT(e) FROM Entrada e
            WHERE e.eventoSector.id.eventoId = :eventoId
              AND e.eventoSector.id.estadioId = :estadioId
              AND e.eventoSector.id.letraSector = :letra
            """)
    long contarPorEventoSector(@Param("eventoId") Long eventoId,
                               @Param("estadioId") Long estadioId,
                               @Param("letra") LetraSector letra);
}
