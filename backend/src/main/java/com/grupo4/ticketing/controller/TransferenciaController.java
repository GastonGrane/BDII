package com.grupo4.ticketing.controller;

import com.grupo4.ticketing.dto.AccionTransferenciaRequest;
import com.grupo4.ticketing.dto.ErrorResponse;
import com.grupo4.ticketing.dto.TransferenciaRequest;
import com.grupo4.ticketing.dto.TransferenciaResponse;
import com.grupo4.ticketing.service.TransferenciaService;
import com.grupo4.ticketing.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// Endpoints de transferencias: crear solicitud (POST), aceptar/rechazar (PUT /{id}) e historial (GET).
// Solo accesibles por USUARIO_GENERAL; el destinatario es quien puede resolver la transferencia.
@RestController
@RequestMapping("/api/transferencias")
public class TransferenciaController {

    private final TransferenciaService transferenciaService;

    public TransferenciaController(TransferenciaService transferenciaService) {
        this.transferenciaService = transferenciaService;
    }

    // GET /api/transferencias/mis-transferencias — historial de transferencias del usuario
    @GetMapping("/mis-transferencias")
    public ResponseEntity<?> misTransferencias(HttpSession session) {
        String mail;
        try {
            mail = SessionUtils.requireRol(session, "USUARIO_GENERAL");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }
        return ResponseEntity.ok(transferenciaService.misTransferencias(mail));
    }

    // POST /api/transferencias — solicitar una transferencia (solo USUARIO_GENERAL)
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody TransferenciaRequest req, HttpSession session) {
        String mail;
        try {
            mail = SessionUtils.requireRol(session, "USUARIO_GENERAL");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }

        try {
            TransferenciaResponse resp = transferenciaService.crearTransferencia(mail, req);
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // PUT /api/transferencias/{id} — aceptar o rechazar (solo USUARIO_GENERAL destinatario)
    @PutMapping("/{id}")
    public ResponseEntity<?> resolver(@PathVariable Long id,
                                      @RequestBody AccionTransferenciaRequest req,
                                      HttpSession session) {
        String mail;
        try {
            mail = SessionUtils.requireRol(session, "USUARIO_GENERAL");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }

        try {
            transferenciaService.resolverTransferencia(mail, id, req.accion());
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}
