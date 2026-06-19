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

---

## Probar los triggers y stored procedures

El archivo `sql/test_triggers.sql` es una suite de pruebas que valida los 13 triggers
y los 2 stored procedures contra los datos de `seed.sql`. Imprime una tabla con el
resultado (`PASS`/`FAIL`) de cada caso y un resumen final, y limpia sus objetos
temporales al terminar.

> ⚠️ Requiere la base ya cargada con `schema.sql` + `triggers.sql` + `seed.sql`
> (ver "Base de datos" más arriba). Conviene correrlo sobre datos recién cargados,
> porque consume el seed (genera transferencias, validaciones, etc.).

**Con Docker** (la base corre en el contenedor `db`, root password `root1234`):
```bash
docker compose exec -T db mariadb -u root -proot1234 CD_Grupo4 < sql/test_triggers.sql
```

**Local (MariaDB/MySQL):**
```bash
mariadb -u root -p < sql/test_triggers.sql
```

Para volver a un estado limpio antes de correrlo de nuevo, reseteá la BD
(`docker compose down -v && docker compose up --build`, o recargá los `.sql` a mano).
