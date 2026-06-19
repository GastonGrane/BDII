package com.grupo4.ticketing.service;

import com.grupo4.ticketing.dto.MisTransferenciasItemResponse;
import com.grupo4.ticketing.dto.TransferenciaRequest;
import com.grupo4.ticketing.dto.TransferenciaResponse;
import com.grupo4.ticketing.entity.Entrada;
import com.grupo4.ticketing.entity.Transferencia;
import com.grupo4.ticketing.entity.enums.EstadoEntrada;
import com.grupo4.ticketing.entity.enums.EstadoTransferencia;
import com.grupo4.ticketing.repository.EntradaRepository;
import com.grupo4.ticketing.repository.TransferenciaRepository;
import com.grupo4.ticketing.repository.UsuarioGeneralRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static com.grupo4.ticketing.util.SessionUtils.extractDbMessage;

// Gestión del ciclo de vida de transferencias: valida RNE 2 (máx. 3) y RNE 6 (solo Activa),
// crea la solicitud y delega la máquina de estados de ENTRADA a los triggers de BD.
@Service
public class TransferenciaService {

    private final EntradaRepository       entradaRepo;
    private final TransferenciaRepository transferenciaRepo;
    private final UsuarioGeneralRepository ugRepo;

    public TransferenciaService(EntradaRepository entradaRepo,
                                TransferenciaRepository transferenciaRepo,
                                UsuarioGeneralRepository ugRepo) {
        this.entradaRepo       = entradaRepo;
        this.transferenciaRepo = transferenciaRepo;
        this.ugRepo            = ugRepo;
    }

    // ── Crear solicitud de transferencia ──────────────────────────────────────
    @Transactional
    public TransferenciaResponse crearTransferencia(String mailOrigen, TransferenciaRequest req) {

        // 1. Validar que el destino no sea el mismo usuario emisor
        if (mailOrigen.equals(req.mailDestino())) {
            throw new IllegalArgumentException(
                    "No podés transferirte una entrada a vos mismo");
        }

        // 2. Obtener la entrada
        Entrada entrada = entradaRepo.findById(req.entradaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Entrada no encontrada: " + req.entradaId()));

        // 3. El usuario logueado debe ser el propietario actual (relación Posee, no Genera)
        String propietarioActual = entrada.getPropietario().getMailUsuario();
        if (!propietarioActual.equals(mailOrigen)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No eres el propietario actual de esta entrada");
        }

        // 4. RNE 6 pre-validado: la entrada debe estar Activa
        if (entrada.getEstadoEntrada() != EstadoEntrada.Activa) {
            throw new IllegalArgumentException(
                    "RNE 6: la entrada debe estar en estado Activa para transferirse "
                    + "(estado actual: " + entrada.getEstadoEntrada() + ")");
        }

        // 5. RNE 2 pre-validado: máximo 3 transferencias no rechazadas
        long noRechazadas = transferenciaRepo.countByEntradaEntradaIdAndEstadoNot(
                req.entradaId(), EstadoTransferencia.Rechazada);
        if (noRechazadas >= 3) {
            throw new IllegalArgumentException(
                    "RNE 2: esta entrada ya tiene 3 transferencias — no puede transferirse más");
        }

        // 6. El destinatario debe ser un USUARIO_GENERAL registrado
        if (!ugRepo.existsById(req.mailDestino())) {
            throw new IllegalArgumentException(
                    "El destinatario '" + req.mailDestino() + "' no existe o no es un usuario general");
        }

        // 7. Crear TRANSFERENCIA — el trigger tr_transferencia_marcar_pendiente
        //    pone la ENTRADA en PendienteTransferencia automáticamente
        Transferencia t = new Transferencia();
        t.setFechaSol(LocalDateTime.now());
        t.setEstado(EstadoTransferencia.Pendiente);
        t.setEntrada(entrada);
        t.setMailOrigen(mailOrigen);
        t.setMailDestino(req.mailDestino());

        try {
            t = transferenciaRepo.saveAndFlush(t);
        } catch (DataAccessException e) {
            throw new IllegalArgumentException(extractDbMessage(e));
        }

        return new TransferenciaResponse(t.getTransfId());
    }

    // ── Aceptar o rechazar una transferencia ──────────────────────────────────
    @Transactional
    public void resolverTransferencia(String mailSesion, Long transfId, String accion) {

        Transferencia t = transferenciaRepo.findById(transfId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transferencia no encontrada: " + transfId));

        // Solo el destinatario puede resolver
        if (!t.getMailDestino().equals(mailSesion)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el destinatario puede aceptar o rechazar esta transferencia");
        }

        // Solo se puede resolver si está Pendiente
        if (t.getEstado() != EstadoTransferencia.Pendiente) {
            throw new IllegalArgumentException(
                    "Esta transferencia ya fue " + t.getEstado().name().toLowerCase());
        }

        EstadoTransferencia nuevoEstado = parsearAccion(accion);

        // UPDATE → dispara tr_transferencia_resolver, que a su vez puede disparar
        // tr_entrada_consumida_irreversible (RNE 7) si la entrada ya fue consumida.
        // Usamos saveAndFlush para que el trigger corra dentro de este try/catch.
        try {
            t.setEstado(nuevoEstado);
            transferenciaRepo.saveAndFlush(t);
        } catch (DataAccessException e) {
            String msg = extractDbMessage(e);
            if (msg.contains("RNE 7")) {
                // Escenario PENDIENTE-01 (documentado): la entrada fue consumida mientras
                // esta transferencia estaba Pendiente. El trigger bloquea el cambio de estado.
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La entrada ya fue validada y consumida — "
                        + "la transferencia no puede completarse");
            }
            throw new IllegalArgumentException(msg);
        }
    }

    // Lista todas las transferencias donde el usuario participa como origen o destino.
    // El campo 'rol' ("ORIGEN"/"DESTINO") indica si el usuario inició o recibió la solicitud.
    @Transactional(readOnly = true)
    public List<MisTransferenciasItemResponse> misTransferencias(String mail) {
        return transferenciaRepo.findByMailOrigenOrMailDestino(mail, mail).stream()
                .map(t -> {
                    boolean esOrigen = mail.equals(t.getMailOrigen());
                    String otroUsuario = esOrigen ? t.getMailDestino() : t.getMailOrigen();
                    return new MisTransferenciasItemResponse(
                            t.getTransfId(),
                            t.getEntrada().getEntradaId(),
                            t.getEstado().name(),
                            t.getFechaSol(),
                            esOrigen ? "ORIGEN" : "DESTINO",
                            otroUsuario
                    );
                })
                .toList();
    }

    // Convierte el string "ACEPTAR"/"RECHAZAR" al enum EstadoTransferencia correspondiente.
    private EstadoTransferencia parsearAccion(String accion) {
        if (accion == null) {
            throw new IllegalArgumentException("El campo 'accion' es requerido (ACEPTAR o RECHAZAR)");
        }
        return switch (accion.toUpperCase()) {
            case "ACEPTAR"  -> EstadoTransferencia.Aceptada;
            case "RECHAZAR" -> EstadoTransferencia.Rechazada;
            default -> throw new IllegalArgumentException(
                    "Acción inválida: '" + accion + "'. Use ACEPTAR o RECHAZAR");
        };
    }
}
