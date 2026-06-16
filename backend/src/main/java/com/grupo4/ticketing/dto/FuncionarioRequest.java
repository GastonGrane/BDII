package com.grupo4.ticketing.dto;

// Alta de funcionario: datos del usuario base + número de legajo.
public record FuncionarioRequest(
        RegistroRequest datos,
        String nroLegajo
) {}
