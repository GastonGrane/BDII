package com.grupo4.ticketing.controller;

import com.grupo4.ticketing.dto.*;
import com.grupo4.ticketing.service.GestionService;
import com.grupo4.ticketing.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// Altas que realiza el administrador: funcionarios, administradores, dispositivos y asignaciones.
// El primer administrador viene del seed; desde él se crean los demás.
@RestController
@RequestMapping("/api/gestion")
public class GestionController {

    private final GestionService gestionService;

    public GestionController(GestionService gestionService) {
        this.gestionService = gestionService;
    }

    @PostMapping("/funcionarios")
    public ResponseEntity<?> crearFuncionario(@RequestBody FuncionarioRequest req, HttpSession session) {
        ResponseEntity<?> denegado = requireAdmin(session);
        if (denegado != null) return denegado;
        try {
            gestionService.crearFuncionario(req);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/administradores")
    public ResponseEntity<?> crearAdministrador(@RequestBody AdministradorRequest req, HttpSession session) {
        ResponseEntity<?> denegado = requireAdmin(session);
        if (denegado != null) return denegado;
        try {
            gestionService.crearAdministrador(req);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/dispositivos")
    public ResponseEntity<?> crearDispositivo(@RequestBody DispositivoRequest req, HttpSession session) {
        ResponseEntity<?> denegado = requireAdmin(session);
        if (denegado != null) return denegado;
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(gestionService.crearDispositivo(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/asignaciones")
    public ResponseEntity<?> asignarFuncionario(@RequestBody AsignacionRequest req, HttpSession session) {
        ResponseEntity<?> denegado = requireAdmin(session);
        if (denegado != null) return denegado;
        try {
            gestionService.asignarFuncionario(req);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Devuelve una respuesta de error si la sesión no es de un ADMINISTRADOR; null si está autorizado.
    private ResponseEntity<?> requireAdmin(HttpSession session) {
        try {
            SessionUtils.requireRol(session, "ADMINISTRADOR");
            return null;
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }
    }
}
