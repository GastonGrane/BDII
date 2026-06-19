import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { fmtFecha, fmtMoney } from '../utils/format'

// Panel de reportes del administrador: eventos más vendidos, ranking de mayores compradores
// y cobertura de sectores por funcionario (RNE 5).
export default function AdminReportes() {
  const [eventos, setEventos]         = useState(null)
  const [compradores, setCompradores] = useState(null)
  const [error, setError]             = useState('')

  /* ── cobertura RNE 5 ── */
  const [cobEventoId, setCobEventoId] = useState('')
  const [cobEstado,   setCobEstado]   = useState(null)
  const [cobError,    setCobError]    = useState('')

  useEffect(() => {
    Promise.all([api.eventosMasVendidos(), api.rankingCompradores()])
      .then(([ev, comp]) => { setEventos(ev); setCompradores(comp) })
      .catch(e => setError(e.message))
  }, [])

  function verCobertura(eventoId) {
    setCobEventoId(eventoId)
    setCobEstado(null); setCobError('')
    if (!eventoId) return
    api.coberturaEstado(Number(eventoId))
      .then(setCobEstado)
      .catch(e => setCobError(e.message))
  }

  if (error)   return <div className="alert alert-error">{error}</div>
  if (!eventos || !compradores) return <p className="text-muted">Cargando reportes…</p>

  return (
    <div>
      <h1>Reportes</h1>
      <p className="subtitle">Estadísticas de ventas del Mundial 2026</p>

      <div className="card" style={{ marginBottom: 24 }}>
        <h2>Eventos con más entradas vendidas</h2>
        <div className="table-wrapper">
        <table>
          <thead>
            <tr><th>#</th><th>Evento</th><th>Fecha</th><th>Entradas vendidas</th></tr>
          </thead>
          <tbody>
            {eventos.map((e, i) => (
              <tr key={e.eventoId}>
                <td>{i + 1}</td>
                <td>{e.equipoLocal} vs {e.equipoVisitante}</td>
                <td>{fmtFecha(e.fechaHora)}</td>
                <td>{e.entradasVendidas}</td>
              </tr>
            ))}
            {eventos.length === 0 && <tr><td colSpan="4" className="text-muted">Sin datos</td></tr>}
          </tbody>
        </table>
        </div>
      </div>

      <div className="card">
        <h2>Ranking de mayores compradores</h2>
        <div className="table-wrapper">
        <table>
          <thead>
            <tr><th>#</th><th>Usuario</th><th>Entradas</th><th>Monto gastado</th></tr>
          </thead>
          <tbody>
            {compradores.map((c, i) => (
              <tr key={c.mail}>
                <td>{i + 1}</td>
                <td>{c.mail}</td>
                <td>{c.cantidadEntradas}</td>
                <td>{fmtMoney(c.montoGastado)}</td>
              </tr>
            ))}
            {compradores.length === 0 && <tr><td colSpan="4" className="text-muted">Sin datos</td></tr>}
          </tbody>
        </table>
        </div>
        <p className="text-muted text-sm" style={{ marginTop: 8 }}>
          Monto gastado: suma de los costos de las entradas (sin comisión).
        </p>
      </div>

      {/* ── Cobertura de sectores por funcionario (RNE 5) ── */}
      <div className="card" style={{ marginTop: 24 }}>
        <h2>Cobertura de sectores (RNE 5)</h2>
        <p className="subtitle">
          Verifica si cada funcionario validó al menos una entrada en todos los sectores que tiene asignados.
        </p>

        <div className="form-group" style={{ maxWidth: 420 }}>
          <label htmlFor="cob-evento">Evento</label>
          <select id="cob-evento" value={cobEventoId} onChange={e => verCobertura(e.target.value)}>
            <option value="">— Seleccioná un evento —</option>
            {eventos.map(e => (
              <option key={e.eventoId} value={e.eventoId}>
                #{e.eventoId} — {e.equipoLocal} vs {e.equipoVisitante}
              </option>
            ))}
          </select>
        </div>

        {cobError && <div className="alert alert-error">{cobError}</div>}

        {cobEstado && (
          cobEstado.cumple ? (
            <div className="alert alert-success">
              ✅ El evento #{cobEstado.eventoId} cumple RNE 5: todos los sectores asignados fueron validados.
            </div>
          ) : (
            <>
              <div className="alert alert-error">
                ⛔ El evento #{cobEstado.eventoId} NO cumple RNE 5: quedan sectores asignados sin validación.
              </div>
              <div className="table-wrapper">
                <table>
                  <thead>
                    <tr><th>Funcionario</th><th>Estadio</th><th>Sector pendiente</th></tr>
                  </thead>
                  <tbody>
                    {cobEstado.pendientes.map((p, i) => (
                      <tr key={i}>
                        <td>{p.mailFuncionario}</td>
                        <td>{p.estadioId}</td>
                        <td><span className="badge badge-blue">Sector {p.letraSector}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )
        )}
      </div>
    </div>
  )
}
