import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { fmtFecha, fmtMoney } from '../utils/format'

// Panel de reportes del administrador: eventos más vendidos y ranking de mayores compradores.
export default function AdminReportes() {
  const [eventos, setEventos]         = useState(null)
  const [compradores, setCompradores] = useState(null)
  const [error, setError]             = useState('')

  useEffect(() => {
    Promise.all([api.eventosMasVendidos(), api.rankingCompradores()])
      .then(([ev, comp]) => { setEventos(ev); setCompradores(comp) })
      .catch(e => setError(e.message))
  }, [])

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
    </div>
  )
}
