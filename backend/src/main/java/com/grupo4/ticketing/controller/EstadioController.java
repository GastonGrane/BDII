package com.grupo4.ticketing.controller;

import com.grupo4.ticketing.dto.ErrorResponse;
import com.grupo4.ticketing.dto.EstadioDetalleResponse;
import com.grupo4.ticketing.dto.EstadioRequest;
import com.grupo4.ticketing.dto.SectorRequest;
import com.grupo4.ticketing.service.AdminService;
import com.grupo4.ticketing.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// Endpoints de estadios y sectores: listar estadios con sectores (GET) y crear estadio o sector (POST).
// Todos requieren rol ADMINISTRADOR; el listado se usa también en la creación de eventos.
@RestController
@RequestMapping("/api/estadios")
public class EstadioController {

    private final AdminService adminService;

    public EstadioController(AdminService adminService) {
        this.adminService = adminService;
    }

    // GET /api/estadios — estadios de la jurisdicción del administrador (solo ADMINISTRADOR)
    @GetMapping
    public ResponseEntity<?> listarEstadios(HttpSession session) {
        String mail;
        try {
            mail = SessionUtils.requireRol(session, "ADMINISTRADOR");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }
        return ResponseEntity.ok(adminService.listarEstadios(mail));
    }

    // POST /api/estadios — alta de estadio (solo ADMINISTRADOR, dentro de su jurisdicción)
    @PostMapping
    public ResponseEntity<?> crearEstadio(@RequestBody EstadioRequest req, HttpSession session) {
        String mail;
        try {
            mail = SessionUtils.requireRol(session, "ADMINISTRADOR");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }

        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(adminService.crearEstadio(mail, req));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // POST /api/estadios/{id}/sectores — alta de sector (solo ADMINISTRADOR, dentro de su jurisdicción)
    @PostMapping("/{id}/sectores")
    public ResponseEntity<?> crearSector(@PathVariable Long id,
                                         @RequestBody SectorRequest req,
                                         HttpSession session) {
        String mail;
        try {
            mail = SessionUtils.requireRol(session, "ADMINISTRADOR");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }

        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(adminService.crearSector(mail, id, req));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}
