# Explicación Backend — Ticketing Mundial 2026

## ¿Qué es Spring Boot y por qué lo usamos?

Spring Boot es un framework de Java que levanta un servidor web (API REST) con mínima configuración. En lugar de configurar Tomcat, el pool de conexiones a la BD y la serialización JSON manualmente, Spring los configura automáticamente al detectar las dependencias correctas en `pom.xml`.

**Nuestra API escucha en `http://localhost:8080`** y expone endpoints bajo `/api/`. El frontend React la consume vía el proxy de Vite.

---

## Estructura de carpetas

```
backend/src/main/java/com/grupo4/ticketing/
│
├── TicketingApplication.java     ← punto de entrada, carga el .env al arrancar
├── config/
│   └── CorsConfig.java           ← permite peticiones cross-origin desde localhost:3000
├── entity/                       ← representación Java de las tablas SQL (JPA/Hibernate)
├── repository/                   ← acceso a la BD mediante Spring Data JPA
├── service/                      ← lógica de negocio y aplicación de RNEs
├── controller/                   ← recibe HTTP, llama al service, devuelve respuesta
├── dto/                          ← objetos de entrada/salida de la API (sin lógica)
└── util/
    └── SessionUtils.java         ← valida sesión activa y extrae mensajes de errores de triggers
```

---

## Las cinco capas — explicadas con ejemplos del proyecto

### 1. Entity (Entidad JPA)

Una **entidad** es una clase Java anotada con `@Entity` que Hibernate mapea a una tabla. Cada instancia = una fila.

```java
@Entity
@Table(name = "VENTA")
public class Venta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ventaId;           // → columna VentaID (AUTO_INCREMENT)

    @ManyToOne
    @JoinColumn(name = "Mail_Comprador")
    private UsuarioGeneral comprador; // → FK a USUARIO_GENERAL

    @Enumerated(EnumType.STRING)
    private EstadoVenta estado;       // → ENUM('Pendiente','Confirmada','Paga')
}
```

**Herencia con `InheritanceType.JOINED`** (tabla por subclase):
```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Usuario { ... }         // tabla USUARIO — datos comunes

@Entity public class UsuarioGeneral extends Usuario { ... }  // tabla USUARIO_GENERAL
@Entity public class Administrador  extends Usuario { ... }  // tabla ADMINISTRADOR
@Entity public class Funcionario    extends Usuario { ... }  // tabla FUNCIONARIO
```
Cuando Hibernate carga un `Funcionario`, hace un JOIN entre `USUARIO` y `FUNCIONARIO` automáticamente.

### 2. Repository (Repositorio)

Un **repository** es una interfaz que Spring implementa automáticamente. No se escribe SQL manual — Spring genera las queries a partir del nombre del método.

```java
public interface VentaRepository extends JpaRepository<Venta, Long> {
    // Spring genera: SELECT * FROM VENTA WHERE Mail_Comprador = ? ORDER BY Fecha DESC
    List<Venta> findByCompradorMailUsuarioOrderByFechaDesc(String mail);

    // @Query custom para calcular MontoTotal usando la vista de BD (DEC-03)
    @Query("SELECT SUM(e.costoHistorico) * (1 + v.comision.porcentaje / 100) " +
           "FROM Venta v JOIN v.entradas e WHERE v.ventaId = :id")
    BigDecimal calcularMontoTotal(@Param("id") Long ventaId);
}
```

**Métodos gratis de `JpaRepository`:** `save()`, `findById()`, `findAll()`, `delete()`, `existsById()`.

**`save()` vs `saveAndFlush()`:**
- `save()` encola el INSERT/UPDATE y lo envía al final de la transacción.
- `saveAndFlush()` lo envía **inmediatamente**, lo que fuerza al trigger a ejecutarse dentro del try/catch actual. Se usa cuando necesitamos capturar el error del trigger en el mismo bloque.

**Derived queries (queries del nombre):**
```java
// En TokenQrRepository:
Optional<TokenQr> findByCodigoQRAndActivoTrue(Long entradaId);
// Spring lee: findBy + CodigoQR (campo) + And + Activo (campo) + True (valor fijo)
// Genera: SELECT * FROM TOKEN_QR WHERE CodigoQR = ? AND Activo = TRUE
```

### 3. Service (Servicio)

Un **service** contiene la lógica de negocio. No sabe de HTTP — recibe datos, aplica reglas y devuelve resultados. Comentarios de clase en el código explican la responsabilidad de cada uno:

| Service | Responsabilidad |
|---|---|
| `AuthService` | Registro de USUARIO_GENERAL, login con validación, determinación de rol |
| `VentaService` | Flujo de compra: valida RNE 1, crea VENTA + ENTRADAs + TOKEN_QR, calcula MontoTotal |
| `TransferenciaService` | Valida RNE 2 y RNE 6, crea solicitud, delega máquina de estados a triggers |
| `ValidacionService` | Verifica token activo (RNE 9), dispositivo del funcionario (RNE 11), estado de entrada (RNE 7) |
| `AdminService` | Alta de estadios, sectores y eventos; usa `saveAndFlush` para capturar RNE 4 |
| `EntradaService` | Lista entradas del usuario con el CodigoQR del token activo |

**`@Transactional`:** Todo el método ocurre en una sola transacción de BD. Si algo falla, todos los cambios se revierten (rollback). Sin esto, un error a mitad de `comprar()` dejaría una VENTA sin ENTRADAs.

**`@Transactional(readOnly = true)`:** Optimización para lecturas — Spring configura la conexión como solo-lectura, lo que permite al driver y a la BD optimizar.

**`@Autowired` / constructor injection:** Spring inyecta automáticamente las instancias de repository. No se instancian con `new`.

### 4. Controller (Controlador)

Un **controller** recibe la petición HTTP, valida la sesión, llama al service y devuelve la respuesta. El comentario de clase de cada controller indica qué endpoints gestiona y quién puede accederlos.

```java
// Endpoints de ventas: compra de entradas (POST) e historial (GET).
// Ambos requieren rol USUARIO_GENERAL; la sesión HTTP identifica al comprador.
@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    // POST /api/ventas — compra de entradas (solo USUARIO_GENERAL)
    @PostMapping
    public ResponseEntity<?> comprar(@RequestBody CompraRequest req, HttpSession session) {
        String mail = SessionUtils.requireRol(session, "USUARIO_GENERAL"); // 401/403 si falla
        CompraResponse resp = ventaService.comprar(mail, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);       // 201
    }
}
```

**Anotaciones HTTP:**
- `@GetMapping` / `@PostMapping` / `@PutMapping("/{id}")` → método HTTP + patrón URL
- `@RequestBody` → deserializa JSON del cuerpo a objeto Java
- `@PathVariable` → extrae `{id}` de la URL
- `ResponseEntity<?>` → permite controlar código HTTP de respuesta (200, 201, 400, 401, 403, 404, 409...)

### 5. DTO (Data Transfer Object)

Un **DTO** define qué datos entran y salen de la API. Sin lógica — solo atributos. Separa la representación interna (entidad con relaciones JPA) de lo que se expone al cliente.

```java
// Lo que llega al hacer una compra:
public record CompraRequest(List<CompraItemRequest> items) {}
public record CompraItemRequest(Long eventoId, Long estadioId, String letraSector, int cantidad) {}

// Lo que devuelve el backend:
public record CompraResponse(Long ventaId, List<Long> entradaIds, BigDecimal montoTotal) {}
```

**¿Por qué no devolver la entidad directamente?** Las entidades JPA tienen relaciones `@OneToMany`/`@ManyToOne` que pueden causar referencias circulares al serializar a JSON. Los DTOs son objetos planos, sin sorpresas.

---

## Autenticación con sesiones HTTP

No usamos JWT — usamos **sesiones de servidor** (la forma clásica de Spring MVC).

```java
// Login: guarda mail y rol en la sesión
session.setAttribute("userMail", resp.mail());
session.setAttribute("userRol",  resp.rol());

// SessionUtils.requireRol(): valida y devuelve el mail, o lanza 401/403
public static String requireRol(HttpSession session, String rolRequerido) {
    String mail = (String) session.getAttribute("userMail");
    String rol  = (String) session.getAttribute("userRol");
    if (mail == null) throw new ResponseStatusException(UNAUTHORIZED, "No autenticado");
    if (!rol.equals(rolRequerido)) throw new ResponseStatusException(FORBIDDEN, "Acceso denegado");
    return mail;
}
```

Spring mantiene una cookie `JSESSIONID` en el navegador. Con cada petición, el navegador la envía → Spring recupera la sesión → encontramos mail y rol sin que el frontend los mande explícitamente.

---

## Cómo Spring detecta las clases

Al arrancar, Spring escanea todos los `.class` buscando anotaciones:
- `@Entity` → registra con Hibernate (gestión de BD)
- `@Repository` → registra como acceso a datos
- `@Service` → registra como lógica de negocio
- `@RestController` → registra como endpoint HTTP
- `@Component` → registro genérico (ej. `CorsConfig`)

Esto es **Inyección de Dependencias (IoC)**: Spring crea y conecta los objetos. Eso es lo que hace el constructor injection.

---

## Cómo se manejan los errores de los triggers

Cuando un trigger lanza `SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '[RNE 4] ...'`, llega como `DataIntegrityViolationException` de Hibernate. `SessionUtils.extractDbMessage()` extrae el texto útil:

```java
public static String extractDbMessage(Exception e) {
    String msg = e.getMessage();
    int idx = msg.indexOf("RNE");
    if (idx == -1) return "Error de base de datos";
    String extracted = msg.substring(idx);
    int end = extracted.indexOf("] [SQL");  // corta el sufijo técnico de Hibernate
    return end != -1 ? extracted.substring(0, end) : extracted;
}
```

El controller recibe ese string y lo devuelve como `ErrorResponse` con el código HTTP adecuado (409 para RNE 4, 404 para token inválido, etc.).

---

## Variables de entorno y conexión a la BD

```properties
# application.properties
spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=none    # Hibernate NO toca el schema — lo creamos nosotros con schema.sql
spring.jpa.hibernate.naming.physical-strategy=...StandardImpl  # usa nombres de columna exactos del @Column
```

Los valores vienen del archivo `.env` en la raíz del proyecto, que `TicketingApplication.java` lee al arrancar.

---

## Flujo completo de una compra (de clic a BD)

1. Usuario hace clic en "Confirmar" en `CompraModal`
2. `api.comprar({ items })` → `POST /api/ventas` con JSON
3. `VentaController.comprar()` — valida sesión (USUARIO_GENERAL), extrae mail
4. `VentaService.comprar()` (`@Transactional`):
   - Verifica items y total ≤ 5 (RNE 1 en backend)
   - Obtiene comisión vigente (`ComisionRepository.findVigente()`)
   - Crea `VENTA` (`ventaRepo.save(venta)`)
   - Por cada item: verifica `EVENTO_SECTOR`, obtiene precio de `SECTOR`, crea `ENTRADA` + `TOKEN_QR` (UUID aleatorio)
   - Si un trigger rechaza (ej. 6ta entrada), `DataAccessException` se captura y se propaga como `IllegalArgumentException`
   - Calcula `montoTotal = subtotal × (1 + porcentaje/100)`
5. Controller devuelve `201 Created` con `{ ventaId, entradaIds, montoTotal }`
6. Frontend muestra el resumen de compra

---

## Preguntas frecuentes de defensa

**¿Qué es una entidad JPA?**
Una clase Java con `@Entity` que Hibernate mapea a una tabla de BD. Cada instancia representa una fila. Hibernate genera el SQL de INSERT/UPDATE/SELECT automáticamente según las anotaciones.

**¿Qué es `@Transactional` y por qué es importante?**
Marca que un método debe ejecutarse en una sola transacción de BD. Si el método lanza una excepción no chequeada, todos los cambios se revierten (rollback). En `comprar()`, si falla al crear la tercera ENTRADA, las dos primeras y la VENTA se deshacen automáticamente.

**¿Por qué `saveAndFlush()` en lugar de `save()`?**
`save()` encola el SQL y lo envía al final de la transacción. `saveAndFlush()` lo envía inmediatamente, lo que fuerza a los triggers a ejecutarse *dentro* del bloque try/catch actual. Sin flush, el error del trigger llegaría fuera del catch y no podríamos capturar el mensaje de RNE 4.

**¿Qué diferencia hay entre Service y Controller?**
- **Controller**: sabe de HTTP (métodos, códigos de estado, cookies de sesión)
- **Service**: sabe de negocio (reglas, validaciones, cálculos, acceso a datos)
- Esta separación permite testear la lógica de negocio sin necesidad de un servidor HTTP.

**¿Qué es un DTO y por qué no devolver la entidad?**
DTO = Data Transfer Object. Define el contrato entre frontend y backend. Las entidades tienen relaciones JPA que generan referencias circulares al serializar. Los DTOs son registros planos, controlamos exactamente qué campos expone la API.

**¿Cómo funciona `InheritanceType.JOINED`?**
Crea una tabla por clase. `USUARIO` tiene columnas comunes. `FUNCIONARIO` tiene solo sus columnas propias y su PK es FK a `USUARIO`. Para cargar un Funcionario, Hibernate hace `SELECT u.*, f.* FROM USUARIO u JOIN FUNCIONARIO f ON u.Mail = f.Mail_Usuario`.

**¿Qué es CORS y por qué lo configuramos?**
Los navegadores bloquean peticiones de `localhost:3000` a `localhost:8080` por seguridad (diferentes puertos = diferentes orígenes). `CorsConfig.java` le dice al navegador que nuestro backend acepta esas peticiones, incluyendo cookies (`allowCredentials = true`).

**¿Cómo determina el backend el rol del usuario?**
`AuthService.determinarRol()` busca el mail en orden: primero en `ADMINISTRADOR`, luego en `FUNCIONARIO`, y si no está en ninguno devuelve `USUARIO_GENERAL` (por defecto). Esto es posible porque la herencia tabla-por-subclase garantiza que un mail solo existe en una subtabla.

**¿Qué pasa si dos usuarios intentan comprar la última entrada al mismo tiempo?**
El trigger `tr_entrada_limite_venta` hace `SELECT COUNT(*)` dentro del BEFORE INSERT. Si ambas transacciones pasan el check "simultáneamente" antes de que la otra haga commit, ambas podrían insertar. La mitigación correcta sería `SELECT ... FOR UPDATE` sobre la fila de VENTA; en el scope del obligatorio no se implementó, pero es un punto válido de defensa.
