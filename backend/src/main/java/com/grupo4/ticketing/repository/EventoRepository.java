package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.Evento;
import com.grupo4.ticketing.repository.projection.EventoVendidoView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {
    List<Evento> findAllByOrderByFechaHoraAsc();

    // Reporte: eventos ordenados por cantidad de entradas vendidas (descendente).
    @Query(value = """
            SELECT ev.EventoID        AS eventoId,
                   ev.EquipoLocal     AS equipoLocal,
                   ev.EquipoVisitante AS equipoVisitante,
                   ev.FechaHora       AS fechaHora,
                   COUNT(e.EntradaID) AS vendidas
            FROM EVENTO ev
            LEFT JOIN ENTRADA e ON e.EventoID = ev.EventoID
            GROUP BY ev.EventoID, ev.EquipoLocal, ev.EquipoVisitante, ev.FechaHora
            ORDER BY vendidas DESC, ev.FechaHora ASC
            """, nativeQuery = true)
    List<EventoVendidoView> rankingEventosPorVentas();
}
