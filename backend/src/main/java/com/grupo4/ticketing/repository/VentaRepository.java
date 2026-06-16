package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.Venta;
import com.grupo4.ticketing.repository.projection.CompradorRankingView;
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

    // Reporte: ranking de mayores compradores por cantidad de entradas.
    // montoGastado = suma de costos históricos de sus entradas (sin comisión, ver doc).
    @Query(value = """
            SELECT v.Mail_Comprador            AS mail,
                   COUNT(e.EntradaID)          AS cantidad,
                   COALESCE(SUM(e.Costo_Historico), 0) AS montoGastado
            FROM VENTA v
            JOIN ENTRADA e ON e.VentaID = v.VentaID
            GROUP BY v.Mail_Comprador
            ORDER BY cantidad DESC, montoGastado DESC
            """, nativeQuery = true)
    List<CompradorRankingView> rankingCompradores();
}
