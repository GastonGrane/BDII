export function fmtFecha(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('es-UY', {
    weekday: 'short', day: 'numeric', month: 'short',
    year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

export function fmtFechaSola(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('es-UY', {
    day: 'numeric', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

export function fmtMoney(n) {
  if (n == null) return '—'
  return '$ ' + Number(n).toLocaleString('es-UY', {
    minimumFractionDigits: 2, maximumFractionDigits: 2,
  })
}
