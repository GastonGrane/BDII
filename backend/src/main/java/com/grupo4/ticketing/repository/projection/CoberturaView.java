package com.grupo4.ticketing.repository.projection;

// Proyección para el control de cobertura de sectores por funcionario (RNE 5):
// sectores asignados a un funcionario que aún no validó en un evento.
public interface CoberturaView {
    String getMailFuncionario();
    Long getEstadioId();
    String getLetraSector();
}
