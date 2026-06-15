package com.grupo4.ticketing.repository;

import com.grupo4.ticketing.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {
    Optional<Usuario> findByMailAndContrasena(String mail, String contrasena);
}
