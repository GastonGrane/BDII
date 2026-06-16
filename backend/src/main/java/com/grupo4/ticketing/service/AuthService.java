package com.grupo4.ticketing.service;

import com.grupo4.ticketing.dto.LoginResponse;
import com.grupo4.ticketing.dto.RegistroRequest;
import com.grupo4.ticketing.entity.UsuarioGeneral;
import com.grupo4.ticketing.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// Lógica de autenticación: registro de nuevos usuarios como USUARIO_GENERAL, login con
// validación de credenciales y determinación del rol según la subtabla en que exista el mail.
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepo;
    private final AdministradorRepository adminRepo;
    private final FuncionarioRepository funcRepo;
    private final UsuarioGeneralRepository ugRepo;
    private final UsuarioService usuarioService;

    public AuthService(UsuarioRepository usuarioRepo,
                       AdministradorRepository adminRepo,
                       FuncionarioRepository funcRepo,
                       UsuarioGeneralRepository ugRepo,
                       UsuarioService usuarioService) {
        this.usuarioRepo = usuarioRepo;
        this.adminRepo   = adminRepo;
        this.funcRepo    = funcRepo;
        this.ugRepo      = ugRepo;
        this.usuarioService = usuarioService;
    }

    // Registra un usuario nuevo como USUARIO_GENERAL (con sus teléfonos).
    @Transactional
    public void registrar(RegistroRequest req) {
        usuarioService.crearUsuarioBase(req);

        UsuarioGeneral ug = new UsuarioGeneral();
        ug.setMailUsuario(req.mail());
        ug.setFechaRegistro(LocalDate.now());
        ugRepo.save(ug);
    }

    // Verifica credenciales y devuelve mail + rol. Lanza excepción si son incorrectas.
    public LoginResponse login(String mail, String contrasena) {
        usuarioRepo.findByMailAndContrasena(mail, contrasena)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales incorrectas"));

        String rol = determinarRol(mail);
        return new LoginResponse(mail, rol);
    }

    // Busca el mail en orden ADMINISTRADOR → FUNCIONARIO → USUARIO_GENERAL (por defecto).
    public String determinarRol(String mail) {
        if (adminRepo.existsById(mail)) return "ADMINISTRADOR";
        if (funcRepo.existsById(mail))  return "FUNCIONARIO";
        return "USUARIO_GENERAL";
    }
}
