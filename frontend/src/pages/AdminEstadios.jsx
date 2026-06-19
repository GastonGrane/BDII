import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { fmtMoney } from '../utils/format'

const LETRAS = ['A', 'B', 'C', 'D']
const SECTOR_EMPTY = { letraSector: '', capacidadMax: '', costoEntrada: '' }

export default function AdminEstadios() {
  const [estadios,      setEstadios]      = useState([])
  const [loading,       setLoading]       = useState(true)
  const [loadError,     setLoadError]     = useState('')

  // Form nuevo estadio. RNE 3: el alta exige al menos un sector, por eso arranca con una fila.
  const [eForm,         setEForm]         = useState({ nombre: '', pais: '', ciudad: '' })
  const [eSectores,     setESectores]     = useState([{ ...SECTOR_EMPTY }])
  const [eError,        setEError]        = useState('')
  const [eSubmitting,   setESubmitting]   = useState(false)

  // sectorForms/Errors/Pending: estado aislado por estadioId para que cada card funcione de forma independiente.
  // Usar un objeto keyed (no un array) permite actualizar solo el estadio afectado sin re-renderizar los demás.
  const [sectorForms,   setSectorForms]   = useState({})
  const [sectorErrors,  setSectorErrors]  = useState({})
  const [sectorPending, setSectorPending] = useState({})

  useEffect(() => {
    api.getEstadios()
      .then(data => {
        setEstadios(data)
        const forms = {}
        data.forEach(e => { forms[e.estadioId] = { ...SECTOR_EMPTY } })
        setSectorForms(forms)
      })
      .catch(e => setLoadError(e.message))
      .finally(() => setLoading(false))
  }, [])

  /* ── Sectores del nuevo estadio (RNE 3) ── */
  // Letras ya elegidas en otras filas, para no permitir repetir un sector en el mismo estadio.
  function letrasTomadasEnAlta(exceptoIdx) {
    return eSectores.filter((_, i) => i !== exceptoIdx).map(s => s.letraSector).filter(Boolean)
  }
  function setNuevoSectorField(idx, field, value) {
    setESectores(prev => prev.map((s, i) => (i === idx ? { ...s, [field]: value } : s)))
    setEError('')
  }
  function agregarFilaSector() {
    if (eSectores.length >= LETRAS.length) return
    setESectores(prev => [...prev, { ...SECTOR_EMPTY }])
  }
  function quitarFilaSector(idx) {
    setESectores(prev => (prev.length === 1 ? prev : prev.filter((_, i) => i !== idx)))
  }

  /* ── Crear estadio ── */
  async function handleCrearEstadio(ev) {
    ev.preventDefault()
    const sectores = eSectores.filter(s => s.letraSector && s.capacidadMax && s.costoEntrada)
    if (sectores.length === 0) {
      setEError('RNE 3: agregá al menos un sector con letra, capacidad y costo.')
      return
    }
    setEError(''); setESubmitting(true)
    try {
      const nuevo = await api.crearEstadio({
        ...eForm,
        sectores: sectores.map(s => ({
          letraSector:  s.letraSector,
          capacidadMax: Number(s.capacidadMax),
          costoEntrada: Number(s.costoEntrada),
        })),
      })
      setEstadios(prev => [...prev, nuevo])
      setSectorForms(prev => ({ ...prev, [nuevo.estadioId]: { ...SECTOR_EMPTY } }))
      setEForm({ nombre: '', pais: '', ciudad: '' })
      setESectores([{ ...SECTOR_EMPTY }])
    } catch (err) {
      setEError(err.message)
    } finally {
      setESubmitting(false)
    }
  }

  /* ── Crear sector ── */
  async function handleCrearSector(estadioId) {
    const sf = sectorForms[estadioId]
    setSectorErrors(prev => ({ ...prev, [estadioId]: '' }))
    setSectorPending(prev => ({ ...prev, [estadioId]: true }))
    try {
      const nuevo = await api.crearSector(estadioId, {
        letraSector:  sf.letraSector,
        capacidadMax: Number(sf.capacidadMax),
        costoEntrada: Number(sf.costoEntrada),
      })
      setEstadios(prev => prev.map(e =>
        e.estadioId === estadioId
          ? { ...e, sectores: [...e.sectores, nuevo] }
          : e
      ))
      setSectorForms(prev => ({ ...prev, [estadioId]: { ...SECTOR_EMPTY } }))
    } catch (err) {
      setSectorErrors(prev => ({ ...prev, [estadioId]: err.message }))
    } finally {
      setSectorPending(prev => ({ ...prev, [estadioId]: false }))
    }
  }

  function setSectorField(estadioId, field, value) {
    setSectorForms(prev => ({ ...prev, [estadioId]: { ...prev[estadioId], [field]: value } }))
    setSectorErrors(prev => ({ ...prev, [estadioId]: '' }))
  }

  if (loading) return (
    <div className="loading-box"><div className="spinner" /><span>Cargando estadios…</span></div>
  )
  if (loadError) return <div className="alert alert-error">{loadError}</div>

  return (
    <>
      <div className="mb-24">
        <h1 className="page-title">Estadios</h1>
        <p className="page-subtitle">Alta de estadios y configuración de sectores</p>
      </div>

      {/* ── Form nuevo estadio ── */}
      <div className="card" style={{ padding: '22px 28px', marginBottom: 32 }}>
        <h2 style={{ marginBottom: 14 }}>Nuevo estadio</h2>
        {eError && <div className="alert alert-error">{eError}</div>}
        <form onSubmit={handleCrearEstadio}>
          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr', gap: 12, alignItems: 'end' }}>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label htmlFor="e-nombre">Nombre</label>
              <input id="e-nombre" type="text" placeholder="Estadio Monumental"
                value={eForm.nombre}
                onChange={e => setEForm(f => ({ ...f, nombre: e.target.value }))}
                disabled={eSubmitting} required />
            </div>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label htmlFor="e-pais">País</label>
              <input id="e-pais" type="text" placeholder="Argentina"
                value={eForm.pais}
                onChange={e => setEForm(f => ({ ...f, pais: e.target.value }))}
                disabled={eSubmitting} required />
            </div>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label htmlFor="e-ciudad">Ciudad</label>
              <input id="e-ciudad" type="text" placeholder="Buenos Aires"
                value={eForm.ciudad}
                onChange={e => setEForm(f => ({ ...f, ciudad: e.target.value }))}
                disabled={eSubmitting} required />
            </div>
          </div>

          {/* Sectores del estadio (RNE 3: al menos uno) */}
          <div className="form-section-title" style={{ marginTop: 18 }}>
            Sectores <span className="text-muted text-sm">(al menos uno — RNE 3)</span>
          </div>
          {eSectores.map((s, idx) => {
            const tomadas    = letrasTomadasEnAlta(idx)
            const opciones   = LETRAS.filter(l => !tomadas.includes(l))
            return (
              <div key={idx} style={{ display: 'grid', gridTemplateColumns: '130px 1fr 1fr auto', gap: 12, alignItems: 'end', marginBottom: 10 }}>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label>Sector</label>
                  <select value={s.letraSector}
                    onChange={e => setNuevoSectorField(idx, 'letraSector', e.target.value)}
                    disabled={eSubmitting}>
                    <option value="">—</option>
                    {opciones.map(l => (<option key={l} value={l}>Sector {l}</option>))}
                  </select>
                </div>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label>Capacidad máx.</label>
                  <input type="number" placeholder="5000"
                    value={s.capacidadMax}
                    onChange={e => setNuevoSectorField(idx, 'capacidadMax', e.target.value)}
                    disabled={eSubmitting} min={1} />
                </div>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label>Costo (UYU)</label>
                  <input type="number" placeholder="2500"
                    value={s.costoEntrada}
                    onChange={e => setNuevoSectorField(idx, 'costoEntrada', e.target.value)}
                    disabled={eSubmitting} min={1} step="0.01" />
                </div>
                <button type="button" className="btn btn-secondary" style={{ height: 42 }}
                  onClick={() => quitarFilaSector(idx)}
                  disabled={eSubmitting || eSectores.length === 1}
                  title={eSectores.length === 1 ? 'Debe quedar al menos un sector' : 'Quitar sector'}>
                  ✕
                </button>
              </div>
            )
          })}

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 6 }}>
            <button type="button" className="btn btn-secondary"
              onClick={agregarFilaSector}
              disabled={eSubmitting || eSectores.length >= LETRAS.length}>
              + Agregar sector
            </button>
            <button type="submit" className="btn btn-primary"
              style={{ height: 42 }}
              disabled={eSubmitting || !eForm.nombre.trim()}>
              {eSubmitting ? '…' : '+ Crear estadio'}
            </button>
          </div>
        </form>
      </div>

      {/* ── Lista de estadios ── */}
      {estadios.length === 0 ? (
        <div className="empty-state"><p>No hay estadios creados aún.</p></div>
      ) : estadios.map(estadio => {
        const tomadas      = estadio.sectores.map(s => s.letraSector)
        const disponibles  = LETRAS.filter(l => !tomadas.includes(l))
        const sf  = sectorForms[estadio.estadioId]  ?? SECTOR_EMPTY
        const se  = sectorErrors[estadio.estadioId] ?? ''
        const sp  = sectorPending[estadio.estadioId] ?? false

        return (
          <div key={estadio.estadioId} className="card" style={{ marginBottom: 20, overflow: 'hidden' }}>

            {/* Header */}
            <div style={{
              padding: '14px 24px',
              background: 'var(--color-primary-light)',
              borderBottom: '1px solid var(--color-border)',
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            }}>
              <div>
                <span style={{ fontFamily: 'var(--font-heading)', fontWeight: 700, fontSize: '1.05rem', letterSpacing: '.3px' }}>
                  {estadio.nombre}
                </span>
                <span className="text-muted text-sm" style={{ marginLeft: 12 }}>
                  {estadio.ciudad}, {estadio.pais} · ID {estadio.estadioId}
                </span>
              </div>
              <span className="badge badge-blue">
                {estadio.sectores.length}/{LETRAS.length} sectores
              </span>
            </div>

            <div style={{ padding: '18px 24px' }}>
              {/* Tabla de sectores */}
              {estadio.sectores.length > 0 && (
                <div className="table-wrapper" style={{ marginBottom: 20 }}>
                  <table>
                    <thead>
                      <tr>
                        <th>Sector</th>
                        <th>Capacidad máx.</th>
                        <th>Costo entrada</th>
                      </tr>
                    </thead>
                    <tbody>
                      {estadio.sectores.map(s => (
                        <tr key={s.letraSector}>
                          <td><span className="badge badge-blue">Sector {s.letraSector}</span></td>
                          <td>{s.capacidadMax.toLocaleString('es-UY')} personas</td>
                          <td>{fmtMoney(s.costoEntrada)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}

              {/* Form agregar sector */}
              {disponibles.length > 0 ? (
                <>
                  <div className="form-section-title" style={{ marginTop: estadio.sectores.length > 0 ? 0 : 0 }}>
                    Agregar sector
                  </div>
                  {se && <div className="alert alert-error">{se}</div>}
                  <div style={{ display: 'grid', gridTemplateColumns: '130px 1fr 1fr auto', gap: 12, alignItems: 'end' }}>
                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <label>Sector</label>
                      <select value={sf.letraSector}
                        onChange={e => setSectorField(estadio.estadioId, 'letraSector', e.target.value)}
                        disabled={sp}>
                        <option value="">—</option>
                        {disponibles.map(l => (
                          <option key={l} value={l}>Sector {l}</option>
                        ))}
                      </select>
                    </div>
                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <label>Capacidad máx.</label>
                      <input type="number" placeholder="5000"
                        value={sf.capacidadMax}
                        onChange={e => setSectorField(estadio.estadioId, 'capacidadMax', e.target.value)}
                        disabled={sp} min={1} />
                    </div>
                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <label>Costo (UYU)</label>
                      <input type="number" placeholder="2500"
                        value={sf.costoEntrada}
                        onChange={e => setSectorField(estadio.estadioId, 'costoEntrada', e.target.value)}
                        disabled={sp} min={1} step="0.01" />
                    </div>
                    <button className="btn btn-primary" style={{ height: 42 }}
                      disabled={sp || !sf.letraSector || !sf.capacidadMax || !sf.costoEntrada}
                      onClick={() => handleCrearSector(estadio.estadioId)}>
                      {sp ? '…' : '+ Agregar'}
                    </button>
                  </div>
                </>
              ) : (
                <p className="text-muted text-sm">Todos los sectores (A–D) están configurados.</p>
              )}
            </div>
          </div>
        )
      })}
    </>
  )
}
