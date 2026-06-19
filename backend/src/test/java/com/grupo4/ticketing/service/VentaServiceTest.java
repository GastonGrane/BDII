package com.grupo4.ticketing.service;

import com.grupo4.ticketing.dto.CompraItemRequest;
import com.grupo4.ticketing.dto.CompraRequest;
import com.grupo4.ticketing.dto.CompraResponse;
import com.grupo4.ticketing.entity.*;
import com.grupo4.ticketing.entity.enums.EstadoEntrada;
import com.grupo4.ticketing.entity.enums.EstadoVenta;
import com.grupo4.ticketing.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

// Verifica la decisión funcional: una compra exitosa se guarda como Confirmada
// (no Pendiente) y la entrada queda disponible (Activa + token). Ver README → "Estado de la compra".
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VentaServiceTest {

    @Mock ComisionRepository        comisionRepo;
    @Mock UsuarioGeneralRepository  ugRepo;
    @Mock EventoSectorRepository    esRepo;
    @Mock SectorRepository          sectorRepo;
    @Mock VentaRepository           ventaRepo;
    @Mock EntradaRepository         entradaRepo;
    @Mock TokenService              tokenService;

    @Test
    void compraExitosa_quedaConfirmada_yEntregaEntradaActiva() {
        VentaService service = new VentaService(
                comisionRepo, ugRepo, esRepo, sectorRepo, ventaRepo, entradaRepo, tokenService);

        // ── Datos de apoyo
        Comision comision = new Comision();
        comision.setPorcentaje(new BigDecimal("10"));
        when(comisionRepo.findVigente()).thenReturn(Optional.of(comision));

        UsuarioGeneral comprador = mock(UsuarioGeneral.class);
        when(ugRepo.getReferenceById("user1@test.com")).thenReturn(comprador);

        when(esRepo.findById(any())).thenReturn(Optional.of(mock(EventoSector.class)));

        Sector sector = new Sector();
        sector.setCapacidadMax(100);
        sector.setCostoEntrada(new BigDecimal("50.00"));
        when(sectorRepo.findByIdForUpdate(any())).thenReturn(Optional.of(sector));

        when(entradaRepo.contarPorEventoSector(anyLong(), anyLong(), any())).thenReturn(0L);
        when(ventaRepo.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(entradaRepo.save(any(Entrada.class))).thenAnswer(inv -> inv.getArgument(0));

        // ── Acción: el usuario compra 1 entrada
        CompraRequest req = new CompraRequest(List.of(
                new CompraItemRequest(1L, 1L, "A", 1)));
        CompraResponse resp = service.comprar("user1@test.com", req);

        // ── La compra se crea con estado Confirmada (nunca Pendiente)
        ArgumentCaptor<Venta> ventaCap = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepo).save(ventaCap.capture());
        assertEquals(EstadoVenta.Confirmada, ventaCap.getValue().getEstado());

        // ── La entrada queda disponible: Activa, del comprador y con token QR generado
        ArgumentCaptor<Entrada> entradaCap = ArgumentCaptor.forClass(Entrada.class);
        verify(entradaRepo).save(entradaCap.capture());
        assertEquals(EstadoEntrada.Activa, entradaCap.getValue().getEstadoEntrada());
        assertSame(comprador, entradaCap.getValue().getPropietario());
        verify(tokenService).generarParaEntrada(any(Entrada.class));

        assertNotNull(resp);
    }
}
