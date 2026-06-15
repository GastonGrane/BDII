package com.grupo4.ticketing.controller;

import com.grupo4.ticketing.dto.CompraRequest;
import com.grupo4.ticketing.dto.CompraResponse;
import com.grupo4.ticketing.dto.ErrorResponse;
import com.grupo4.ticketing.service.VentaService;
import com.grupo4.ticketing.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Endpoints de ventas: compra de entradas (POST /api/ventas) e historial de compras (GET /api/ventas/mis-compras).
// Ambos requieren rol USUARIO_GENERAL; la sesión HTTP identifica al comprador.
@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    // GET /api/ventas/mis-compras — historial de compras del usuario autenticado
    @GetMapping("/mis-compras")
    public ResponseEntity<?> misCompras(HttpSession session) {
        String mail;
        try {
            mail = SessionUtils.requireRol(session, "USUARIO_GENERAL");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }
        return ResponseEntity.ok(ventaService.misCompras(mail));
    }

    // POST /api/ventas — compra de entradas (solo USUARIO_GENERAL)
    @PostMapping
    public ResponseEntity<?> comprar(@RequestBody CompraRequest req, HttpSession session) {
        String mail;
        try {
            mail = SessionUtils.requireRol(session, "USUARIO_GENERAL");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ErrorResponse(e.getReason()));
        }

        try {
            CompraResponse resp = ventaService.comprar(mail, req);
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse(e.getMessage()));
        }
    }
}
