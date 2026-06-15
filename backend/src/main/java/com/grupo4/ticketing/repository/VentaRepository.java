package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByCompradorMailUsuario(String mailComprador);

    // Calcula MontoTotal sin depender de la vista (DEC-03, DEC-08)
    @Query(value = """
            SELECT SUM(e.Costo_Historico) * (1 + c.Porcentaje / 100)
            FROM VENTA v
            JOIN COMISION c ON v.ComisionID = c.ComisionID
            JOIN ENTRADA  e ON e.VentaID    = v.VentaID
            WHERE v.VentaID = :ventaId
            """, nativeQuery = true)
    BigDecimal calcularMontoTotal(@Param("ventaId") Long ventaId);
}
