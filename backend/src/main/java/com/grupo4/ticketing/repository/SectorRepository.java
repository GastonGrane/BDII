package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.Sector;
import com.grupo4.ticketing.entity.SectorId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SectorRepository extends JpaRepository<Sector, SectorId> {
    List<Sector> findByIdEstadioId(Long estadioId);
}
