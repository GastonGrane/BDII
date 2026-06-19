package com.grupo4.ticketing.service;

import com.grupo4.ticketing.dto.CompraItemRequest;
import com.grupo4.ticketing.dto.CompraRequest;
import com.grupo4.ticketing.dto.CompraResponse;
import com.grupo4.ticketing.dto.MisComprasItemResponse;
import com.grupo4.ticketing.entity.*;
import com.grupo4.ticketing.entity.enums.EstadoEntrada;
import com.grupo4.ticketing.entity.enums.EstadoVenta;
import com.grupo4.ticketing.entity.enums.LetraSector;
import com.grupo4.ticketing.repository.*;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.grupo4.ticketing.util.SessionUtils.extractDbMessage;

// Gestión del flujo de compra: valida RNE 1 (máx. 5 entradas) y RNE 3 (aforo), crea
// VENTA + ENTRADAs + TOKEN_QR en una sola transacción y calcula MontoTotal con comisión vigente (DEC-03).
@Service
public class VentaService {

    private final ComisionRepository    comisionRepo;
    private final UsuarioGeneralRepository ugRepo;
    private final EventoSectorRepository   esRepo;
    private final SectorRepository         sectorRepo;
    private final VentaRepository          ventaRepo;
    private final EntradaRepository        entradaRepo;
    private final TokenService             tokenService;

    public VentaService(ComisionRepository comisionRepo,
                        UsuarioGeneralRepository ugRepo,
                        EventoSectorRepository esRepo,
                        SectorRepository sectorRepo,
                        VentaRepository ventaRepo,
                        EntradaRepository entradaRepo,
                        TokenService tokenService) {
        this.comisionRepo = comisionRepo;
        this.ugRepo       = ugRepo;
        this.esRepo       = esRepo;
        this.sectorRepo   = sectorRepo;
        this.ventaRepo    = ventaRepo;
        this.entradaRepo  = entradaRepo;
        this.tokenService = tokenService;
    }

    // Procesa la compra completa: valida items, crea VENTA, genera una ENTRADA y un TOKEN_QR por cada ticket.
    // MontoTotal = subtotal × (1 + porcentaje/100) calculado en Java (no en la vista de BD).
    @Transactional
    public CompraResponse comprar(String mailComprador, CompraRequest req) {

        // ── Pre-validación en backend (devuelve 400 antes de tocar la BD)
        if (req.items() == null || req.items().isEmpty()) {
            throw new IllegalArgumentException("El pedido debe incluir al menos un ítem");
        }
        int totalEntradas = req.items().stream()
                .mapToInt(CompraItemRequest::cantidad)
                .sum();
        if (totalEntradas < 1) {
            throw new IllegalArgumentException("La cantidad total de entradas debe ser mayor a 0");
        }
        if (totalEntradas > 5) {
            throw new IllegalArgumentException(
                "RNE 1: una venta no puede contener más de 5 entradas (pedido: " + totalEntradas + ")");
        }

        // ── Comisión vigente
        Comision comision = comisionRepo.findVigente()
                .orElseThrow(() -> new IllegalStateException("No hay comisión vigente configurada"));

        // ── Referencia al comprador (ya existe — validado por sesión)
        UsuarioGeneral comprador = ugRepo.getReferenceById(mailComprador);

        // ── Crear VENTA
        // Sin pasarela de pago: la compra se confirma de inmediato al procesarse
        // (la entrada se entrega en el acto con su TOKEN_QR), por eso se salta el
        // estado Pendiente. El dominio {Pendiente, Confirmada, Paga} se mantiene en
        // el enum/schema por completitud respecto a la letra; el flujo solo usa Confirmada.
        Venta venta = new Venta();
        venta.setFecha(LocalDateTime.now());
        venta.setEstado(EstadoVenta.Confirmada);
        venta.setComprador(comprador);
        venta.setComision(comision);
        venta = ventaRepo.save(venta);

        // ── Crear ENTRADAs + TOKEN_QR
        List<Long> entradaIds = new ArrayList<>();
        BigDecimal subtotal   = BigDecimal.ZERO;
        // Reserva acumulada dentro de esta misma compra, por (evento, sector),
        // para no superar el aforo cuando varios ítems apuntan al mismo sector.
        Map<EventoSectorId, Integer> reservadas = new HashMap<>();

        try {
            for (CompraItemRequest item : req.items()) {
                if (item.cantidad() == null || item.cantidad() < 1) {
                    throw new IllegalArgumentException("La cantidad de cada ítem debe ser mayor a 0");
                }

                LetraSector letra = parseSector(item.letraSector());

                // Validar que el sector esté habilitado para el evento
                EventoSectorId esId = new EventoSectorId(item.eventoId(), item.estadioId(), letra);
                EventoSector eventoSector = esRepo.findById(esId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "El sector " + item.letraSector() + " no está habilitado "
                                + "para el evento " + item.eventoId()));

                // Lock pesimista del sector: serializa compras concurrentes del mismo sector (RNE 3)
                SectorId sId = new SectorId(item.estadioId(), letra);
                Sector sector = sectorRepo.findByIdForUpdate(sId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Sector " + item.letraSector() + " no encontrado en estadio " + item.estadioId()));

                // RNE 3: control de aforo. emitidas (en BD) + ya reservadas en esta compra + pedido <= capacidad
                long emitidas  = entradaRepo.contarPorEventoSector(item.eventoId(), item.estadioId(), letra);
                int yaReservadas = reservadas.getOrDefault(esId, 0);
                if (emitidas + yaReservadas + item.cantidad() > sector.getCapacidadMax()) {
                    long disponibles = sector.getCapacidadMax() - emitidas - yaReservadas;
                    throw new IllegalArgumentException(
                            "RNE 3: capacidad insuficiente en el sector " + letra + " del evento " + item.eventoId()
                            + " (disponibles: " + Math.max(disponibles, 0) + ", pedidas: " + item.cantidad() + ")");
                }
                reservadas.merge(esId, item.cantidad(), Integer::sum);

                for (int i = 0; i < item.cantidad(); i++) {
                    Entrada entrada = new Entrada();
                    entrada.setEstadoEntrada(EstadoEntrada.Activa);
                    entrada.setCostoHistorico(sector.getCostoEntrada());
                    entrada.setVenta(venta);
                    entrada.setEventoSector(eventoSector);
                    entrada.setPropietario(comprador);
                    entrada = entradaRepo.save(entrada);
                    entradaIds.add(entrada.getEntradaId());
                    subtotal = subtotal.add(sector.getCostoEntrada());

                    // Primer token dinámico de la entrada (vigente 30s — RNE 10)
                    tokenService.generarParaEntrada(entrada);
                }
            }
        } catch (DataAccessException e) {
            // Trigger (RNE 1 / RNE 3) rechazó el INSERT — propagar mensaje legible
            throw new IllegalArgumentException(extractDbMessage(e));
        }

        // ── MontoTotal calculado en Java (sin depender de la vista — DEC-03/DEC-08)
        BigDecimal factor = BigDecimal.ONE.add(
                comision.getPorcentaje().divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP));
        BigDecimal montoTotal = subtotal.multiply(factor).setScale(2, RoundingMode.HALF_UP);

        return new CompraResponse(venta.getVentaId(), entradaIds, montoTotal);
    }

    // Devuelve el historial de compras del usuario con MontoTotal calculado desde la vista v_monto_total_venta.
    @Transactional(readOnly = true)
    public List<MisComprasItemResponse> misCompras(String mail) {
        return ventaRepo.findByCompradorMailUsuario(mail).stream()
                .map(v -> {
                    BigDecimal monto = ventaRepo.calcularMontoTotal(v.getVentaId());
                    if (monto != null) monto = monto.setScale(2, RoundingMode.HALF_UP);
                    long cantidad = entradaRepo.countByVentaVentaId(v.getVentaId());
                    return new MisComprasItemResponse(
                            v.getVentaId(),
                            v.getFecha(),
                            v.getEstado().name(),
                            monto,
                            cantidad
                    );
                })
                .toList();
    }

    // Convierte un string "A"/"B"/"C"/"D" al enum LetraSector; lanza error descriptivo si no es válido.
    private LetraSector parseSector(String s) {
        try {
            return LetraSector.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Letra de sector inválida: '" + s + "'. Valores: A, B, C, D");
        }
    }
}
