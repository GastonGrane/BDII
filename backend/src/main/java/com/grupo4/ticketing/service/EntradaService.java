package com.grupo4.ticketing.service;

import com.grupo4.ticketing.dto.MisEntradasItemResponse;
import com.grupo4.ticketing.entity.Entrada;
import com.grupo4.ticketing.entity.Evento;
import com.grupo4.ticketing.entity.enums.EstadoEntrada;
import com.grupo4.ticketing.repository.EntradaRepository;
import com.grupo4.ticketing.repository.TokenQrRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EntradaService {

    private final EntradaRepository  entradaRepo;
    private final TokenQrRepository  tokenQrRepo;

    public EntradaService(EntradaRepository entradaRepo, TokenQrRepository tokenQrRepo) {
        this.entradaRepo = entradaRepo;
        this.tokenQrRepo = tokenQrRepo;
    }

    @Transactional(readOnly = true)
    public List<MisEntradasItemResponse> misEntradas(String mail) {
        return entradaRepo.findByPropietarioMailUsuario(mail).stream()
                .map(e -> {
                    Evento ev = e.getEventoSector().getEvento();

                    String codigoQR = null;
                    if (e.getEstadoEntrada() == EstadoEntrada.Activa) {
                        codigoQR = tokenQrRepo
                                .findByEntradaEntradaIdAndActivoTrue(e.getEntradaId())
                                .map(t -> t.getCodigoQR())
                                .orElse(null);
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
                            codigoQR
                    );
                })
                .toList();
    }
}
