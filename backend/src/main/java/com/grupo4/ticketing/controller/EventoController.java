package com.grupo4.ticketing.controller;

import com.grupo4.ticketing.dto.ErrorResponse;
import com.grupo4.ticketing.dto.EventoRequest;
import com.grupo4.ticketing.service.AdminService;
import com.grupo4.ticketing.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// Endpoints de eventos: listar (GET — requiere sesión, accesible a todos los roles)
// y crear (POST — solo ADMINISTRADOR). La creación puede rechazarse con 409 si hay solapamiento (RNE 4).
@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final AdminService adminService;

    public EventoController(AdminService adminService) {
        this.adminService = adminService;
    }

    // GET /api/eventos — listado público (requiere solo sesión activa)
    @GetMapping
    public ResponseEntity<?> listar(HttpSession session) {
        try {
            SessionUtils.requireLogin(session);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }
        return ResponseEntity.ok(adminService.listarEventos());
    }

    // POST /api/eventos — alta de evento (solo ADMINISTRADOR)
    @PostMapping
    public ResponseEntity<?> crearEvento(@RequestBody EventoRequest req, HttpSession session) {
        String mail;
        try {
            mail = SessionUtils.requireRol(session, "ADMINISTRADOR");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }

        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(adminService.crearEvento(mail, req));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}
