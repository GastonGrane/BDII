package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.Dispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {
    List<Dispositivo> findByFuncionarioMailUsuario(String mailFuncionario);
}
