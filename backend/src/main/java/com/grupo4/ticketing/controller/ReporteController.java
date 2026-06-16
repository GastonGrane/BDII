package com.grupo4.ticketing.controller;

import com.grupo4.ticketing.dto.ErrorResponse;
import com.grupo4.ticketing.service.ReporteService;
import com.grupo4.ticketing.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// Reportes del administrador: eventos más vendidos, ranking de compradores y cobertura (RNE 5).
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    // GET /api/reportes/eventos-mas-vendidos
    @GetMapping("/eventos-mas-vendidos")
    public ResponseEntity<?> eventosMasVendidos(HttpSession session) {
        try {
            SessionUtils.requireRol(session, "ADMINISTRADOR");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }
        return ResponseEntity.ok(reporteService.eventosMasVendidos());
    }

    // GET /api/reportes/ranking-compradores
    @GetMapping("/ranking-compradores")
    public ResponseEntity<?> rankingCompradores(HttpSession session) {
        try {
            SessionUtils.requireRol(session, "ADMINISTRADOR");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }
        return ResponseEntity.ok(reporteService.rankingCompradores());
    }

    // GET /api/reportes/cobertura/{eventoId} — sectores asignados aún no cubiertos (RNE 5)
    @GetMapping("/cobertura/{eventoId}")
    public ResponseEntity<?> cobertura(@PathVariable Long eventoId, HttpSession session) {
        try {
            SessionUtils.requireRol(session, "ADMINISTRADOR");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }
        return ResponseEntity.ok(reporteService.coberturaPendiente(eventoId));
    }
}
