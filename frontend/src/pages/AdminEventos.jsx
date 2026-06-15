import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { fmtFecha, fmtMoney } from '../utils/format'

const FORM_EMPTY = {
  equipoLocal: '', equipoVisitante: '', fechaHora: '', estadioId: '', sectores: [],
}

export default function AdminEventos() {
  const [eventos,    setEventos]    = useState([])
  const [estadios,   setEstadios]   = useState([])
  const [loading,    setLoading]    = useState(true)
  const [loadError,  setLoadError]  = useState('')

  const [form,       setForm]       = useState(FORM_EMPTY)
  const [error,      setError]      = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    Promise.all([api.getEventos(), api.getEstadios()])
      .then(([evs, ests]) => { setEventos(evs); setEstadios(ests) })
      .catch(e => setLoadError(e.message))
      .finally(() => setLoading(false))
  }, [])

  // estadioSeleccionado: objeto completo del estadio elegido (incluye sus sectores configurados).
  // sectoresDisponibles: array de sectores para mostrar los checkboxes; vacío si no hay estadio seleccionado.
  const estadioSeleccionado = estadios.find(e => e.estadioId === Number(form.estadioId))
  const sectoresDisponibles = estadioSeleccionado?.sectores ?? []

  // toggleSector: agrega o quita una letra del array form.sectores (selección múltiple de sectores habilitados).
  function toggleSector(letra) {
    setForm(f => ({
      ...f,
      sectores: f.sectores.includes(letra)
        ? f.sectores.filter(s => s !== letra)
        : [...f.sectores, letra],
    }))
  }

  async function handleSubmit(ev) {
    ev.preventDefault()
    if (form.sectores.length === 0) { setError('Seleccioná al menos un sector.'); return }
    setError(''); setSubmitting(true)
    try {
      await api.crearEvento({
        equipoLocal:     form.equipoLocal.trim(),
        equipoVisitante: form.equipoVisitante.trim(),
        // datetime-local devuelve "YYYY-MM-DDTHH:MM"; Spring necesita segundos → agregamos ':00'
        fechaHora:       form.fechaHora + ':00',
        estadioId:       Number(form.estadioId),
        sectores:        form.sectores,
      })
      // Recargar la lista completa para obtener datos del estadio incluidos
      const evs = await api.getEventos()
      setEventos(evs)
      setForm(FORM_EMPTY)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return (
    <div className="loading-box"><div className="spinner" /><span>Cargando eventos…</span></div>
  )
  if (loadError) return <div className="alert alert-error">{loadError}</div>

  const isRne4 = error.includes('RNE 4')

  return (
    <>
      <div className="mb-24">
        <h1 className="page-title">Eventos</h1>
        <p className="page-subtitle">Alta de partidos del Mundial 2026 y asignación de sectores</p>
      </div>

      {/* ── Form nuevo evento ── */}
      <div className="card" style={{ padding: '22px 28px', marginBottom: 32 }}>
        <h2 style={{ marginBottom: 14 }}>Nuevo evento</h2>

        {error && (
          <div className="alert alert-error" role="alert">
            {isRne4 && <><strong>Solapamiento de horario (RNE 4)</strong><br /></>}
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-row" style={{ marginBottom: 14 }}>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label htmlFor="ev-local">Equipo local</label>
              <input id="ev-local" type="text" placeholder="Uruguay"
                value={form.equipoLocal}
                onChange={e => setForm(f => ({ ...f, equipoLocal: e.target.value }))}
                disabled={submitting} required />
            </div>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label htmlFor="ev-visitante">Equipo visitante</label>
              <input id="ev-visitante" type="text" placeholder="Argentina"
                value={form.equipoVisitante}
                onChange={e => setForm(f => ({ ...f, equipoVisitante: e.target.value }))}
                disabled={submitting} required />
            </div>
          </div>

          <div className="form-row" style={{ marginBottom: 14 }}>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label htmlFor="ev-fecha">Fecha y hora</label>
              <input id="ev-fecha" type="datetime-local"
                value={form.fechaHora}
                onChange={e => setForm(f => ({ ...f, fechaHora: e.target.value }))}
                disabled={submitting} required />
            </div>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label htmlFor="ev-estadio">Estadio</label>
              <select id="ev-estadio"
                value={form.estadioId}
                onChange={e => setForm(f => ({ ...f, estadioId: e.target.value, sectores: [] }))}
                disabled={submitting || estadios.length === 0} required>
                <option value="">— Seleccioná un estadio —</option>
                {estadios.map(est => (
                  <option key={est.estadioId} value={est.estadioId}>
                    {est.nombre} — {est.ciudad}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Checkboxes de sectores */}
          {form.estadioId && (
            <div className="form-group" style={{ marginBottom: 18 }}>
              <label>Sectores habilitados</label>
              {sectoresDisponibles.length === 0 ? (
                <p className="text-muted text-sm" style={{ paddingTop: 4 }}>
                  El estadio seleccionado no tiene sectores configurados. Creálos primero en la pantalla de Estadios.
                </p>
              ) : (
                <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', paddingTop: 6 }}>
                  {sectoresDisponibles.map(s => {
                    const checked = form.sectores.includes(s.letraSector)
                    return (
                      <label key={s.letraSector} style={{
                        display: 'flex', alignItems: 'center', gap: 8,
                        padding: '8px 16px',
                        border: `2px solid ${checked ? 'var(--color-primary)' : 'var(--color-border)'}`,
                        borderRadius: 'var(--radius-md)',
                        background: checked ? 'var(--color-primary-light)' : 'var(--color-surface)',
                        cursor: submitting ? 'not-allowed' : 'pointer',
                        transition: 'border-color var(--transition), background var(--transition)',
                        userSelect: 'none',
                      }}>
                        <input type="checkbox"
                          checked={checked}
                          onChange={() => !submitting && toggleSector(s.letraSector)}
                          style={{ width: 'auto', accentColor: 'var(--color-primary)' }}
                        />
                        <span style={{ fontFamily: 'var(--font-heading)', fontWeight: 700, fontSize: '.95rem' }}>
                          Sector {s.letraSector}
                        </span>
                        <span className="text-muted" style={{ fontSize: '.8rem' }}>
                          {fmtMoney(s.costoEntrada)}
                        </span>
                      </label>
                    )
                  })}
                </div>
              )}
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <button type="submit" className="btn btn-primary"
              disabled={submitting || estadios.length === 0}>
              {submitting ? 'Creando…' : '+ Crear evento'}
            </button>
          </div>
        </form>
      </div>

      {/* ── Lista de eventos ── */}
      <div className="mb-24">
        <h2 style={{ marginBottom: 4 }}>Eventos registrados</h2>
        <p className="text-muted text-sm">{eventos.length} evento{eventos.length !== 1 ? 's' : ''} en total</p>
      </div>

      {eventos.length === 0 ? (
        <div className="empty-state"><p>No hay eventos registrados aún.</p></div>
      ) : eventos.map(ev => (
        <div key={ev.eventoId} className="event-card">
          <div className="event-card-header">
            <div>
              <div className="event-teams">{ev.equipoLocal} vs {ev.equipoVisitante}</div>
              <div className="event-meta">
                📅 {fmtFecha(ev.fechaHora)}&nbsp;&nbsp;·&nbsp;&nbsp;
                🏟 {ev.estadio.nombre}, {ev.estadio.ciudad}, {ev.estadio.pais}&nbsp;&nbsp;·&nbsp;&nbsp;
                <span className="td-mono" style={{ fontFamily: 'var(--font-mono)', fontSize: '.78rem' }}>
                  #{ev.eventoId}
                </span>
              </div>
            </div>
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
    </>
  )
}
