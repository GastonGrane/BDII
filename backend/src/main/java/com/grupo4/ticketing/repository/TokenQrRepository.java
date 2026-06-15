package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.TokenQr;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TokenQrRepository extends JpaRepository<TokenQr, Long> {
    Optional<TokenQr> findByCodigoQRAndActivoTrue(String codigoQR);
    Optional<TokenQr> findByEntradaEntradaIdAndActivoTrue(Long entradaId);
}
