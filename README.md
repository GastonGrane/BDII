# Sistema de Ticketing — Mundial 2026

## Requisitos

- [Docker](https://docs.docker.com/get-docker/) y Docker Compose instalados.
- Puertos **80** (frontend) y **8080** (API) disponibles.

---

## Levantar el proyecto

```bash
git clone https://github.com/GastonGrane/BDII.git
cd BDII
docker compose up --build
```

La primera vez descarga imágenes y compila — puede tardar unos minutos.

| Servicio | URL |
|---|---|
| Frontend | http://localhost |
| API | http://localhost:8080/api |

---

## Usuarios de prueba

| Email | Contraseña | Rol |
|---|---|---|
| admin@ticketing.com | admin123 | ADMINISTRADOR |
| func@ticketing.com | func123 | FUNCIONARIO |
| user1@test.com | user123 | USUARIO_GENERAL |
| user2@test.com | user123 | USUARIO_GENERAL |
| user3@test.com | user123 | USUARIO_GENERAL |

---

## Estado de la compra (decisión funcional)

En esta implementación, **la compra de entrada se considera confirmada automáticamente
al completarse correctamente**. Al comprar, el sistema ya genera y entrega la entrada
(estado `Activa` + token QR), por lo que la venta se guarda directamente con estado
`Confirmada` y así aparece en "Mis compras".

- **No** se implementa pasarela de pago externa ni confirmación manual.
- El estado **`Pendiente`** queda **reservado** para futuros flujos donde exista pago
  externo, validación manual o confirmación diferida. Se mantiene en el modelo
  (enum `EstadoVenta` y `ENUM` de la tabla `VENTA`) por compatibilidad y documentación,
  pero hoy no es el estado de una compra finalizada con éxito.

Referencias en el código: `VentaService.comprar` (asigna `EstadoVenta.Confirmada`),
`entity/enums/EstadoVenta.java` y los badges de `frontend/src/pages/MisCosas.jsx`
(`Confirmada` en verde = estado correcto, `Pendiente` en amarillo = advertencia).

---

## Apagar

```bash
docker compose down
```

## Resetear datos (borrar BD y recargar seed)

```bash
docker compose down -v
docker compose up --build
```

---

## Desarrollo local (sin Docker)

Requiere Java 21, Node 20 y MariaDB/MySQL local.

**Base de datos** (cualquier OS):
```bash
mariadb -u root -p < sql/schema.sql
mariadb -u root -p < sql/triggers.sql
mariadb -u root -p < sql/seed.sql
```

**Backend — Linux/macOS:**
```bash
cd backend
export DB_HOST=localhost DB_PORT=3306 DB_NAME=CD_Grupo4 DB_USER=root DB_PASSWORD=tu_password
./mvnw spring-boot:run
```

**Backend — Windows (CMD):**
```cmd
cd backend
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=CD_Grupo4
set DB_USER=root
set DB_PASSWORD=tu_password
mvnw.cmd spring-boot:run
```

**Backend — Windows (PowerShell):**
```powershell
cd backend
$env:DB_HOST="localhost"; $env:DB_PORT="3306"; $env:DB_NAME="CD_Grupo4"; $env:DB_USER="root"; $env:DB_PASSWORD="tu_password"
./mvnw.cmd spring-boot:run
```

**Frontend** (cualquier OS):
```bash
cd frontend
npm install
npm run dev   # http://localhost:3000
```
