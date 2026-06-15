package com.grupo4.ticketing.controller;

import com.grupo4.ticketing.dto.ErrorResponse;
import com.grupo4.ticketing.service.EntradaService;
import com.grupo4.ticketing.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/entradas")
public class EntradaController {

    private final EntradaService entradaService;

    public EntradaController(EntradaService entradaService) {
        this.entradaService = entradaService;
    }

    // GET /api/entradas/mis-entradas — tickets en propiedad del usuario autenticado
    @GetMapping("/mis-entradas")
    public ResponseEntity<?> misEntradas(HttpSession session) {
        String mail;
        try {
            mail = SessionUtils.requireRol(session, "USUARIO_GENERAL");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }
        return ResponseEntity.ok(entradaService.misEntradas(mail));
    }
}
