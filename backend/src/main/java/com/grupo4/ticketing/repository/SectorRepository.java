package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.Sector;
import com.grupo4.ticketing.entity.SectorId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SectorRepository extends JpaRepository<Sector, SectorId> {
    List<Sector> findByIdEstadioId(Long estadioId);

    // Lock pesimista sobre la fila del sector: serializa compras concurrentes del mismo
    // sector para que el control de aforo (RNE 3) no sufra condición de carrera.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sector s WHERE s.id = :id")
    Optional<Sector> findByIdForUpdate(@Param("id") SectorId id);
}
