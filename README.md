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

```bash
# Base de datos
mariadb -u root -p < sql/schema.sql
mariadb -u root -p < sql/triggers.sql
mariadb -u root -p < sql/seed.sql

# Backend
cd backend
export DB_HOST=localhost DB_PORT=3306 DB_NAME=CD_Grupo4 DB_USER=root DB_PASSWORD=tu_password
./mvnw spring-boot:run

# Frontend
cd frontend
npm install
npm run dev   # http://localhost:3000
```
