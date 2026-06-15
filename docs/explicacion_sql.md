# Explicación SQL — Ticketing Mundial 2026

## Estructura general

La base de datos se llama `CD_Grupo4` (producción) / `CD_Grupo4_local` (desarrollo).
Tres archivos que se corren en orden estricto:

| Archivo | Qué hace |
|---|---|
| `schema.sql` | Crea todas las tablas, restricciones, índices y la vista `v_monto_total_venta` |
| `triggers.sql` | Instala los 9 triggers, 2 stored procedures y la vista `v_cobertura_funcionario` |
| `seed.sql` | Inserta datos iniciales: usuarios de prueba, estadios, sectores, eventos y comisión vigente |

---

## Las tablas — qué es cada una y por qué existe

### Módulo 1 — Usuarios (jerarquía herencia)

```
USUARIO  ← superclase (Mail, credenciales, documento, dirección)
   ├── ADMINISTRADOR  ← da de alta estadios y eventos
   ├── FUNCIONARIO    ← valida entradas en el estadio con un dispositivo
   └── USUARIO_GENERAL ← compra entradas y las transfiere
TELEFONO ← atributo multivaluado de USUARIO (tabla 1:N aparte)
```

**Patrón tabla-por-subclase (InheritanceType.JOINED):** `USUARIO` guarda los datos comunes. Cada subtipo tiene su propia tabla cuya `PK` es también `FK` hacia `USUARIO`. Esto garantiza especialización disjunta y total: un mismo mail no puede ser Admin *y* Funcionario simultáneamente.

### Módulo 2 — Infraestructura y eventos

```
ESTADIO             ← sede física del partido
   └── SECTOR       ← entidad débil; PK compuesta (EstadioID, LetraSector)
         └── EVENTO_SECTOR ← qué sectores habilita un evento concreto
EVENTO              ← partido (EquipoLocal vs EquipoVisitante, FechaHora, EstadioID)
```

**¿Por qué `EVENTO_SECTOR`?** Un evento puede habilitar solo 2 de los 4 sectores de un estadio. `EVENTO_SECTOR` modela esa relación N:N (evento × sector). Además, `ENTRADA` y `ASIGNACION_FUNCIONARIO` referencian el par `(EventoID, EstadioID, LetraSector)` como unidad, por eso la tabla intermedia es necesaria.

**¿Por qué PK compuesta en `SECTOR`?** La letra "A" puede existir en múltiples estadios. Solo es única *dentro* de un estadio, de ahí que la PK sea `(EstadioID, LetraSector)`.

### Módulo 3 — Ventas y entradas

```
COMISION    ← porcentaje vigente en un rango temporal (F_Desde, F_Hasta NULL = vigente)
VENTA       ← transacción de compra (sin MontoTotal — es calculado, ver DEC-03)
   └── ENTRADA ← ticket individual; propietario actual puede diferir del comprador tras transferencias
         └── TRANSFERENCIA ← log histórico de cambios de propietario
```

**¿Por qué `Costo_Historico` en `ENTRADA`?** El precio del sector puede cambiar. Guardamos el costo *al momento de la compra* (snapshot) para que el MontoTotal histórico sea correcto incluso si después cambia el precio del sector.

**¿Por qué no hay `MontoTotal` en `VENTA`?** Es un atributo calculable: `SUM(Costo_Historico) × (1 + Porcentaje/100)`. Lo calcula la vista `v_monto_total_venta` y el backend lo usa para mostrar el historial (DEC-03).

### Módulo 4 — Seguridad y validación

```
TOKEN_QR    ← código UUID activo por entrada (Activo = TRUE/FALSE)
DISPOSITIVO ← hardware de escaneo, asignado a un FUNCIONARIO (RNE 11)
VALIDACION  ← registro de cada validación exitosa; PK = TokenID (participación (0,1))
ASIGNACION_FUNCIONARIO ← qué funcionario cubre qué sector de qué evento
```

**¿Por qué `Activo` en lugar de borrar el token?** Para mantener historial de auditoría. La columna `Activo = FALSE` indica "ya no sirve" sin perder el registro. Un trigger garantiza que solo haya un token activo por entrada a la vez.

---

## Los triggers — reglas de negocio automáticas

Un trigger es código SQL que la BD ejecuta automáticamente ante INSERT/UPDATE. Son la capa que impone reglas que no se pueden expresar con restricciones simples.

### Módulo 1: ENTRADA

| Trigger | Cuándo dispara | Qué hace |
|---|---|---|
| `tr_entrada_limite_venta` | BEFORE INSERT ON ENTRADA | Rechaza si la VentaID ya tiene ≥ 5 entradas (RNE 1) |
| `tr_entrada_consumida_irreversible` | BEFORE UPDATE ON ENTRADA | Rechaza cambiar EstadoEntrada fuera de "Consumida" (RNE 7) |

### Módulo 2: TRANSFERENCIA (máquina de estados de ENTRADA)

| Trigger | Cuándo dispara | Qué hace |
|---|---|---|
| `tr_transferencia_limite` | BEFORE INSERT ON TRANSFERENCIA | Rechaza si hay ≥ 3 transferencias no-Rechazadas (RNE 2). Cuenta Pendiente + Aceptada para impedir gaming por rechazo |
| `tr_transferencia_entrada_activa` | BEFORE INSERT ON TRANSFERENCIA | Rechaza si la entrada no está Activa (RNE 6) |
| `tr_transferencia_marcar_pendiente` | AFTER INSERT ON TRANSFERENCIA | Pone la ENTRADA en PendienteTransferencia — bloquea nuevas transferencias concurrentes |
| `tr_transferencia_resolver` | AFTER UPDATE ON TRANSFERENCIA | Si Pendiente→Aceptada: cambia Mail_Propietario y restaura Activa; si Pendiente→Rechazada: solo restaura Activa |

### Módulo 3: TOKEN_QR

| Trigger | Cuándo dispara | Qué hace |
|---|---|---|
| `tr_token_unico_activo_insert` | BEFORE INSERT ON TOKEN_QR | Rechaza si ya existe un token Activo para esa EntradaID |
| `tr_token_unico_activo_update` | BEFORE UPDATE ON TOKEN_QR | Rechaza reactivar un token si ya hay otro activo para la misma entrada |

MySQL no soporta índices parciales (`WHERE Activo = TRUE`), de ahí que la restricción viva en triggers.

### Módulo 4: VALIDACION

| Trigger | Cuándo dispara | Qué hace |
|---|---|---|
| `tr_validacion_token_activo` | BEFORE INSERT ON VALIDACION | Rechaza si el token no está activo (RNE 9) |
| `tr_validacion_entrada_no_consumida` | BEFORE INSERT ON VALIDACION | Rechaza si la entrada ya está Consumida (RNE 7, defensa en profundidad) |
| `tr_validacion_post_insert` | AFTER INSERT ON VALIDACION | Marca la ENTRADA como Consumida y desactiva todos sus TOKEN_QR — atómico con el INSERT |

### Módulo 5: EVENTO

| Trigger | Cuándo dispara | Qué hace |
|---|---|---|
| `tr_evento_sin_solapamiento` | BEFORE INSERT ON EVENTO | Rechaza si hay otro evento en el mismo estadio dentro de la ventana de 4 horas (RNE 4) |

**Condición de solapamiento de intervalos:** Dos intervalos `[A, A+4h)` y `[B, B+4h)` se solapan si `A < B+4h AND B < A+4h`. Esta es la fórmula estándar de solapamiento.

### Módulo 6: COMISION

| Elemento | Tipo | Qué hace |
|---|---|---|
| `tr_comision_sin_solapamiento` | Trigger BEFORE INSERT | Rechaza cualquier INSERT que genere solapamiento de vigencias (RNE 12). Es la red de seguridad |
| `sp_nueva_comision(porcentaje, desde)` | Stored Procedure | Cierra la comisión vigente (UPDATE F_Hasta) e inserta la nueva en una sola transacción atómica |

**¿Por qué el SP en lugar de solo el trigger?** MySQL no permite hacer UPDATE sobre la misma tabla dentro de un trigger. El cierre de la comisión anterior debe hacerse en el SP; el trigger protege contra INSERTs directos que saltean el SP.

---

## Preguntas frecuentes de defensa

**¿Por qué usar triggers y no hacer todo en el backend?**
Las reglas críticas (consumo de entrada, solapamiento de evento) deben ser atómicas e imposibles de saltear. Un trigger garantiza que *siempre* se aplican, incluso si alguien ejecuta SQL directamente contra la BD. El backend podría fallar a mitad de una operación; el trigger no.

**¿Por qué `saveAndFlush()` en el backend cuando hay triggers?**
Con `save()`, Hibernate encola el INSERT pero lo envía al final de la transacción. `saveAndFlush()` lo envía inmediatamente, lo que fuerza al trigger a correr *dentro* del try/catch del backend. Así podemos capturar el mensaje de error del trigger (ej. "RNE 4: solapamiento") y devolvérselo al frontend con un 409.

**¿Qué es una PK compuesta y cuándo se usa?**
Es una clave primaria que requiere más de un atributo para identificar una fila. Se usa en `SECTOR` `(EstadioID, LetraSector)`, `EVENTO_SECTOR` `(EventoID, EstadioID, LetraSector)` y `ASIGNACION_FUNCIONARIO` — porque ninguno de sus atributos por sí solo es único.

**¿Qué garantiza una FK con ON DELETE RESTRICT?**
Impide borrar una fila padre si tiene hijos. Por ejemplo, no se puede borrar un ESTADIO que tiene EVENTOs, ni borrar un TOKEN_QR que ya tiene una VALIDACION. Protege la integridad referencial automáticamente.

**¿Por qué `TRANSFERENCIA.Mail_Origen/Destino` referencian `USUARIO` y no `USUARIO_GENERAL`?**
El MER modela ambas relaciones (EsOrigen, EsDestino) desde la superclase USUARIO. Aunque en la práctica solo los USUARIO_GENERAL transfieren, la FK apunta a la superclase para ser fiel al modelo conceptual.

**¿Qué es el seed y para qué sirve?**
Son los datos iniciales: usuarios de prueba (user1/user2/func/admin), estadios, sectores, eventos y una comisión vigente del 5%. Sin seed, la aplicación arranca con BD vacía e inutilizable. El seed también es el estado "limpio" al que volvemos antes de demo/defensa.

**¿Cómo funciona el flujo completo de validación de ingreso a nivel de BD?**
1. Backend hace `INSERT INTO VALIDACION (TokenID, ...)`
2. `tr_validacion_token_activo` (BEFORE): verifica `TOKEN_QR.Activo = TRUE` → si no, SIGNAL error
3. `tr_validacion_entrada_no_consumida` (BEFORE): verifica que la entrada no sea Consumida → si sí, SIGNAL error
4. Si pasan ambos BEFORE: la fila se inserta
5. `tr_validacion_post_insert` (AFTER): `UPDATE ENTRADA SET EstadoEntrada = 'Consumida'` + `UPDATE TOKEN_QR SET Activo = FALSE` para toda la entrada
6. Todo ocurre dentro de la misma transacción — si algo falla, nada queda escrito
