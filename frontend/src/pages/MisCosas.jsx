import { Fragment, useEffect, useState, useCallback } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { api } from '../api/client'
import { useAuth } from '../contexts/AuthContext'
import { fmtFechaSola, fmtMoney } from '../utils/format'

/* ── helpers de badge ──────────────────────────────────────────────────── */
const BADGE_ENTRADA = {
  Activa:                 { cls: 'badge-green',  label: 'Activa' },
  PendienteTransferencia: { cls: 'badge-yellow', label: 'En transferencia' },
  Consumida:              { cls: 'badge-gray',   label: 'Consumida' },
}
const BADGE_VENTA = {
  Pendiente:  { cls: 'badge-yellow', label: 'Pendiente' },
  Confirmada: { cls: 'badge-green',  label: 'Confirmada' },
  Paga:       { cls: 'badge-blue',   label: 'Pagada' },
}
const BADGE_TRANSF = {
  Pendiente: { cls: 'badge-yellow', label: 'Pendiente' },
  Aceptada:  { cls: 'badge-green',  label: 'Aceptada' },
  Rechazada: { cls: 'badge-red',    label: 'Rechazada' },
}
function Badge({ map, value }) {
  const { cls, label } = map[value] ?? { cls: 'badge-gray', label: value }
  return <span className={`badge ${cls}`}>{label}</span>
}

/* ── tabs config ────────────────────────────────────────────────────────── */
const TABS = [
  { id: 'compras',         to: '/mis-compras',         label: 'Mis compras' },
  { id: 'entradas',        to: '/mis-entradas',         label: 'Mis entradas' },
  { id: 'transferencias',  to: '/mis-transferencias',   label: 'Transferencias' },
]

function activeTab(pathname) {
  if (pathname.startsWith('/mis-entradas'))       return 'entradas'
  if (pathname.startsWith('/mis-transferencias')) return 'transferencias'
  return 'compras'
}

/* ── componente principal ───────────────────────────────────────────────── */
export default function MisCosas() {
  const { user }  = useAuth()
  const { pathname } = useLocation()
  const tab = activeTab(pathname)

  const [compras,        setCompras]        = useState([])
  const [entradas,       setEntradas]       = useState([])
  const [transferencias, setTransferencias] = useState([])
  const [loading, setLoading] = useState(true)
  const [error,   setError]   = useState('')

  /* transfer modal */
  const [transfEntrada,   setTransfEntrada]   = useState(null)
  const [mailDestino,     setMailDestino]     = useState('')
  const [transfLoading,   setTransfLoading]   = useState(false)
  const [transfError,     setTransfError]     = useState('')

  /* qr copy feedback: entradaId | null */
  const [copiedQR, setCopiedQR] = useState(null)

  /* resolver error */
  const [resolverError, setResolverError] = useState('')

  // loadAll: carga las tres secciones en paralelo (Promise.all) y las guarda en estado.
  // Se usa también como recarga post-acción (transferir, aceptar, rechazar).
  const loadAll = useCallback(() => {
    setLoading(true)
    Promise.all([api.misCompras(), api.misEntradas(), api.misTransferencias()])
      .then(([c, e, t]) => { setCompras(c); setEntradas(e); setTransferencias(t) })
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { loadAll() }, [loadAll])

  /* ── copiar QR ── */
  function handleCopyQR(entradaId, codigoQR) {
    navigator.clipboard.writeText(codigoQR).then(() => {
      setCopiedQR(entradaId)
      setTimeout(() => setCopiedQR(null), 2000)
    })
  }

  /* ── crear transferencia ── */
  async function handleTransferir() {
    if (!mailDestino.trim()) { setTransfError('Ingresá el email del destinatario.'); return }
    setTransfLoading(true); setTransfError('')
    try {
      await api.crearTransferencia({ entradaId: transfEntrada.entradaId, mailDestino: mailDestino.trim() })
      setTransfEntrada(null); setMailDestino('')
      loadAll()
    } catch (e) {
      setTransfError(e.message)
    } finally {
      setTransfLoading(false)
    }
  }

  /* ── aceptar / rechazar transferencia ── */
  async function handleResolver(transfId, accion) {
    setResolverError('')
    try {
      await api.resolverTransferencia(transfId, accion)
      loadAll()
    } catch (e) {
      setResolverError(e.message)
    }
  }

  if (loading) return (
    <div className="loading-box"><div className="spinner" /><span>Cargando…</span></div>
  )
  if (error) return <div className="alert alert-error">{error}</div>

  return (
    <>
      {/* ── Mis Compras ── */}
      {tab === 'compras' && (
        <>
          <h2 className="page-title" style={{ fontSize: '1.15rem' }}>Mis compras</h2>
          <p className="page-subtitle">Historial de todas tus compras de entradas</p>
          {compras.length === 0 ? (
            <div className="empty-state"><p>Todavía no hiciste ninguna compra.</p></div>
          ) : (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Fecha</th>
                    <th>Estado</th>
                    <th>Entradas</th>
                    <th style={{ textAlign: 'right' }}>Total</th>
                  </tr>
                </thead>
                <tbody>
                  {compras.map(c => (
                    <tr key={c.ventaId}>
                      <td className="td-mono">{c.ventaId}</td>
                      <td>{fmtFechaSola(c.fecha)}</td>
                      <td><Badge map={BADGE_VENTA} value={c.estado} /></td>
                      <td>{c.cantidadEntradas}</td>
                      <td style={{ textAlign: 'right', fontWeight: 600 }}>{fmtMoney(c.montoTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {/* ── Mis Entradas ── */}
      {tab === 'entradas' && (
        <>
          <h2 className="page-title" style={{ fontSize: '1.15rem' }}>Mis entradas</h2>
          <p className="page-subtitle">Entradas que tenés en tu poder actualmente</p>
          {entradas.length === 0 ? (
            <div className="empty-state"><p>No tenés entradas.</p></div>
          ) : (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Partido</th>
                    <th>Sector</th>
                    <th>Estado</th>
                    <th style={{ textAlign: 'right' }}>Costo</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {entradas.map(e => (
                    <Fragment key={e.entradaId}>
                      <tr>
                        <td className="td-mono">{e.entradaId}</td>
                        <td>
                          <div style={{ fontWeight: 600 }}>{e.equipoLocal} vs {e.equipoVisitante}</div>
                          <div className="text-muted text-sm">{fmtFechaSola(e.fechaHoraEvento)}</div>
                        </td>
                        <td><span className="badge badge-blue">Sector {e.letraSector}</span></td>
                        <td><Badge map={BADGE_ENTRADA} value={e.estadoEntrada} /></td>
                        <td style={{ textAlign: 'right' }}>{fmtMoney(e.costoHistorico)}</td>
                        <td style={{ minWidth: 110 }}>
                          {e.estadoEntrada === 'Activa' && (
                            <button
                              className="btn btn-secondary btn-sm"
                              onClick={() => { setTransfEntrada(e); setMailDestino(''); setTransfError('') }}
                            >
                              Transferir
                            </button>
                          )}
                        </td>
                      </tr>
                      {e.estadoEntrada === 'Activa' && e.codigoQR && (
                        <tr style={{ background: 'var(--color-accent-light)' }}>
                          <td colSpan={6} style={{ padding: '6px 16px 10px', borderBottom: '1px solid var(--color-accent-muted)' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                              <span style={{ fontSize: '.75rem', fontFamily: 'var(--font-heading)', fontWeight: 700, color: 'var(--color-accent-dark)', letterSpacing: '.5px', textTransform: 'uppercase' }}>
                                Código QR
                              </span>
                              <code style={{
                                fontFamily: 'var(--font-mono)', fontSize: '.82rem',
                                background: 'rgba(0,0,0,.06)', padding: '3px 10px',
                                borderRadius: 'var(--radius-sm)', letterSpacing: '.5px',
                                userSelect: 'all', color: 'var(--color-text)',
                              }}>
                                {e.codigoQR}
                              </code>
                              <button
                                className="btn btn-sm"
                                style={{ background: copiedQR === e.entradaId ? 'var(--color-success)' : 'var(--color-accent)', color: '#fff', minWidth: 80 }}
                                onClick={() => handleCopyQR(e.entradaId, e.codigoQR)}
                              >
                                {copiedQR === e.entradaId ? '✓ Copiado' : 'Copiar'}
                              </button>
                            </div>
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {/* ── Mis Transferencias ── */}
      {tab === 'transferencias' && (
        <>
          <h2 className="page-title" style={{ fontSize: '1.15rem' }}>Transferencias</h2>
          <p className="page-subtitle">Historial de transferencias en las que participás</p>

          {resolverError && (
            <div className="alert alert-error" style={{ marginBottom: 16 }}>{resolverError}</div>
          )}

          {transferencias.length === 0 ? (
            <div className="empty-state"><p>No tenés transferencias registradas.</p></div>
          ) : (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Entrada</th>
                    <th>Tu rol</th>
                    <th>Otro usuario</th>
                    <th>Estado</th>
                    <th>Fecha solicitud</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {transferencias.map(t => {
                    const canResolve = t.rol === 'DESTINO' && t.estado === 'Pendiente'
                    return (
                      <tr key={t.transfId}>
                        <td className="td-mono">{t.transfId}</td>
                        <td className="td-mono">#{t.entradaId}</td>
                        <td>
                          <span className={`badge ${t.rol === 'ORIGEN' ? 'badge-blue' : 'badge-green'}`}>
                            {t.rol === 'ORIGEN' ? 'Enviaste' : 'Recibiste'}
                          </span>
                        </td>
                        <td className="text-sm">{t.otroUsuario}</td>
                        <td><Badge map={BADGE_TRANSF} value={t.estado} /></td>
                        <td className="text-sm">{fmtFechaSola(t.fechaSol)}</td>
                        <td style={{ minWidth: 170 }}>
                          {canResolve && (
                            <div className="btn-group">
                              <button
                                className="btn btn-success btn-sm"
                                onClick={() => handleResolver(t.transfId, 'ACEPTAR')}
                              >
                                Aceptar
                              </button>
                              <button
                                className="btn btn-danger btn-sm"
                                onClick={() => handleResolver(t.transfId, 'RECHAZAR')}
                              >
                                Rechazar
                              </button>
                            </div>
                          )}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {/* ── Modal: Transferir entrada ── */}
      {transfEntrada && (
        <div className="modal-backdrop" onClick={e => e.target === e.currentTarget && setTransfEntrada(null)}>
          <div className="modal">
            <div className="modal-header">
              <div>
                <h2>Transferir entrada #{transfEntrada.entradaId}</h2>
                <p className="text-muted text-sm" style={{ marginTop: 4 }}>
                  Sector {transfEntrada.letraSector} · {transfEntrada.equipoLocal} vs {transfEntrada.equipoVisitante}
                </p>
              </div>
              <button className="modal-close" onClick={() => setTransfEntrada(null)}>✕</button>
            </div>
            <div className="modal-body">
              {transfError && <div className="alert alert-error">{transfError}</div>}
              <div className="form-group">
                <label htmlFor="mailDestino">Email del destinatario</label>
                <input
                  id="mailDestino"
                  type="email"
                  placeholder="destinatario@email.com"
                  value={mailDestino}
                  onChange={e => { setMailDestino(e.target.value); setTransfError('') }}
                  autoFocus
                />
              </div>
              <p className="text-muted text-sm">
                El destinatario deberá aceptar la transferencia desde su cuenta.
                La entrada quedará en estado "En transferencia" hasta que responda.
              </p>
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setTransfEntrada(null)} disabled={transfLoading}>
                Cancelar
              </button>
              <button className="btn btn-primary" onClick={handleTransferir} disabled={transfLoading}>
                {transfLoading ? 'Enviando…' : 'Enviar transferencia'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
