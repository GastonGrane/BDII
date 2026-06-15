package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {
    List<Evento> findAllByOrderByFechaHoraAsc();
}
