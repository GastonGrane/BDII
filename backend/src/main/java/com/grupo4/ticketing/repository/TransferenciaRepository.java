package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.Transferencia;
import com.grupo4.ticketing.entity.enums.EstadoTransferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {

    List<Transferencia> findByMailOrigenOrMailDestino(String mailOrigen, String mailDestino);

    // Cuenta transfers no rechazadas de una entrada — para pre-validar RNE 2
    long countByEntradaEntradaIdAndEstadoNot(Long entradaId, EstadoTransferencia estado);

    // Historial ordenado de transferencias de una entrada — base de la cadena de custodia.
    List<Transferencia> findByEntradaEntradaIdOrderByFechaSolAsc(Long entradaId);
}
