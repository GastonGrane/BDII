import { useEffect, useRef, useState } from 'react'
import { api } from '../api/client'

export default function Validar() {
  /* ── dispositivos asignados al funcionario ── */
  const [dispositivos, setDispositivos] = useState(null)   // null = cargando
  const [devError,     setDevError]     = useState('')
  const [dispositivoId, setDispositivoId] = useState('')

  /* ── formulario de validación ── */
  const [codigoQR,  setCodigoQR]  = useState('')
  const [loading,   setLoading]   = useState(false)
  const [error,     setError]     = useState('')
  const [result,    setResult]    = useState(null)

  const qrRef = useRef(null)

  useEffect(() => {
    api.misDispositivos()
      .then(devs => {
        setDispositivos(devs)
        if (devs.length === 1) setDispositivoId(String(devs[0].dispositivoId))
      })
      .catch(e => setDevError(e.message))
  }, [])

  async function handleSubmit(e) {
    e.preventDefault()
    if (!codigoQR.trim() || !dispositivoId) return
    setLoading(true); setError(''); setResult(null)
    try {
      const res = await api.validar({
        codigoQR:      codigoQR.trim(),
        dispositivoId: Number(dispositivoId),
      })
      setResult(res)
      setCodigoQR('')
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  function resetResult() {
    setResult(null); setError('')
    setTimeout(() => qrRef.current?.focus(), 50)
  }

  /* ── helpers de render del campo dispositivo ── */
  function renderDispositivo() {
    if (devError) return (
      <div className="alert alert-error" style={{ marginBottom: 0 }}>{devError}</div>
    )
    if (dispositivos === null) return (
      <div style={{ padding: '10px 0', color: 'var(--color-text-muted)', fontSize: '.875rem' }}>
        Cargando dispositivo…
      </div>
    )
    if (dispositivos.length === 0) return (
      <div className="alert alert-error" style={{ marginBottom: 0 }}>
        No tenés dispositivos asignados. Contactá al administrador.
      </div>
    )
    if (dispositivos.length === 1) return (
      <div style={{
        padding: '10px 13px',
        background: 'var(--color-primary-light)',
        border: '1.5px solid var(--color-primary-muted)',
        borderRadius: 'var(--radius-md)',
        fontSize: '.9rem',
        display: 'flex', alignItems: 'center', gap: 8,
      }}>
        <span style={{ fontFamily: 'var(--font-heading)', fontWeight: 700, color: 'var(--color-primary-dark)' }}>
          Dispositivo #{dispositivos[0].dispositivoId}
        </span>
        <span className="text-muted text-sm">— asignado a tu cuenta</span>
      </div>
    )
    // múltiples dispositivos
    return (
      <select
        value={dispositivoId}
        onChange={e => setDispositivoId(e.target.value)}
        disabled={loading}
        required
      >
        <option value="">— Seleccioná un dispositivo —</option>
        {dispositivos.map(d => (
          <option key={d.dispositivoId} value={d.dispositivoId}>
            Dispositivo #{d.dispositivoId}
          </option>
        ))}
      </select>
    )
  }

  const deviceReady = dispositivos !== null && dispositivos.length > 0 && !devError

  return (
    <div className="center-page">
      {result ? (
        /* ── Resultado: acceso habilitado ── */
        <div className="card" style={{ maxWidth: 460, width: '100%', padding: '36px 40px' }}>
          <div className="success-block" style={{ marginBottom: 24 }}>
            <div className="success-icon">✅</div>
            <h2 style={{ marginBottom: 4 }}>Ingreso habilitado</h2>
            <p>La entrada es válida. El acceso fue registrado.</p>
          </div>

          <div className="success-detail">
            <div className="detail-row">
              <span className="dk">Entrada</span>
              <span className="dv">#{result.entradaId}</span>
            </div>
            <div className="detail-row">
              <span className="dk">Evento</span>
              <span className="dv">#{result.eventoId}</span>
            </div>
            {result.letraSector && (
              <div className="detail-row">
                <span className="dk">Sector</span>
                <span className="dv">{result.letraSector}</span>
              </div>
            )}
            <div className="detail-row">
              <span className="dk">Titular</span>
              <span className="dv">{result.mailPropietario}</span>
            </div>
          </div>

          <button className="btn btn-primary btn-full" style={{ marginTop: 24 }} onClick={resetResult}>
            Validar otro ingreso
          </button>
        </div>

      ) : (
        /* ── Formulario ── */
        <div className="card card-auth" style={{ maxWidth: 460 }}>
          <h1 style={{ marginBottom: 4 }}>Validar ingreso</h1>
          <p className="subtitle">Pegá el código QR de la entrada del asistente.</p>

          {error && <div className="alert alert-error" role="alert">{error}</div>}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="codigoQR">Código QR</label>
              <input
                id="codigoQR"
                ref={qrRef}
                type="text"
                placeholder="Pegá el código aquí"
                value={codigoQR}
                onChange={e => { setCodigoQR(e.target.value); setError('') }}
                disabled={loading || !deviceReady}
                autoFocus
                autoComplete="off"
              />
            </div>

            <div className="form-group">
              <label>Dispositivo</label>
              {renderDispositivo()}
            </div>

            <button
              className="btn btn-primary btn-full"
              type="submit"
              style={{ marginTop: 8 }}
              disabled={loading || !codigoQR.trim() || !dispositivoId || !deviceReady}
            >
              {loading ? 'Validando…' : 'Validar ingreso'}
            </button>
          </form>
        </div>
      )}
    </div>
  )
}
