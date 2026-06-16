# Bitácora de modelado

Esta bitácora explica cómo se interpretó el dominio y por qué el modelo quedó como está.
Sigue el enfoque del material de clase: *modelar no es dibujar un MER, es interpretar una
realidad; si no puedo explicar por qué modelé algo así, entonces no lo entendí.*

## 1. Interpretación del dominio

El sistema vende, transfiere y valida entradas para partidos del Mundial 2026. La idea
central es que **una entrada no es una imagen estática**: es un activo digital que tiene
un dueño (que puede cambiar por transferencia) y que para ingresar muestra un **token que
muta cada 30 segundos**, de modo que una captura de pantalla no sirve para entrar.

De ahí surgen tres ejes de modelado:

1. **Quiénes** participan: usuarios con tres roles (administrador por país sede,
   funcionario de validación, usuario general).
2. **Qué** se vende: eventos en estadios divididos en sectores con capacidad y precio.
3. **Cómo** se controla: ventas con comisión, transferencias con aceptación, tokens
   dinámicos y validación con dispositivo autorizado.

## 2. Supuestos

- **Duración del evento**: el enunciado no la fija. Se asume 4 horas para detectar
  superposición de eventos en el mismo estadio (DEC-05).
- **Ventana del token**: 30 segundos (RNE 10). El token se regenera *bajo demanda*: cuando
  el cliente lo solicita y el anterior venció, el backend genera uno nuevo. Es equivalente
  a regenerarlo cada 30s y evita un proceso en segundo plano que generaría millones de
  filas (DEC-02).
- **EstadoVerificacion** del usuario general: dominio {Pendiente, Verificado, Rechazado},
  definido en el modelo lógico (DEC-01).
- **Contraseñas en texto plano**: simplificación de alcance del prototipo (DEC-08).

## 3. Entidades, relaciones, cardinalidades y participación

Ver la tabla de cardinalidades en [04_MODELO_DATOS.md](04_MODELO_DATOS.md). El criterio
fue, para cada relación, tomar una ocurrencia de A y preguntar con cuántas de B se
relaciona como máximo (cardinalidad máxima) y si la participación es obligatoria
(mínima 1) u opcional (mínima 0). Ejemplos:

- Una **VENTA** participa con mínimo 1 y máximo 5 **ENTRADAS** (regla de negocio acotando
  el máximo). Una **ENTRADA** participa con exactamente 1 **VENTA**.
- Una **ENTRADA** tiene mínimo 0 y máximo 3 **TRANSFERENCIAS** antes de validarse.
- Una **ENTRADA** tiene muchos **TOKEN_QR** en el tiempo, pero como máximo 1 activo.

## 4. Restricciones no estructurales (y dónde se implementan)

Algunas reglas no entran “naturalmente” en el MER y se llevan a SQL, a servicio o a
consulta/reporte:

| Regla | Dónde se implementa | Por qué ahí |
|---|---|---|
| Máx. 5 entradas por venta | Trigger + servicio | Conteo sobre filas: trigger es la garantía; el servicio da el mensaje. |
| Aforo (capacidad dura) | Lock pesimista + servicio + trigger | El lock serializa concurrencia; el trigger es la red de seguridad. |
| No superposición de eventos | Trigger | Depende de comparar intervalos de otras filas. |
| Máx. 3 transferencias | Trigger + servicio | Conteo sobre filas. |
| Cambio de propietario solo al aceptar | Trigger (máquina de estados) | Mantiene `ENTRADA` consistente con el ciclo de transferencia. |
| Consumo irreversible / no doble validación | Triggers + PK de `VALIDACION` | La BD garantiza atomicidad e irreversibilidad. |
| Token vigente (30s) | Servicio (`TokenService`/`ValidacionService`) | La vigencia es temporal y se evalúa en cada operación. |
| Jurisdicción del administrador | Servicio (`AdminService`) | Depende del usuario autenticado, no de una sola tabla. |
| Cobertura de sectores (RNE 5) | Vista + reporte | Es una restricción sobre *ausencia* de validaciones: no la dispara ningún INSERT, se verifica con una consulta. |

## 5. Qué se descartó y por qué

- **Persistir MontoTotal**: descartado por 3FN; se calcula con vista/servicio.
- **Regenerar el token con un job en segundo plano cada 30s**: descartado por volumen;
  se regenera bajo demanda con el mismo efecto funcional.
- **Modelo de roles genérico (una sola tabla de usuarios con un campo “rol”)**:
  descartado porque se perderían los datos propios de cada subtipo (PaisSede,
  NroLegajo, FechaRegistro/EstadoVerificacion).

## 6. Decisiones tomadas a partir del MER `BDIIFifaSegundaVersion`

- **Por qué USUARIO se modela con subtipos**: cada rol tiene atributos propios
  (administrador: PaisSede/FechaAsignacion; funcionario: NroLegajo; usuario general:
  FechaRegistro/EstadoVerificacion). Un único “usuario con rol” perdería esos datos. Se
  usa especialización disjunta y total → tabla por subclase.
- **Por qué Teléfonos va como tabla separada**: es un atributo multivaluado; ponerlo como
  texto separado por comas violaría 1FN e impediría consultas. Va a `TELEFONO` (1:N).
- **Por qué Sector depende de Estadio**: un sector “A” solo tiene sentido dentro de un
  estadio; es entidad débil con PK compuesta `(EstadioID, LetraSector)`.
- **Por qué EventoSector es necesario**: “Evento habilita Sector” es N:N (un evento
  habilita varios sectores; un sector se habilita en varios eventos). Se materializa en
  `EVENTO_SECTOR`, y `ENTRADA` referencia ese par como unidad.
- **Por qué Entrada tiene comprador original y propietario actual**: el obligatorio separa
  compra de tenencia. El comprador original se obtiene por `ENTRADA → VENTA`; el
  propietario actual es `ENTRADA.Mail_Propietario`, que cambia al aceptarse una
  transferencia.
- **Por qué Transferencia queda como histórico**: cada solicitud (aceptada o rechazada) es
  una fila de `TRANSFERENCIA`; así se reconstruye la cadena de custodia y se cuenta el
  máximo de 3 transferencias.
- **Por qué TokenQR es entidad propia**: cada token tiene su vigencia y queda registrado;
  además permite saber exactamente qué token se aceptó en la validación. Las filas
  anteriores forman el histórico de tokens de la entrada.
- **Por qué Validación registra funcionario, dispositivo y token**: es la relación ternaria
  del MER; registrar los tres da trazabilidad (quién validó, con qué dispositivo, qué
  token) y, con PK = TokenID, garantiza que no haya doble validación.
- **Por qué algunas reglas son SQL, otras servicio y otras consulta**: las reglas sobre
  filas existentes (conteos, estados, solapamientos) se hacen en triggers porque la BD las
  garantiza siempre; las reglas que dependen del usuario autenticado o del tiempo se hacen
  en el servicio; las reglas sobre *ausencia* de registros (cobertura RNE 5) se verifican
  con una consulta/reporte porque no hay INSERT que las dispare.

## 7. Conclusión defendible

Partimos del MER entregado, lo usamos como modelo conceptual base, lo pasamos a modelo
lógico relacional, aplicamos normalización (1FN–3FN) y llevamos las restricciones
obligatorias al modelo físico (PK/FK/UNIQUE/CHECK/triggers) y a la lógica de aplicación
(transacciones, locks, validaciones) según dónde cada regla se garantiza mejor.
