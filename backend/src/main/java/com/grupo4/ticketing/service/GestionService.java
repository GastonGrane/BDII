package com.grupo4.ticketing.service;

import com.grupo4.ticketing.dto.*;
import com.grupo4.ticketing.entity.*;
import com.grupo4.ticketing.entity.enums.LetraSector;
import com.grupo4.ticketing.repository.*;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.grupo4.ticketing.util.SessionUtils.extractDbMessage;

// Altas de gestión que realiza el administrador: funcionarios, otros administradores,
// dispositivos autorizados y asignación de funcionarios a sectores de un evento.
// Permite ejecutar la demo completa desde la aplicación, sin depender del seed.
@Service
public class GestionService {

    private final UsuarioService          usuarioService;
    private final FuncionarioRepository   funcionarioRepo;
    private final AdministradorRepository administradorRepo;
    private final DispositivoRepository   dispositivoRepo;
    private final EventoSectorRepository  eventoSectorRepo;
    private final AsignacionFuncionarioRepository asignacionRepo;

    public GestionService(UsuarioService usuarioService,
                          FuncionarioRepository funcionarioRepo,
                          AdministradorRepository administradorRepo,
                          DispositivoRepository dispositivoRepo,
                          EventoSectorRepository eventoSectorRepo,
                          AsignacionFuncionarioRepository asignacionRepo) {
        this.usuarioService    = usuarioService;
        this.funcionarioRepo   = funcionarioRepo;
        this.administradorRepo = administradorRepo;
        this.dispositivoRepo   = dispositivoRepo;
        this.eventoSectorRepo  = eventoSectorRepo;
        this.asignacionRepo    = asignacionRepo;
    }

    // Alta de funcionario de validación (usuario base + legajo).
    @Transactional
    public void crearFuncionario(FuncionarioRequest req) {
        if (req.nroLegajo() == null || req.nroLegajo().isBlank()) {
            throw new IllegalArgumentException("El número de legajo es requerido");
        }
        Usuario u = usuarioService.crearUsuarioBase(req.datos());

        Funcionario f = new Funcionario();
        f.setMailUsuario(u.getMail());
        f.setNroLegajo(req.nroLegajo());
        try {
            funcionarioRepo.save(f);
        } catch (DataAccessException e) {
            throw new IllegalArgumentException(extractDbMessage(e));
        }
    }

    // Alta de administrador por país sede (usuario base + jurisdicción + fecha de asignación).
    @Transactional
    public void crearAdministrador(AdministradorRequest req) {
        if (req.paisSede() == null || req.paisSede().isBlank()) {
            throw new IllegalArgumentException("El país sede es requerido");
        }
        if (req.fechaAsignacion() == null) {
            throw new IllegalArgumentException("La fecha de asignación es requerida");
        }
        Usuario u = usuarioService.crearUsuarioBase(req.datos());

        Administrador a = new Administrador();
        a.setMailUsuario(u.getMail());
        a.setPaisSede(req.paisSede());
        a.setFechaAsignacion(req.fechaAsignacion());
        administradorRepo.save(a);
    }

    // Alta de dispositivo autorizado, vinculado obligatoriamente a un funcionario (RNE 11).
    @Transactional
    public DispositivoResponse crearDispositivo(DispositivoRequest req) {
        Funcionario funcionario = funcionarioRepo.findById(req.mailFuncionario())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El funcionario '" + req.mailFuncionario() + "' no existe"));

        Dispositivo d = new Dispositivo();
        d.setFuncionario(funcionario);
        d = dispositivoRepo.save(d);
        return new DispositivoResponse(d.getDispositivoId());
    }

    // Asigna un funcionario a un sector habilitado de un evento (RNE 5).
    @Transactional
    public void asignarFuncionario(AsignacionRequest req) {
        if (!funcionarioRepo.existsById(req.mailFuncionario())) {
            throw new IllegalArgumentException(
                    "El funcionario '" + req.mailFuncionario() + "' no existe");
        }
        LetraSector letra = parseSector(req.letraSector());
        EventoSectorId esId = new EventoSectorId(req.eventoId(), req.estadioId(), letra);
        if (!eventoSectorRepo.existsById(esId)) {
            throw new IllegalArgumentException(
                    "El sector " + letra + " no está habilitado para el evento " + req.eventoId());
        }

        AsignacionFuncionario a = new AsignacionFuncionario();
        a.setId(new AsignacionFuncionarioId(req.mailFuncionario(), req.eventoId(), req.estadioId(), letra));
        a.setFuncionario(funcionarioRepo.getReferenceById(req.mailFuncionario()));
        try {
            asignacionRepo.save(a);
        } catch (DataAccessException e) {
            throw new IllegalArgumentException(extractDbMessage(e));
        }
    }

    private LetraSector parseSector(String s) {
        try {
            return LetraSector.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Letra de sector inválida: '" + s + "'. Valores: A, B, C, D");
        }
    }
}
