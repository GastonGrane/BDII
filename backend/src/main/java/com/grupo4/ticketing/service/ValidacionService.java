package com.grupo4.ticketing.service;

import com.grupo4.ticketing.dto.ValidacionRequest;
import com.grupo4.ticketing.dto.ValidacionResponse;
import com.grupo4.ticketing.entity.Dispositivo;
import com.grupo4.ticketing.entity.Entrada;
import com.grupo4.ticketing.entity.Validacion;
import com.grupo4.ticketing.entity.enums.EstadoEntrada;
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

import static com.grupo4.ticketing.util.SessionUtils.extractDbMessage;

// Validación de ingreso al estadio: verifica token activo (RNE 9), dispositivo asignado al
// funcionario (RNE 11) y que la entrada no esté consumida (RNE 7 — defensa en profundidad).
// El trigger tr_validacion_post_insert completa el marcado de ENTRADA como Consumida.
@Service
public class ValidacionService {

    private final TokenQrRepository    tokenRepo;
    private final DispositivoRepository dispositivoRepo;
    private final FuncionarioRepository  funcionarioRepo;
    private final ValidacionRepository   validacionRepo;
    private final TokenService           tokenService;

    public ValidacionService(TokenQrRepository tokenRepo,
                             DispositivoRepository dispositivoRepo,
                             FuncionarioRepository funcionarioRepo,
                             ValidacionRepository validacionRepo,
                             TokenService tokenService) {
        this.tokenRepo      = tokenRepo;
        this.dispositivoRepo = dispositivoRepo;
        this.funcionarioRepo = funcionarioRepo;
        this.validacionRepo  = validacionRepo;
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
}
