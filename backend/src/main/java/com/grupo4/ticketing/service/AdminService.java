package com.grupo4.ticketing.service;

import com.grupo4.ticketing.dto.*;
import com.grupo4.ticketing.entity.*;
import com.grupo4.ticketing.entity.enums.LetraSector;
import com.grupo4.ticketing.repository.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.grupo4.ticketing.util.SessionUtils.extractDbMessage;

// Operaciones del administrador: alta de estadios, sectores y eventos.
// crearEvento usa saveAndFlush para que el trigger tr_evento_sin_solapamiento (RNE 4)
// lance su error dentro de la misma transacción y pueda capturarse con un try/catch.
@Service
public class AdminService {

    private final EstadioRepository       estadioRepo;
    private final SectorRepository        sectorRepo;
    private final EventoRepository        eventoRepo;
    private final EventoSectorRepository  eventoSectorRepo;
    private final AdministradorRepository adminRepo;

    public AdminService(EstadioRepository estadioRepo,
                        SectorRepository sectorRepo,
                        EventoRepository eventoRepo,
                        EventoSectorRepository eventoSectorRepo,
                        AdministradorRepository adminRepo) {
        this.estadioRepo      = estadioRepo;
        this.sectorRepo       = sectorRepo;
        this.eventoRepo       = eventoRepo;
        this.eventoSectorRepo = eventoSectorRepo;
        this.adminRepo        = adminRepo;
    }

    // ── POST /api/estadios ────────────────────────────────────────────────────
    @Transactional
    public EstadioResponse crearEstadio(EstadioRequest req) {
        if (req.nombre() == null || req.nombre().isBlank())
            throw new IllegalArgumentException("El nombre del estadio es requerido");
        if (req.pais() == null || req.pais().isBlank())
            throw new IllegalArgumentException("El país del estadio es requerido");
        if (req.ciudad() == null || req.ciudad().isBlank())
            throw new IllegalArgumentException("La ciudad del estadio es requerida");

        Estadio e = new Estadio();
        e.setNombre(req.nombre());
        e.setPais(req.pais());
        e.setCiudad(req.ciudad());
        e = estadioRepo.save(e);

        return new EstadioResponse(e.getEstadioId(), e.getNombre(), e.getPais(), e.getCiudad());
    }

    // ── POST /api/estadios/{id}/sectores ──────────────────────────────────────
    // Agrega un sector (A-D) al estadio dado; valida que la letra no esté ya registrada en ese estadio.
    @Transactional
    public SectorListItem crearSector(Long estadioId, SectorRequest req) {
        Estadio estadio = estadioRepo.findById(estadioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Estadio no encontrado: " + estadioId));

        if (req.capacidadMax() == null || req.capacidadMax() < 1)
            throw new IllegalArgumentException("CapacidadMax debe ser mayor a 0");
        if (req.costoEntrada() == null || req.costoEntrada().signum() <= 0)
            throw new IllegalArgumentException("CostoEntrada debe ser mayor a 0");

        LetraSector letra = parseSector(req.letraSector());

        SectorId sId = new SectorId(estadioId, letra);
        if (sectorRepo.existsById(sId))
            throw new IllegalArgumentException(
                    "El sector " + letra + " ya existe en el estadio " + estadioId);

        Sector s = new Sector();
        s.setId(sId);
        s.setEstadio(estadio);
        s.setCapacidadMax(req.capacidadMax());
        s.setCostoEntrada(req.costoEntrada());
        sectorRepo.save(s);

        return new SectorListItem(letra.name(), req.capacidadMax(), req.costoEntrada());
    }

    // ── POST /api/eventos ─────────────────────────────────────────────────────
    @Transactional
    public EventoResponse crearEvento(String mailAdmin, EventoRequest req) {
        if (req.sectores() == null || req.sectores().isEmpty())
            throw new IllegalArgumentException("El evento debe tener al menos un sector habilitado");

        Estadio estadio = estadioRepo.findById(req.estadioId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Estadio no encontrado: " + req.estadioId()));

        Evento evento = new Evento();
        evento.setEquipoLocal(req.equipoLocal());
        evento.setEquipoVisitante(req.equipoVisitante());
        evento.setFechaHora(req.fechaHora());
        evento.setEstadio(estadio);
        evento.setAdministrador(adminRepo.getReferenceById(mailAdmin));

        // saveAndFlush: el trigger tr_evento_sin_solapamiento corre aquí
        try {
            evento = eventoRepo.saveAndFlush(evento);
        } catch (DataAccessException e) {
            String msg = extractDbMessage(e);
            if (msg.contains("RNE 4")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, msg);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }

        // Crear filas EVENTO_SECTOR para cada sector habilitado
        for (String letraStr : req.sectores()) {
            LetraSector letra = parseSector(letraStr);

            SectorId sId = new SectorId(req.estadioId(), letra);
            if (!sectorRepo.existsById(sId))
                throw new IllegalArgumentException(
                        "El sector " + letra + " no existe en el estadio " + req.estadioId()
                        + ". Crealo primero con POST /api/estadios/" + req.estadioId() + "/sectores");

            EventoSectorId esId = new EventoSectorId(evento.getEventoId(), req.estadioId(), letra);
            EventoSector es = new EventoSector();
            es.setId(esId);
            es.setEvento(evento);
            es.setSector(sectorRepo.getReferenceById(sId));
            eventoSectorRepo.save(es);
        }

        return new EventoResponse(
                evento.getEventoId(),
                evento.getEquipoLocal(),
                evento.getEquipoVisitante(),
                evento.getFechaHora(),
                req.estadioId()
        );
    }

    // ── GET /api/estadios ─────────────────────────────────────────────────────
    // Devuelve todos los estadios con sus sectores anidados (estructura que usa el frontend para crear eventos).
    @Transactional(readOnly = true)
    public List<EstadioDetalleResponse> listarEstadios() {
        return estadioRepo.findAll().stream()
                .map(e -> {
                    List<SectorListItem> sectores = sectorRepo
                            .findByIdEstadioId(e.getEstadioId())
                            .stream()
                            .map(s -> new SectorListItem(
                                    s.getId().getLetraSector().name(),
                                    s.getCapacidadMax(),
                                    s.getCostoEntrada()))
                            .toList();
                    return new EstadioDetalleResponse(
                            e.getEstadioId(), e.getNombre(), e.getPais(), e.getCiudad(), sectores);
                })
                .toList();
    }

    // ── GET /api/eventos ──────────────────────────────────────────────────────
    // Devuelve eventos ordenados por fecha ascendente, con datos del estadio y sectores habilitados.
    @Transactional(readOnly = true)
    public List<EventoListItemResponse> listarEventos() {
        return eventoRepo.findAllByOrderByFechaHoraAsc().stream()
                .map(ev -> {
                    Estadio est = ev.getEstadio();
                    EstadioResponse estadioDto = new EstadioResponse(
                            est.getEstadioId(), est.getNombre(), est.getPais(), est.getCiudad());

                    List<SectorListItem> sectores = eventoSectorRepo
                            .findByIdEventoId(ev.getEventoId())
                            .stream()
                            .map(es -> {
                                Sector s = es.getSector();
                                return new SectorListItem(
                                        es.getId().getLetraSector().name(),
                                        s.getCapacidadMax(),
                                        s.getCostoEntrada()
                                );
                            })
                            .toList();

                    return new EventoListItemResponse(
                            ev.getEventoId(),
                            ev.getEquipoLocal(),
                            ev.getEquipoVisitante(),
                            ev.getFechaHora(),
                            estadioDto,
                            sectores
                    );
                })
                .toList();
    }

    private LetraSector parseSector(String s) {
        try {
            return LetraSector.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Letra de sector inválida: '" + s + "'. Valores: A, B, C, D");
        }
    }
}
