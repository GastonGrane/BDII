package com.grupo4.ticketing.service;

import com.grupo4.ticketing.dto.EstadioDetalleResponse;
import com.grupo4.ticketing.dto.EstadioRequest;
import com.grupo4.ticketing.dto.EventoRequest;
import com.grupo4.ticketing.dto.SectorRequest;
import com.grupo4.ticketing.entity.*;
import com.grupo4.ticketing.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// RNE 3 — Sectores habilitados por evento:
//   • un evento no puede crearse sin sectores habilitados;
//   • no se puede habilitar un sector que no pertenece al estadio del evento;
//   • el alta de estadio exige al menos un sector.
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServiceTest {

    @Mock EstadioRepository       estadioRepo;
    @Mock SectorRepository        sectorRepo;
    @Mock EventoRepository        eventoRepo;
    @Mock EventoSectorRepository  eventoSectorRepo;
    @Mock AdministradorRepository adminRepo;

    private static final String ADMIN = "admin@ticketing.com";

    private AdminService service() {
        return new AdminService(estadioRepo, sectorRepo, eventoRepo, eventoSectorRepo, adminRepo);
    }

    private void adminEnUruguay() {
        Administrador a = new Administrador();
        a.setMailUsuario(ADMIN);
        a.setPaisSede("Uruguay");
        when(adminRepo.findById(ADMIN)).thenReturn(Optional.of(a));
        when(adminRepo.getReferenceById(ADMIN)).thenReturn(a);
    }

    private Estadio estadioUruguay(long id) {
        Estadio e = new Estadio();
        e.setEstadioId(id);
        e.setNombre("Centenario");
        e.setPais("Uruguay");
        e.setCiudad("Montevideo");
        return e;
    }

    // ── RNE 3: no permitir crear evento sin sectores habilitados ──────────────
    @Test
    void crearEvento_sinSectores_esRechazado() {
        EventoRequest req = new EventoRequest(
                "Uruguay", "Brasil", LocalDateTime.now(), 1L, List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service().crearEvento(ADMIN, req));
        assertTrue(ex.getMessage().toLowerCase().contains("sector"));
        verify(eventoRepo, never()).saveAndFlush(any());
    }

    // ── RNE 3: no permitir habilitar un sector que no pertenece al estadio ────
    @Test
    void crearEvento_conSectorInexistenteEnEstadio_esRechazado() {
        adminEnUruguay();
        when(estadioRepo.findById(1L)).thenReturn(Optional.of(estadioUruguay(1L)));
        when(eventoRepo.saveAndFlush(any(Evento.class))).thenAnswer(inv -> {
            Evento e = inv.getArgument(0); e.setEventoId(10L); return e;
        });
        // El sector C NO existe en el estadio
        when(sectorRepo.existsById(any())).thenReturn(false);

        EventoRequest req = new EventoRequest(
                "Uruguay", "Brasil", LocalDateTime.now(), 1L, List.of("C"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service().crearEvento(ADMIN, req));
        assertTrue(ex.getMessage().contains("no existe"));
        verify(eventoSectorRepo, never()).save(any());
    }

    // ── RNE 3: crear evento con al menos un sector válido del estadio ─────────
    @Test
    void crearEvento_conSectorValido_creaEventoSector() {
        adminEnUruguay();
        when(estadioRepo.findById(1L)).thenReturn(Optional.of(estadioUruguay(1L)));
        when(eventoRepo.saveAndFlush(any(Evento.class))).thenAnswer(inv -> {
            Evento e = inv.getArgument(0); e.setEventoId(10L); return e;
        });
        when(sectorRepo.existsById(any())).thenReturn(true);
        when(sectorRepo.getReferenceById(any())).thenReturn(new Sector());
        when(eventoSectorRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventoRequest req = new EventoRequest(
                "Uruguay", "Brasil", LocalDateTime.now(), 1L, List.of("A"));

        var resp = service().crearEvento(ADMIN, req);

        assertEquals(10L, resp.eventoId());
        verify(eventoSectorRepo, times(1)).save(any());
    }

    // ── RNE 3: el alta de estadio exige al menos un sector ────────────────────
    @Test
    void crearEstadio_sinSectores_esRechazado() {
        EstadioRequest req = new EstadioRequest("Centenario", "Uruguay", "Montevideo", List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service().crearEstadio(ADMIN, req));
        assertTrue(ex.getMessage().toLowerCase().contains("al menos un sector"));
        verify(estadioRepo, never()).save(any());
    }

    // ── RNE 3: alta de estadio con un sector válido ───────────────────────────
    @Test
    void crearEstadio_conUnSector_persisteEstadioYSector() {
        adminEnUruguay();
        when(estadioRepo.save(any(Estadio.class))).thenAnswer(inv -> {
            Estadio e = inv.getArgument(0); e.setEstadioId(5L); return e;
        });
        when(sectorRepo.existsById(any())).thenReturn(false);
        when(sectorRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EstadioRequest req = new EstadioRequest("Centenario", "Uruguay", "Montevideo",
                List.of(new SectorRequest("A", 1000, new BigDecimal("100.00"))));

        EstadioDetalleResponse resp = service().crearEstadio(ADMIN, req);

        assertEquals(5L, resp.estadioId());
        assertEquals(1, resp.sectores().size());
        assertEquals("A", resp.sectores().get(0).letraSector());
        verify(sectorRepo, times(1)).save(any());
    }
}
