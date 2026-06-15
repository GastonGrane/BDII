package com.grupo4.ticketing.dto;

import java.util.List;

public record CompraRequest(List<CompraItemRequest> items) {}
