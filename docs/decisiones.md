# Decisiones de diseño — Sistema de Ticketing Mundial 2026

Registro de decisiones tomadas durante el diseño del modelo lógico y físico,
para reusar en el Informe y repasar en la defensa oral.

---

## DEC-01 — EstadoVerificacion en USUARIO_GENERAL

**Tabla:** `USUARIO_GENERAL`  
**Columna:** `EstadoVerificacion ENUM('Pendiente','Verificado','Rechazado')`  
**Etapa:** Modelo Lógico (no forma parte del MER original ni de las RNEs del informe)

**Decisión:** Se agrega la columna con dominio fijo {'Pendiente', 'Verificado', 'Rechazado'} y valor por defecto 'Pendiente'.

**Origen:** El atributo `EstadoVerificacion` figura en el MER como atributo de `USUARIO_GENERAL`, pero el enunciado no fija su dominio ni sus estados válidos. El dominio se definió en la etapa de Modelo Lógico como supuesto de implementación, siguiendo el patrón estándar de flujos de verificación de cuenta (pendiente → verificado / rechazado).

**Cómo presentarlo:** Mencionarlo en el informe como supuesto aclarado en la etapa de Modelo Lógico, no como dato del MER. Ante la defensa: "el MER tenía el atributo pero no su dominio; lo definimos en el modelo lógico como supuesto razonable para la implementación."

---

## DEC-02 — Volumen de TOKEN_QR y limpieza de tokens viejos

**Tabla:** `TOKEN_QR`  
**Etapa:** Modelo Físico / consideración para demo y defensa

**Situación:** El QR se regenera cada 30 segundos por cada entrada activa mientras la app está en primer plano (RNE 10). Cada regeneración inserta una nueva fila en `TOKEN_QR` y desactiva la anterior (`Activo = FALSE`). En un evento con miles de asistentes activos simultáneamente, la tabla puede crecer muy rápido: por ejemplo, 10.000 usuarios activos durante 2 horas antes del partido equivalen a ~2.400.000 tokens generados por evento.

**Decisión actual:** No se implementa ningún proceso de limpieza en el alcance del obligatorio. Los tokens inactivos (`Activo = FALSE` y ya pasados por validación o expirados) quedan en la tabla como historial.

**Mejora futura a mencionar en la defensa:** Un job de limpieza periódica (DELETE de tokens inactivos con más de X horas) reduciría el volumen sin perder información relevante — los tokens ya validados tienen su registro en `VALIDACION`, y los tokens inactivos que nunca fueron validados no tienen valor histórico. Esta es una optimización de modelo físico, no un cambio de modelo lógico.

**Índice crítico:** `INDEX idx_token_qr_entrada_activo (EntradaID, Activo)` — permite localizar el token activo de una entrada en O(log n) sin escanear toda la tabla.

---

## DEC-04 — INNER JOIN en v_monto_total_venta es correcto (no puede haber VENTA sin ENTRADA)

**Vista:** `v_monto_total_venta`  
**Etapa:** Modelo Físico

**Decisión:** La vista usa `INNER JOIN ENTRADA` (no `LEFT JOIN`). Una VENTA sin filas en ENTRADA quedaría excluida, pero **ese caso no puede ocurrir en estado estable**.

**Por qué:** El MER define la relación Genera con cardinalidad `VENTA(1,1) -- (1,N)ENTRADA`: cada VENTA genera al menos una ENTRADA. En la implementación, la transacción de compra inserta VENTA y sus ENTRADAs atómicamente; si falla alguna INSERT de ENTRADA, toda la transacción hace rollback y la VENTA no persiste.

**Cómo presentarlo ante la defensa:** "El INNER JOIN refleja la cardinalidad del MER. La atomicidad de la transacción de compra garantiza que nunca persista una VENTA sin entradas — si algo falla en la inserción de entradas, el rollback elimina también la VENTA."

---

## DEC-05 — Duración fija de 4 horas para RNE 4 (solapamiento de eventos)

**Tabla:** `EVENTO` / Trigger `tr_evento_sin_solapamiento`  
**Etapa:** Modelo Físico

**Decisión:** El esquema no tiene columna `FechaFin` en EVENTO. Para verificar solapamiento (RNE 4) en el trigger, se asume una **duración fija de 4 horas** por evento.

**Por qué:** El enunciado no establece duración de los partidos. Cuatro horas cubre el partido más ceremonias. Agregar `FechaFin` sería más correcto pero implica un cambio de esquema fuera del alcance de la normalización cerrada.

**Mejora futura:** Agregar `FechaFin DATETIME NOT NULL` a EVENTO y usar `FechaFin` real en el trigger. Para la demo/defensa, mencionar esto proactivamente como simplificación consciente.

---

## DEC-06 — RNE 8 (propietario al validar) se enforcea en aplicación, no en BD

**RNE:** 8 — "solo el propietario actual de una entrada puede presentarla para validación"  
**Etapa:** Modelo Físico

**Decisión:** Esta RNE no puede implementarse como trigger en BD. La tabla `VALIDACION` registra al funcionario que escanea, no al usuario que presenta el ticket. La BD no tiene noción de "quién está mostrando el QR en su teléfono".

**Dónde se enforcea:** En la capa de aplicación (Spring Boot): cuando el backend recibe la solicitud de validación, verifica que `ENTRADA.Mail_Propietario` coincida con el usuario autenticado que inició el proceso de validación antes de dejar que el funcionario escanee.

---

## DEC-07 — RNE 5 (cobertura de sectores) se verifica con vista + SP, no con trigger

**RNE:** 5 — "un funcionario debe haber validado en todos los sectores asignados"  
**Etapa:** Modelo Físico

**Decisión:** No se implementa como trigger porque es una restricción sobre la **ausencia** de registros (un sector sin validación no genera ningún INSERT que dispare nada). Se implementa como:
- Vista `v_cobertura_funcionario`: muestra por funcionario/evento/sector si hay al menos una validación.
- SP `sp_verificar_cobertura(p_EventoID)`: retorna los funcionarios incumplidores para un evento dado.

**Cuándo usarlo:** Llamar al cierre de cada evento, o exponer como endpoint de auditoría en el backend.

---

## ~~PENDIENTE-01~~ — Revisar interacción tr_transferencia_resolver / RNE 7 ✔ RESUELTO

**Archivo:** `sql/triggers.sql` — trigger `tr_transferencia_resolver`  
**Estado:** resuelto — verificado con T14 en test_triggers.sql

**Comportamiento confirmado:** Si una entrada pasa a `Consumida` mientras tiene una transferencia `Pendiente`, intentar aceptar esa transferencia dispara `tr_transferencia_resolver` → intenta `UPDATE ENTRADA SET EstadoEntrada = 'Activa'` → `tr_entrada_consumida_irreversible` (BEFORE UPDATE) eleva error RNE 7 → el UPDATE de TRANSFERENCIA hace rollback. La BD queda consistente: ENTRADA sigue `Consumida`, TRANSFERENCIA sigue `Pendiente`.

---

## ~~PENDIENTE-02~~ — sp_nueva_comision necesita manejo explícito de transacción ✔ RESUELTO

**Archivo:** `sql/triggers.sql` — procedimiento `sp_nueva_comision`  
**Estado:** resuelto — fix aplicado en triggers.sql

**Situación original:** Sin manejo de transacción, un fallo en el INSERT dejaba el UPDATE de cierre aplicado, resultando en una BD sin comisión activa.

**Solución aplicada:** Se agregó `DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;` y `START TRANSACTION / COMMIT`. El ROLLBACK en el handler garantiza atomicidad: si el INSERT falla, el UPDATE de cierre se revierte. Probado en T13 (test_triggers.sql).

---

## DEC-03 — MontoTotal de VENTA como vista calculada

**Tabla:** `VENTA` (columna eliminada) / Vista `v_monto_total_venta`  
**Etapa:** Normalización (Parte III §3.4 del informe) → Modelo Físico

**Decisión:** `MontoTotal` no existe como columna en `VENTA`. Se calcula dinámicamente mediante la vista `v_monto_total_venta`:

```
MontoTotal = SUM(ENTRADA.Costo_Historico) × (1 + COMISION.Porcentaje / 100)
```

**Por qué:** `MontoTotal` es un atributo derivado — depende de datos de `ENTRADA` y `COMISION`, no de `VentaID` directamente. Persistirlo violaría 3FN e introduciría riesgo de desincronización.

**Alternativa descartada:** Materializar con trigger. Se descarta en el modelo lógico porque el dato sigue siendo redundante; queda como optimización física futura si hay problemas de performance.

**Cómo presentarlo ante la defensa:** "La normalización detectó que MontoTotal era un atributo calculable. Lo eliminamos de VENTA y lo exponemos como vista, que suma los costos históricos de las entradas más la comisión vigente al momento de la compra."

---

## DEC-08 — Alcance del prototipo ejecutable

**Etapa:** Implementación (ejecutable — entrega 24/6/2026)

### Dentro del alcance — núcleo obligatorio

| Módulo | Descripción |
|---|---|
| Registro y login | Alta de usuario con mail/contraseña en texto plano. Login determina rol (Administrador / Funcionario / Usuario General) y habilita las funciones correspondientes. |
| Compra de entradas | Crea VENTA + ENTRADAs en una transacción. RNE 1 (máx. 5 entradas/venta) la enforcea el trigger. `MontoTotal` se calcula en el backend con query directa (vista no disponible en BD remota hasta habilitar el permiso — DEC-03). |
| Transferencia de entradas | Crear solicitud de transferencia (Pendiente); el destinatario puede aceptar o rechazar. RNEs 2, 6 y máquina de estados las enforcea el trigger. |
| Validación de ingreso | El funcionario ingresa manualmente el `CodigoQR` (texto). El backend hace INSERT en VALIDACION; los triggers de RNE 7/8/9 corren normalmente. Sin escaneo de cámara (requerimiento opcional del enunciado — ver más abajo). |
| Listados por usuario | Mis compras, mis transferencias, mis entradas actualmente asignadas. |
| Alta de eventos / estadios / sectores | Pantalla simple para Administrador. RNE 4 (solapamiento) la enforcea el trigger. |

### Fuera del alcance — requerimientos opcionales del enunciado, no implementados por tiempo

| Requerimiento | Justificación |
|---|---|
| Escaneo real de QR con cámara/dispositivo | Marcado como opcional en el enunciado. La validación en sí (INSERT en VALIDACION con sus RNEs) sí se implementa; solo se omite el escaneo óptico. |
| Reportes estadísticos (rankings de compradores, eventos más vendidos, etc.) | Marcados como opcionales en el enunciado ("Reportes estadísticos del lado del administrador"). Si sobra tiempo, se pueden mostrar como queries SQL directas en DBeaver. |
| Docker, alta disponibilidad, optimización avanzada de modelo físico | Opcionales de implementación, fuera del alcance de un prototipo académico. |

### Simplificaciones conscientes (no opcionales, pero documentadas)

- **Contraseñas en texto plano:** sin hashing por alcance del obligatorio. Mejora futura: BCrypt en Spring Security.
- **Autenticación por sesión HTTP básica:** sin JWT ni OAuth. Suficiente para prototipo de laboratorio.
- **`MontoTotal` via query en vez de vista:** la vista existe en el esquema local; en CD_Grupo4 se calcula con la misma lógica hasta que la cátedra habilite `log_bin_trust_function_creators`.

---

## Nota de validación

Los scripts `schema.sql`, `triggers.sql` y `seed.sql` fueron validados end-to-end en un entorno MySQL 8.0 equivalente al de la cátedra antes de aplicarse a `CD_Grupo4`. Se verificó la creación de las 17 tablas, los 13 triggers, los 2 stored procedures, las 2 vistas y los datos iniciales de `COMISION`. Los pendientes PENDIENTE-01 y PENDIENTE-02 se resuelven antes del despliegue final en la BD remota.
