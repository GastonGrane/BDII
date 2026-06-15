package com.grupo4.ticketing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class TicketingApplication {

    public static void main(String[] args) {
        cargarEnv();
        SpringApplication.run(TicketingApplication.class, args);
    }

    // Lee .env desde la raíz del proyecto (un nivel arriba de /backend).
    // Solo establece propiedades que no están ya en el entorno del sistema.
    private static void cargarEnv() {
        Path[] candidatos = {
            Paths.get("../.env"),
            Paths.get(".env")
        };
        for (Path env : candidatos) {
            if (!Files.exists(env)) continue;
            try {
                Files.lines(env)
                    .map(String::trim)
                    .filter(l -> !l.startsWith("#") && l.contains("="))
                    .forEach(l -> {
                        int idx = l.indexOf('=');
                        String clave = l.substring(0, idx).trim();
                        String valor = l.substring(idx + 1).trim();
                        if (System.getProperty(clave) == null && System.getenv(clave) == null) {
                            System.setProperty(clave, valor);
                        }
                    });
            } catch (IOException e) {
                System.err.println("[WARN] No se pudo leer " + env + ": " + e.getMessage());
            }
            break;
        }
    }
}
