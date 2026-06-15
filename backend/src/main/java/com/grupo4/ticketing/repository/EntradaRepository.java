package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.Entrada;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {
    List<Entrada> findByPropietarioMailUsuario(String mailPropietario);
    List<Entrada> findByVentaVentaId(Long ventaId);
    long countByVentaVentaId(Long ventaId);
}
