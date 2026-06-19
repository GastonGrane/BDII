package com.grupo4.ticketing.service;

import com.grupo4.ticketing.dto.ValidacionRequest;
import com.grupo4.ticketing.dto.ValidacionResponse;
import com.grupo4.ticketing.entity.*;
import com.grupo4.ticketing.entity.enums.EstadoEntrada;
import com.grupo4.ticketing.entity.enums.LetraSector;
import com.grupo4.ticketing.repository.AsignacionFuncionarioRepository;
import com.grupo4.ticketing.repository.DispositivoRepository;
import com.grupo4.ticketing.repository.FuncionarioRepository;
import com.grupo4.ticketing.repository.TokenQrRepository;
import com.grupo4.ticketing.repository.ValidacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// RNE 5 — un funcionario solo puede validar entradas de los sectores que tiene asignados en
// el evento. Una validación sobre un sector no asignado se rechaza (y por lo tanto no cuenta
// para la cobertura). Una validación sobre un sector asignado se registra correctamente.
// RNE 8 — la entrada es "al portador": no se verifica la identidad del propietario; basta
// presentar un token vigente.
// RNE 10 — un token fuera de su ventana de 30s se rechaza aunque siga marcado como activo.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ValidacionServiceTest {

    @Mock TokenQrRepository               tokenRepo;
    @Mock DispositivoRepository           dispositivoRepo;
    @Mock FuncionarioRepository           funcionarioRepo;
    @Mock ValidacionRepository            validacionRepo;
    @Mock AsignacionFuncionarioRepository asignacionRepo;
    @Mock TokenService                    tokenService;

    private static final String FUNC = "func@ticketing.com";

    private ValidacionService service() {
        return new ValidacionService(
                tokenRepo, dispositivoRepo, funcionarioRepo, validacionRepo, asignacionRepo, tokenService);
    }

    // Arma un token activo y vigente para una entrada Activa del evento 1, estadio 1, sector A.
    private TokenQr escenarioBase() {
        EventoSector es = new EventoSector();
        es.setId(new EventoSectorId(1L, 1L, LetraSector.A));

        UsuarioGeneral prop = new UsuarioGeneral();
        prop.setMailUsuario("user1@test.com");

        Entrada entrada = new Entrada();
        entrada.setEntradaId(1L);
        entrada.setEstadoEntrada(EstadoEntrada.Activa);
        entrada.setEventoSector(es);
        entrada.setPropietario(prop);

        TokenQr token = new TokenQr();
        token.setCodigoQR("QR-OK");
        token.setActivo(true);
        token.setEntrada(entrada);

        when(tokenRepo.findByCodigoQRAndActivoTrue("QR-OK")).thenReturn(Optional.of(token));
        when(tokenService.estaVigente(token)).thenReturn(true);

        Funcionario func = new Funcionario();
        func.setMailUsuario(FUNC);
        Dispositivo disp = new Dispositivo();
        disp.setDispositivoId(1L);
        disp.setFuncionario(func);
        when(dispositivoRepo.findById(1L)).thenReturn(Optional.of(disp));
        when(funcionarioRepo.getReferenceById(FUNC)).thenReturn(func);

        return token;
    }

    // ── RNE 5: validar un sector NO asignado al funcionario debe rechazarse ───
    @Test
    void validar_sectorNoAsignado_esRechazado() {
        escenarioBase();
        when(asignacionRepo.existsById(any())).thenReturn(false);   // no asignado

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service().validar(FUNC, new ValidacionRequest("QR-OK", 1L)));

        assertTrue(ex.getReason().contains("RNE 5"));
        verify(validacionRepo, never()).saveAndFlush(any());
    }

    // ── RNE 5: validar un sector asignado se registra correctamente ───────────
    @Test
    void validar_sectorAsignado_registraValidacion() {
        escenarioBase();
        when(asignacionRepo.existsById(any())).thenReturn(true);    // asignado
        when(validacionRepo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        ValidacionResponse resp = service().validar(FUNC, new ValidacionRequest("QR-OK", 1L));

        assertEquals("A", resp.letraSector());
        assertEquals(1L, resp.entradaId());
        verify(validacionRepo, times(1)).saveAndFlush(any());
    }

    // ── RNE 8: entrada "al portador" — no se exige que quien valida sea el propietario ──
    @Test
    void validar_alPortador_noVerificaIdentidadDelPropietario() {
        // El propietario de la entrada es user1@, distinto del funcionario que valida (func@).
        // La validación procede igual: basta un token vigente y el sector asignado. La respuesta
        // conserva al propietario original sin exigir que coincida con quien presenta el QR.
        escenarioBase();
        when(asignacionRepo.existsById(any())).thenReturn(true);
        when(validacionRepo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        ValidacionResponse resp = service().validar(FUNC, new ValidacionRequest("QR-OK", 1L));

        assertEquals("user1@test.com", resp.mailPropietario());
        verify(validacionRepo, times(1)).saveAndFlush(any());
    }

    // ── RNE 10: validar con un token vencido (fuera de la ventana de 30s) se rechaza ──
    @Test
    void validar_tokenVencido_esRechazado() {
        TokenQr token = escenarioBase();
        when(tokenService.estaVigente(token)).thenReturn(false);   // token fuera de ventana

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service().validar(FUNC, new ValidacionRequest("QR-OK", 1L)));

        assertEquals(HttpStatus.GONE, ex.getStatusCode());
        verify(validacionRepo, never()).saveAndFlush(any());
    }
}
