package com.grupo4.ticketing.dto;

import com.grupo4.ticketing.entity.enums.TipoDoc;

public record RegistroRequest(
    String mail,
    String contrasena,
    TipoDoc tipoDoc,
    String paisDoc,
    String nroDoc,
    String paisDir,
    String localidad,
    String calle,
    String nroPuerta,
    String codPostal
) {}
