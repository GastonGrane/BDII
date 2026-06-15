import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { fmtFecha, fmtMoney } from '../utils/format'
import CompraModal from '../components/CompraModal'

export default function Eventos() {
  const [eventos, setEventos]   = useState([])
  const [loading, setLoading]   = useState(true)
  const [error, setError]       = useState('')
  // comprando: evento seleccionado por el usuario; su valor no-null abre el CompraModal.
  const [comprando, setComprando] = useState(null)

  // Carga la lista de eventos al montar el componente; el array vacío [] evita re-ejecuciones.
  useEffect(() => {
    api.getEventos()
      .then(setEventos)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return (
    <div className="loading-box">
      <div className="spinner" />
      <span>Cargando eventos…</span>
    </div>
  )

  if (error) return <div className="alert alert-error">{error}</div>

  return (
    <>
      <div className="mb-24">
        <h1 className="page-title">Eventos disponibles</h1>
        <p className="page-subtitle">Seleccioná un partido para ver sectores y comprar entradas</p>
      </div>

      {eventos.length === 0 && (
        <div className="empty-state">
          <p>No hay eventos disponibles en este momento.</p>
        </div>
      )}

      {eventos.map(ev => (
        <div key={ev.eventoId} className="event-card">
          <div className="event-card-header">
            <div>
              <div className="event-teams">{ev.equipoLocal} vs {ev.equipoVisitante}</div>
              <div className="event-meta">
                📅 {fmtFecha(ev.fechaHora)}&nbsp;&nbsp;·&nbsp;&nbsp;
                🏟 {ev.estadio.nombre}, {ev.estadio.ciudad}, {ev.estadio.pais}
              </div>
            </div>
            <button className="btn btn-primary" onClick={() => setComprando(ev)}>
              Comprar entradas
            </button>
          </div>

          <div className="event-card-sectors">
            {ev.sectores.map(s => (
              <div key={s.letraSector} className="sector-chip">
                <div className="sc-label">Sector {s.letraSector}</div>
                <div className="sc-price">{fmtMoney(s.costoEntrada)}</div>
                <div className="sc-cupos">{s.capacidadMax.toLocaleString('es-UY')} cupos</div>
              </div>
            ))}
          </div>
        </div>
      ))}

      {comprando && (
        <CompraModal evento={comprando} onClose={() => setComprando(null)} />
      )}
    </>
  )
}
