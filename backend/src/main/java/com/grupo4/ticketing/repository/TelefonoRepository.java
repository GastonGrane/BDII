package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.Telefono;
import com.grupo4.ticketing.entity.TelefonoId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TelefonoRepository extends JpaRepository<Telefono, TelefonoId> {
    List<Telefono> findByIdMailUsuario(String mailUsuario);
}
