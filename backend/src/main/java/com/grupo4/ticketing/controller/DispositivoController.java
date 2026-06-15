package com.grupo4.ticketing.controller;

import com.grupo4.ticketing.dto.DispositivoResponse;
import com.grupo4.ticketing.dto.ErrorResponse;
import com.grupo4.ticketing.repository.DispositivoRepository;
import com.grupo4.ticketing.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/dispositivos")
public class DispositivoController {

    private final DispositivoRepository dispositivoRepo;

    public DispositivoController(DispositivoRepository dispositivoRepo) {
        this.dispositivoRepo = dispositivoRepo;
    }

    // GET /api/dispositivos/mios — dispositivos asignados al funcionario autenticado (RNE 11)
    @GetMapping("/mios")
    public ResponseEntity<?> misDispositivos(HttpSession session) {
        String mail;
        try {
            mail = SessionUtils.requireRol(session, "FUNCIONARIO");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        }

        List<DispositivoResponse> result = dispositivoRepo
                .findByFuncionarioMailUsuario(mail)
                .stream()
                .map(d -> new DispositivoResponse(d.getDispositivoId()))
                .toList();

        return ResponseEntity.ok(result);
    }
}
