package com.grupo4.ticketing.controller;

import com.grupo4.ticketing.dto.ErrorResponse;
import com.grupo4.ticketing.dto.ValidacionRequest;
import com.grupo4.ticketing.dto.ValidacionResponse;
import com.grupo4.ticketing.service.ValidacionService;
import com.grupo4.ticketing.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// Endpoint de validación de ingreso al estadio: solo accesible por FUNCIONARIO.
// El servicio verifica que el dispositivo usado esté asignado al funcionario autenticado (RNE 11).
@RestController
@RequestMapping("/api/validaciones")
public class ValidacionController {

    private final ValidacionService validacionService;

    public ValidacionController(ValidacionService validacionService) {
        this.validacionService = validacionService;
    }

    // POST /api/validaciones — validar ingreso (solo FUNCIONARIO)
    @PostMapping
    public ResponseEntity<?> validar(@RequestBody ValidacionRequest req, HttpSession session) {
        String mail;
        try {
            mail = SessionUtils.requireRol(session, "FUNCIONARIO");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }

        try {
            ValidacionResponse resp = validacionService.validar(mail, req);
            return ResponseEntity.ok(resp);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }
    }

    // GET /api/validaciones/cobertura — checklist de cobertura del funcionario autenticado (RNE 5):
    // sus sectores asignados por evento, marcados como cumplidos o pendientes.
    @GetMapping("/cobertura")
    public ResponseEntity<?> coberturaPropia(HttpSession session) {
        String mail;
        try {
            mail = SessionUtils.requireRol(session, "FUNCIONARIO");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }
        return ResponseEntity.ok(validacionService.coberturaDelFuncionario(mail));
    }
}
