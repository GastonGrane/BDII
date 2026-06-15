package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.Comision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface ComisionRepository extends JpaRepository<Comision, Long> {

    @Query("SELECT c FROM Comision c WHERE c.fHasta IS NULL")
    Optional<Comision> findVigente();
}
