package com.grupo4.ticketing.repository.projection;

import java.math.BigDecimal;

// Proyección para el ranking de mayores compradores.
public interface CompradorRankingView {
    String getMail();
    long getCantidad();
    BigDecimal getMontoGastado();
}
