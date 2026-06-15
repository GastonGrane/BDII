package com.grupo4.ticketing.dto;

import java.math.BigDecimal;

public record SectorRequest(String letraSector, Integer capacidadMax, BigDecimal costoEntrada) {}
