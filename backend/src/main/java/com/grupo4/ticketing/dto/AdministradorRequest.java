package com.grupo4.ticketing.dto;

import java.time.LocalDate;

// Alta de administrador: datos del usuario base + país sede + fecha de asignación al cargo.
public record AdministradorRequest(
        RegistroRequest datos,
        String paisSede,
        LocalDate fechaAsignacion
) {}
