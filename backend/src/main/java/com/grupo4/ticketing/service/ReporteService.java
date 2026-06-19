package com.grupo4.ticketing.service;

import com.grupo4.ticketing.dto.CoberturaEstadoResponse;
import com.grupo4.ticketing.dto.CoberturaPendienteResponse;
import com.grupo4.ticketing.dto.CompradorRankingResponse;
import com.grupo4.ticketing.dto.EventoVendidoResponse;
import com.grupo4.ticketing.repository.AsignacionFuncionarioRepository;
import com.grupo4.ticketing.repository.EventoRepository;
import com.grupo4.ticketing.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

// Reportes del panel de administración: eventos más vendidos, ranking de compradores
// y control de cobertura de sectores por funcionario (RNE 5).
@Service
public class ReporteService {

    private final EventoRepository eventoRepo;
    private final VentaRepository  ventaRepo;
    private final AsignacionFuncionarioRepository asignacionRepo;

    public ReporteService(EventoRepository eventoRepo,
                          VentaRepository ventaRepo,
                          AsignacionFuncionarioRepository asignacionRepo) {
        this.eventoRepo     = eventoRepo;
        this.ventaRepo      = ventaRepo;
        this.asignacionRepo = asignacionRepo;
    }

    @Transactional(readOnly = true)
    public List<EventoVendidoResponse> eventosMasVendidos() {
        return eventoRepo.rankingEventosPorVentas().stream()
                .map(v -> new EventoVendidoResponse(
                        v.getEventoId(),
                        v.getEquipoLocal(),
                        v.getEquipoVisitante(),
                        v.getFechaHora(),
                        v.getVendidas()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompradorRankingResponse> rankingCompradores() {
        return ventaRepo.rankingCompradores().stream()
                .map(v -> new CompradorRankingResponse(
                        v.getMail(),
                        v.getCantidad(),
                        v.getMontoGastado() != null ? v.getMontoGastado() : BigDecimal.ZERO))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CoberturaPendienteResponse> coberturaPendiente(Long eventoId) {
        return asignacionRepo.coberturaPendiente(eventoId).stream()
                .map(c -> new CoberturaPendienteResponse(
                        c.getMailFuncionario(),
                        c.getEstadioId(),
                        c.getLetraSector()))
                .toList();
    }

    // RNE 5 — verificación de cierre: el evento "cumple" cuando no queda ningún sector asignado
    // sin validación. Sirve como puerta de cierre/finalización del evento (no hay borrado de datos).
    @Transactional(readOnly = true)
    public CoberturaEstadoResponse verificarCobertura(Long eventoId) {
        List<CoberturaPendienteResponse> pendientes = coberturaPendiente(eventoId);
        return new CoberturaEstadoResponse(eventoId, pendientes.isEmpty(), pendientes);
    }
}
