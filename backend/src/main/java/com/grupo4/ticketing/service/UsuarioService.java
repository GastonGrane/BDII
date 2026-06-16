package com.grupo4.ticketing.service;

import com.grupo4.ticketing.dto.RegistroRequest;
import com.grupo4.ticketing.entity.Telefono;
import com.grupo4.ticketing.entity.Usuario;
import com.grupo4.ticketing.repository.TelefonoRepository;
import com.grupo4.ticketing.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Creación de la superclase USUARIO (datos comunes a todos los roles) y sus teléfonos.
// Lo reutilizan el registro de usuario general (AuthService) y las altas de
// administrador/funcionario (GestionService), evitando duplicar el armado del usuario.
@Service
public class UsuarioService {

    private final UsuarioRepository  usuarioRepo;
    private final TelefonoRepository telefonoRepo;

    public UsuarioService(UsuarioRepository usuarioRepo, TelefonoRepository telefonoRepo) {
        this.usuarioRepo  = usuarioRepo;
        this.telefonoRepo = telefonoRepo;
    }

    // Crea la fila USUARIO y sus teléfonos. No define el subtipo (eso lo hace cada caller).
    @Transactional
    public Usuario crearUsuarioBase(RegistroRequest req) {
        if (req.mail() == null || req.mail().isBlank()) {
            throw new IllegalArgumentException("El mail es requerido");
        }
        if (usuarioRepo.existsById(req.mail())) {
            throw new IllegalArgumentException("El mail ya está registrado");
        }

        Usuario u = new Usuario();
        u.setMail(req.mail());
        u.setContrasena(req.contrasena());
        u.setTipoDoc(req.tipoDoc());
        u.setPaisDoc(req.paisDoc());
        u.setNroDoc(req.nroDoc());
        u.setPaisDir(req.paisDir());
        u.setLocalidad(req.localidad());
        u.setCalle(req.calle());
        u.setNroPuerta(req.nroPuerta());
        u.setCodPostal(req.codPostal());
        usuarioRepo.save(u);

        if (req.telefonos() != null) {
            req.telefonos().stream()
                    .filter(t -> t != null && !t.isBlank())
                    .distinct()
                    .forEach(t -> telefonoRepo.save(new Telefono(u, t.trim())));
        }

        return u;
    }
}
