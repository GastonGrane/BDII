package com.grupo4.ticketing.service;

import com.grupo4.ticketing.entity.Entrada;
import com.grupo4.ticketing.entity.TokenQr;
import com.grupo4.ticketing.repository.TokenQrRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

// Entrada Dinámica (RNE 10): cada entrada activa tiene un token vigente que dura 30 segundos.
// Cuando el cliente pide el token y el anterior ya venció, se desactiva y se genera uno nuevo.
// Las filas previas quedan como histórico (cadena de tokens), no se borran.
@Service
public class TokenService {

    // Ventana de validez del token, en segundos.
    public static final int VENTANA_SEGUNDOS = 30;

    private final TokenQrRepository tokenRepo;

    public TokenService(TokenQrRepository tokenRepo) {
        this.tokenRepo = tokenRepo;
    }

    // Genera un token nuevo para la entrada, desactivando el anterior si existía.
    // Se usa en la compra (primer token) y en cada regeneración por vencimiento.
    @Transactional
    public TokenQr generarParaEntrada(Entrada entrada) {
        tokenRepo.findByEntradaEntradaIdAndActivoTrue(entrada.getEntradaId())
                .ifPresent(anterior -> {
                    anterior.setActivo(false);
                    tokenRepo.saveAndFlush(anterior);
                });

        LocalDateTime ahora = LocalDateTime.now();
        TokenQr nuevo = new TokenQr();
        nuevo.setCodigoQR(UUID.randomUUID().toString());
        nuevo.setGeneradoEn(ahora);
        nuevo.setExpiraEn(ahora.plusSeconds(VENTANA_SEGUNDOS));
        nuevo.setActivo(true);
        nuevo.setEntrada(entrada);
        return tokenRepo.saveAndFlush(nuevo);
    }

    // Devuelve el token vigente de la entrada; si no hay o el actual venció, genera uno nuevo.
    @Transactional
    public TokenQr tokenVigente(Entrada entrada) {
        return tokenRepo.findByEntradaEntradaIdAndActivoTrue(entrada.getEntradaId())
                .filter(this::estaVigente)
                .orElseGet(() -> generarParaEntrada(entrada));
    }

    // Un token sirve para validar solo si está activo y dentro de su ventana de 30s.
    public boolean estaVigente(TokenQr token) {
        return Boolean.TRUE.equals(token.getActivo())
                && token.getExpiraEn() != null
                && token.getExpiraEn().isAfter(LocalDateTime.now());
    }
}
