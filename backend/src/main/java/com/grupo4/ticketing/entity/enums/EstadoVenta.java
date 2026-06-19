package com.grupo4.ticketing.entity.enums;

// Estados posibles de una VENTA.
//   Confirmada → estado de toda compra exitosa: la entrada se entrega en el acto (ver VentaService.comprar).
//   Pendiente  → RESERVADO para futuros flujos con pago externo / validación manual / confirmación diferida.
//                Se mantiene por compatibilidad y documentación; hoy no es el estado de una compra finalizada.
//   Paga       → reservado para un eventual registro de pago externo.
public enum EstadoVenta {
    Pendiente, Confirmada, Paga
}
