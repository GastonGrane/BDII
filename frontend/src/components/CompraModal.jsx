import { useState } from 'react'
import { api } from '../api/client'
import { fmtFecha, fmtMoney } from '../utils/format'

export default function CompraModal({ evento, onClose }) {
  const initQty = Object.fromEntries(evento.sectores.map(s => [s.letraSector, 0]))

  const [quantities, setQuantities] = useState(initQty)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError]           = useState('')
  const [result, setResult]         = useState(null)

  const totalEntradas = Object.values(quantities).reduce((a, b) => a + b, 0)
  const subtotal = evento.sectores.reduce(
    (sum, s) => sum + s.costoEntrada * (quantities[s.letraSector] || 0), 0)

  // adj: incrementa o decrementa la cantidad de un sector sin bajar de 0 (los botones limitan a 5 en total).
  function adj(letra, delta) {
    setQuantities(q => ({ ...q, [letra]: Math.max(0, (q[letra] || 0) + delta) }))
    setError('')
  }

  // confirmar: arma el array de items (filtrando cantidad > 0) y llama a POST /api/ventas.
  // Si la respuesta es OK muestra el resumen; si falla muestra el error del backend.
  async function confirmar() {
    if (totalEntradas === 0) { setError('Seleccioná al menos una entrada.'); return }

    const items = evento.sectores
      .filter(s => quantities[s.letraSector] > 0)
      .map(s => ({
        eventoId:    evento.eventoId,
        estadioId:   evento.estadio.estadioId,
        letraSector: s.letraSector,
        cantidad:    quantities[s.letraSector],
      }))

    setSubmitting(true); setError('')
    try {
      setResult(await api.comprar({ items }))
    } catch (e) {
      setError(e.message)
    } finally {
      setSubmitting(false)
    }
  }

  // backdropClick: cierra el modal al hacer clic en el overlay oscuro (fuera del contenido).
  function backdropClick(e) { if (e.target === e.currentTarget) onClose() }

  return (
    <div className="modal-backdrop" onClick={backdropClick}>
      <div className="modal">

        {result ? (
          /* ── Éxito ── */
          <>
            <div className="modal-header">
              <h2>Compra exitosa</h2>
              <button className="modal-close" onClick={onClose}>✕</button>
            </div>
            <div className="modal-body">
              <div className="success-block">
                <div className="success-icon">✅</div>
                <h3>¡Tus entradas están confirmadas!</h3>
                <p>{evento.equipoLocal} vs {evento.equipoVisitante}</p>
              </div>
              <div className="success-detail">
                <div className="detail-row">
                  <span className="dk">Venta #</span>
                  <span className="dv">{result.ventaId}</span>
                </div>
                <div className="detail-row">
                  <span className="dk">Entradas generadas</span>
                  <span className="dv">{result.entradaIds.length}</span>
                </div>
                <div className="detail-row">
                  <span className="dk">IDs de entrada</span>
                  <span className="dv">{result.entradaIds.join(', ')}</span>
                </div>
                <div className="detail-row highlight">
                  <span className="dk">Total pagado</span>
                  <span className="dv">{fmtMoney(result.montoTotal)}</span>
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-primary" onClick={onClose}>Listo</button>
            </div>
          </>
        ) : (
          /* ── Selección ── */
          <>
            <div className="modal-header">
              <div>
                <h2>{evento.equipoLocal} vs {evento.equipoVisitante}</h2>
                <p className="text-muted text-sm" style={{ marginTop: 4 }}>
                  {evento.estadio.nombre} · {fmtFecha(evento.fechaHora)}
                </p>
              </div>
              <button className="modal-close" onClick={onClose}>✕</button>
            </div>

            <div className="modal-body">
              {error && <div className="alert alert-error">{error}</div>}

              {evento.sectores.map(s => (
                <div key={s.letraSector} className="sector-row">
                  <div className="sector-info">
                    <div className="s-name">Sector {s.letraSector}</div>
                    <div className="s-price">{fmtMoney(s.costoEntrada)} por entrada</div>
                  </div>
                  <div className="qty-control">
                    <button
                      className="qty-btn"
                      onClick={() => adj(s.letraSector, -1)}
                      disabled={!quantities[s.letraSector]}
                    >−</button>
                    <span className="qty-value">{quantities[s.letraSector] || 0}</span>
                    <button
                      className="qty-btn"
                      onClick={() => adj(s.letraSector, +1)}
                      disabled={totalEntradas >= 5}
                    >+</button>
                  </div>
                  <div className="sector-subtotal">
                    {fmtMoney(s.costoEntrada * (quantities[s.letraSector] || 0))}
                  </div>
                </div>
              ))}

              <div className="compra-total">
                <div>
                  <div className="ct-label">
                    {totalEntradas} entrada{totalEntradas !== 1 ? 's' : ''} seleccionada{totalEntradas !== 1 ? 's' : ''}
                    {totalEntradas === 5 && <span style={{ color: 'var(--color-warning)', marginLeft: 6 }}>máx.</span>}
                  </div>
                  <div className="ct-note">Subtotal estimado · precio final incluye comisión</div>
                </div>
                <div className="ct-amount">{fmtMoney(subtotal)}</div>
              </div>
            </div>

            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={onClose} disabled={submitting}>
                Cancelar
              </button>
              <button
                className="btn btn-primary"
                onClick={confirmar}
                disabled={submitting || totalEntradas === 0}
              >
                {submitting ? 'Procesando…' : `Confirmar (${totalEntradas} entrada${totalEntradas !== 1 ? 's' : ''})`}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
