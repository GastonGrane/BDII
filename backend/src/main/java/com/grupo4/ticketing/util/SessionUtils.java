package com.grupo4.ticketing.util;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class SessionUtils {

    private SessionUtils() {}

    // Devuelve el mail del usuario logueado o lanza 401.
    public static String requireLogin(HttpSession session) {
        String mail = (String) session.getAttribute("userMail");
        if (mail == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No hay sesión activa");
        }
        return mail;
    }

    // Devuelve el mail si el rol de la sesión está entre los permitidos; lanza 403 si no.
    public static String requireRol(HttpSession session, String... rolesPermitidos) {
        String mail = requireLogin(session);
        String rol  = (String) session.getAttribute("userRol");
        for (String r : rolesPermitidos) {
            if (r.equals(rol)) return mail;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Rol " + rol + " no tiene permiso para esta operación");
    }

    // Extrae el mensaje de error de BD más cercano al origen (p.ej. mensaje de un trigger SIGNAL).
    public static String extractDbMessage(Throwable e) {
        Throwable t = e;
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null && msg.contains("RNE")) {
                // Extraer desde "RNE" — JDBC puede agregar "] [SQL...]" al final
                int start = msg.indexOf("RNE");
                String rneMsg = msg.substring(start);
                int end = rneMsg.indexOf(']');
                return (end >= 0 ? rneMsg.substring(0, end) : rneMsg).trim();
            }
            t = t.getCause();
        }
        // Fallback: causa raíz
        t = e;
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() != null ? t.getMessage() : "Error en la base de datos";
    }
}
