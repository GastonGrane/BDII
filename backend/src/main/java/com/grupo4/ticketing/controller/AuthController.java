package com.grupo4.ticketing.controller;

import com.grupo4.ticketing.dto.ErrorResponse;
import com.grupo4.ticketing.dto.LoginRequest;
import com.grupo4.ticketing.dto.LoginResponse;
import com.grupo4.ticketing.dto.RegistroRequest;
import com.grupo4.ticketing.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Endpoints de autenticación: registro (crea USUARIO_GENERAL), login (abre sesión HTTP),
// logout (invalida sesión) y /yo (devuelve el usuario activo para que el frontend sepa su rol).
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/auth/registro — registra un nuevo USUARIO_GENERAL
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegistroRequest req) {
        try {
            authService.registrar(req);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // POST /api/auth/login — verifica credenciales y abre sesión
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpSession session) {
        try {
            LoginResponse resp = authService.login(req.mail(), req.contrasena());
            session.setAttribute("userMail", resp.mail());
            session.setAttribute("userRol",  resp.rol());
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage()));
        }
    }

    // POST /api/auth/logout — cierra sesión
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    // GET /api/auth/yo — devuelve el usuario de sesión actual (para que el frontend sepa si está logueado)
    @GetMapping("/yo")
    public ResponseEntity<?> yo(HttpSession session) {
        String mail = (String) session.getAttribute("userMail");
        String rol  = (String) session.getAttribute("userRol");
        if (mail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("No hay sesión activa"));
        }
        return ResponseEntity.ok(new LoginResponse(mail, rol));
    }
}
