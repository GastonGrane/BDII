# Decisiones de diseño

Decisiones (`DEC-xx`) referenciadas desde el código. Las demás (`DEC-01/02/03/05/07/08`)
están registradas en el informe del obligatorio; aquí se documenta la que se agregó en esta
iteración para que no quede incongruente con la letra.

## DEC-09 — Estadio con al menos un sector (mejora de modelo, apoya RNE 3)

**Contexto.** La RNE 3 del obligatorio dice literalmente: *"un evento debe habilitar al menos un
sector del estadio donde se realiza"*. Es una regla sobre el **evento**, no sobre el estadio.

**Decisión.** Como mejora del modelo, el alta de **estadio** exige cargar al menos un sector en el
mismo formulario/payload. Motivo: un estadio sin sectores es inútil (no se le puede dar de alta
ningún evento, porque un evento necesita habilitar sectores que existan en su estadio). Crear el
estadio y sus sectores juntos evita el estado intermedio "estadio vacío" y hace el flujo usable.

**Importante (para no contradecir la letra).** Esta exigencia es un **agregado**, no la RNE 3
literal. Por eso:
- El mensaje de error del estadio dice *"El estadio debe tener al menos un sector"* (sin etiquetarlo
  como "RNE 3").
- La RNE 3 propiamente dicha (un **evento** debe habilitar ≥1 sector, y solo sectores de su estadio)
  se sigue validando en `AdminService.crearEvento` y se refuerza con los triggers
  `tr_evento_sector_estadio_insert/update`.

**Implementación.**
- Endpoint único `POST /api/estadios` recibe `{ nombre, pais, ciudad, sectores[] }` y crea el
  estadio con sus sectores en una sola transacción (`AdminService.crearEstadio` →
  `persistirSector`). Si algún sector es inválido (letra repetida, capacidad/costo ≤ 0) se rechaza
  toda el alta.
- Se mantiene `POST /api/estadios/{id}/sectores` para agregar sectores después.
- Frontend (`pages/AdminEstadios.jsx`): el formulario "Nuevo estadio" incluye filas de sectores con
  "+ Agregar sector" y botón de quitar; no permite crear si no hay al menos un sector completo.

**Validación.** Frontend (no permite enviar sin sectores) **y** backend (rechaza el payload sin
sectores y valida cada sector). Tests: `AdminServiceTest` (alta sin sectores rechazada, sector
inválido/ajeno al estadio, alta válida).
