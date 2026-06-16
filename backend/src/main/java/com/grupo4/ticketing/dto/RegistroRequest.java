package com.grupo4.ticketing.dto;

import com.grupo4.ticketing.entity.enums.TipoDoc;

import java.util.List;

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
    String codPostal,
    List<String> telefonos   // un usuario puede tener varios teléfonos (atributo multivaluado)
) {}
