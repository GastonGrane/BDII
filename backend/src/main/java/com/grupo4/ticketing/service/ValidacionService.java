package com.grupo4.ticketing.service;

import com.grupo4.ticketing.dto.CoberturaSectorResponse;
import com.grupo4.ticketing.dto.ValidacionRequest;
import com.grupo4.ticketing.dto.ValidacionResponse;
import com.grupo4.ticketing.entity.AsignacionFuncionarioId;
import com.grupo4.ticketing.entity.Dispositivo;
import com.grupo4.ticketing.entity.Entrada;
import com.grupo4.ticketing.entity.Validacion;
import com.grupo4.ticketing.entity.enums.EstadoEntrada;
import com.grupo4.ticketing.entity.enums.LetraSector;
import com.grupo4.ticketing.repository.AsignacionFuncionarioRepository;
import com.grupo4.ticketing.repository.DispositivoRepository;
import com.grupo4.ticketing.repository.FuncionarioRepository;
import com.grupo4.ticketing.repository.TokenQrRepository;
import com.grupo4.ticketing.repository.ValidacionRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static com.grupo4.ticketing.util.SessionUtils.extractDbMessage;

// Validación de ingreso al estadio: verifica token activo (RNE 9), dispositivo asignado al
// funcionario (RNE 11), que el funcionario esté asignado al sector de la entrada (RNE 5) y que
// la entrada no esté consumida (RNE 7 — defensa en profundidad).
// El trigger tr_validacion_post_insert completa el marcado de ENTRADA como Consumida.
// RNE 8 (entrada "al portador"): no se verifica la identidad del propietario;
// basta con presentar un token QR vigente.
@Service
public class ValidacionService {

    private final TokenQrRepository    tokenRepo;
    private final DispositivoRepository dispositivoRepo;
    private final FuncionarioRepository  funcionarioRepo;
    private final ValidacionRepository   validacionRepo;
    private final AsignacionFuncionarioRepository asignacionRepo;
    private final TokenService           tokenService;

    public ValidacionService(TokenQrRepository tokenRepo,
                             DispositivoRepository dispositivoRepo,
                             FuncionarioRepository funcionarioRepo,
                             ValidacionRepository validacionRepo,
                             AsignacionFuncionarioRepository asignacionRepo,
                             TokenService tokenService) {
        this.tokenRepo      = tokenRepo;
        this.dispositivoRepo = dispositivoRepo;
        this.funcionarioRepo = funcionarioRepo;
        this.validacionRepo  = validacionRepo;
        this.asignacionRepo  = asignacionRepo;
        this.tokenService    = tokenService;
    }

    @Transactional
    public ValidacionResponse validar(String mailFuncionario, ValidacionRequest req) {

        // 1. Buscar token activo por código QR
        var token = tokenRepo.findByCodigoQRAndActivoTrue(req.codigoQR())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Código QR inválido o no vigente"));

        // 1b. Entrada Dinámica (RNE 10): el token debe estar dentro de su ventana de 30s.
        //     Un token vencido no se acepta aunque siga marcado como activo.
        if (!tokenService.estaVigente(token)) {
            throw new ResponseStatusException(HttpStatus.GONE,
                    "Token vencido (ventana de " + TokenService.VENTANA_SEGUNDOS
                    + "s). El asistente debe mostrar el QR actualizado.");
        }

        // 2. Verificar que el dispositivo pertenece al funcionario autenticado (RNE 11)
        Dispositivo dispositivo = dispositivoRepo.findById(req.dispositivoId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dispositivo no encontrado: " + req.dispositivoId()));

        if (!dispositivo.getFuncionario().getMailUsuario().equals(mailFuncionario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El dispositivo no está asignado a tu cuenta de funcionario");
        }

        // 3. Defensa en profundidad: verificar que la entrada no esté ya Consumida
        //    (el trigger tr_validacion_entrada_no_consumida también lo cubre)
        Entrada entrada = token.getEntrada();
        if (entrada.getEstadoEntrada() == EstadoEntrada.Consumida) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La entrada ya fue validada anteriormente (RNE 7)");
        }

        // 3b. RNE 5: el funcionario solo puede validar entradas de los sectores que tiene
        //     asignados en ese evento. Una validación sobre un sector no asignado se rechaza,
        //     de modo que las validaciones exitosas siempre corresponden a un sector asignado.
        LetraSector letra = entrada.getLetraSector();
        AsignacionFuncionarioId asigId = new AsignacionFuncionarioId(
                mailFuncionario, entrada.getEventoId(), entrada.getEstadioId(), letra);
        if (!asignacionRepo.existsById(asigId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "RNE 5: no estás asignado al sector " + (letra != null ? letra.name() : "?")
                    + " del evento " + entrada.getEventoId());
        }

        // 4. Crear VALIDACION — dispara tr_validacion_consumir_entrada:
        //    marca la ENTRADA como Consumida y desactiva todos sus tokens
        Validacion v = new Validacion();
        v.setTokenQr(token);
        v.setFuncionario(funcionarioRepo.getReferenceById(mailFuncionario));
        v.setDispositivo(dispositivo);
        v.setFechaHora(LocalDateTime.now());

        try {
            validacionRepo.saveAndFlush(v);
        } catch (DataAccessException e) {
            String msg = extractDbMessage(e);
            if (msg.contains("RNE 7")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La entrada ya fue consumida — " + msg);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
        }

        // 5. Construir respuesta con datos relevantes para el funcionario
        return new ValidacionResponse(
                entrada.getEntradaId(),
                entrada.getEventoId(),
                entrada.getLetraSector() != null ? entrada.getLetraSector().name() : null,
                entrada.getPropietario().getMailUsuario()
        );
    }

    // RNE 5 — checklist del funcionario: todos los sectores que tiene asignados (por evento)
    // con la marca de cumplido/pendiente. El front lo refresca tras cada validación exitosa.
    @Transactional(readOnly = true)
    public List<CoberturaSectorResponse> coberturaDelFuncionario(String mailFuncionario) {
        return asignacionRepo.coberturaPorFuncionario(mailFuncionario).stream()
                .map(c -> new CoberturaSectorResponse(
                        c.getEventoId(),
                        c.getEquipoLocal(),
                        c.getEquipoVisitante(),
                        c.getFechaHora(),
                        c.getEstadioId(),
                        c.getLetraSector(),
                        Boolean.TRUE.equals(c.getCubierto())))
                .toList();
    }
}
