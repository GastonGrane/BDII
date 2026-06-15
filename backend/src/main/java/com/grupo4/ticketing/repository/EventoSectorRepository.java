package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.EventoSector;
import com.grupo4.ticketing.entity.EventoSectorId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventoSectorRepository extends JpaRepository<EventoSector, EventoSectorId> {
    List<EventoSector> findByIdEventoId(Long eventoId);
}
