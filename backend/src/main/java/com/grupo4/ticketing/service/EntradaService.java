package com.grupo4.ticketing.service;

import com.grupo4.ticketing.dto.CadenaCustodiaResponse;
import com.grupo4.ticketing.dto.CadenaCustodiaResponse.TransferenciaCustodia;
import com.grupo4.ticketing.dto.MisEntradasItemResponse;
import com.grupo4.ticketing.dto.TokenVigenteResponse;
import com.grupo4.ticketing.entity.Entrada;
import com.grupo4.ticketing.entity.Evento;
import com.grupo4.ticketing.entity.TokenQr;
import com.grupo4.ticketing.entity.enums.EstadoEntrada;
import com.grupo4.ticketing.repository.EntradaRepository;
import com.grupo4.ticketing.repository.TransferenciaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EntradaService {

    private final EntradaRepository       entradaRepo;
    private final TransferenciaRepository transferenciaRepo;
    private final TokenService            tokenService;

    public EntradaService(EntradaRepository entradaRepo,
                          TransferenciaRepository transferenciaRepo,
                          TokenService tokenService) {
        this.entradaRepo       = entradaRepo;
        this.transferenciaRepo = transferenciaRepo;
        this.tokenService      = tokenService;
    }

    // Entradas en propiedad del usuario. Para las activas se devuelve el token dinámico
    // vigente (se regenera si venció), de modo que el QR mostrado nunca esté vencido (RNE 10).
    @Transactional
    public List<MisEntradasItemResponse> misEntradas(String mail) {
        return entradaRepo.findByPropietarioMailUsuario(mail).stream()
                .map(e -> {
                    Evento ev = e.getEventoSector().getEvento();

                    String codigoQR = null;
                    LocalDateTime expiraEn = null;
                    if (e.getEstadoEntrada() == EstadoEntrada.Activa) {
                        TokenQr token = tokenService.tokenVigente(e);
                        codigoQR = token.getCodigoQR();
                        expiraEn = token.getExpiraEn();
                    }

                    return new MisEntradasItemResponse(
                            e.getEntradaId(),
                            ev.getEventoId(),
                            ev.getEquipoLocal(),
                            ev.getEquipoVisitante(),
                            ev.getFechaHora(),
                            e.getLetraSector() != null ? e.getLetraSector().name() : null,
                            e.getEstadoEntrada().name(),
                            e.getCostoHistorico(),
                            codigoQR,
                            expiraEn
                    );
                })
                .toList();
    }

    // Devuelve el token dinámico vigente de una entrada del usuario (para refresco cada 30s).
    @Transactional
    public TokenVigenteResponse tokenVigente(String mail, Long entradaId) {
        Entrada entrada = entradaRepo.findById(entradaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada no encontrada"));

        if (!entrada.getPropietario().getMailUsuario().equals(mail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No eres el propietario de esta entrada");
        }
        if (entrada.getEstadoEntrada() != EstadoEntrada.Activa) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La entrada no está activa (estado: " + entrada.getEstadoEntrada() + ")");
        }

        TokenQr token = tokenService.tokenVigente(entrada);
        return new TokenVigenteResponse(
                entrada.getEntradaId(),
                token.getCodigoQR(),
                token.getGeneradoEn(),
                token.getExpiraEn(),
                TokenService.VENTANA_SEGUNDOS
        );
    }

    // Cadena de custodia: comprador original, propietario actual e historial de transferencias.
    // Accesible para el comprador original o el propietario actual de la entrada.
    @Transactional(readOnly = true)
    public CadenaCustodiaResponse cadenaCustodia(String mail, Long entradaId) {
        Entrada entrada = entradaRepo.findById(entradaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada no encontrada"));

        String compradorOriginal  = entrada.getVenta().getComprador().getMailUsuario();
        String propietarioActual  = entrada.getPropietario().getMailUsuario();

        if (!mail.equals(compradorOriginal) && !mail.equals(propietarioActual)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el comprador original o el propietario actual pueden ver la cadena de custodia");
        }

        List<TransferenciaCustodia> transferencias = transferenciaRepo
                .findByEntradaEntradaIdOrderByFechaSolAsc(entradaId)
                .stream()
                .map(t -> new TransferenciaCustodia(
                        t.getTransfId(),
                        t.getMailOrigen(),
                        t.getMailDestino(),
                        t.getEstado().name(),
                        t.getFechaSol()))
                .toList();

        return new CadenaCustodiaResponse(
                entrada.getEntradaId(),
                entrada.getEstadoEntrada().name(),
                compradorOriginal,
                entrada.getVenta().getFecha(),
                propietarioActual,
                entrada.getEstadoEntrada() == EstadoEntrada.Consumida,
                transferencias
        );
    }
}
