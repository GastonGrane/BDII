package com.grupo4.ticketing.dto;

// Alta de dispositivo autorizado: queda vinculado obligatoriamente a un funcionario (RNE 11).
public record DispositivoRequest(String mailFuncionario) {}
