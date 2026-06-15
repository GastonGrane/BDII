# Sistema de Ticketing — Mundial 2026

**Trabajo Obligatorio · Bases de Datos II · UCU**  
**Grupo 4:** Sharon Bentos · Gastón Grané · Axel Hernández

Stack: Java 21 + Spring Boot 3.3.5 · MySQL 8.x · React 18 + Vite

---

## Estructura del repositorio

```
/
├── backend/           Java/Spring Boot — API REST (puerto 8080)
│   ├── mvnw / mvnw.cmd   Maven Wrapper (no requiere Maven global)
│   └── src/
├── frontend/          React + Vite — interfaz de usuario (puerto 3000)
│   └── src/
├── sql/
│   ├── schema.sql     Creación de tablas y stored procedures
│   ├── triggers.sql   Triggers (RNE 4, 7, 11, …)
│   └── seed.sql       Datos de prueba para desarrollo
├── .env.example       Plantilla de variables de entorno
└── README.md
```

---

## 1. Requisitos previos

Se asume que tenés **Git** instalado. Instalá el resto con
[winget](https://learn.microsoft.com/es-es/windows/package-manager/winget/)
(incluido en Windows 10 ≥ 1709 y Windows 11).

### JDK 21

```powershell
# Instalar
winget install Microsoft.OpenJDK.21

# Verificar — debe mostrar "21.x.x"
java -version
```

> Reiniciá la terminal después de instalar para que `java` quede en el PATH.

### Node.js LTS (v20 o superior)

```powershell
# Instalar
winget install OpenJS.NodeJS.LTS

# Verificar
node --version   # v20.x o superior
npm --version
```

### MySQL 8.x

```powershell
# Instalar
winget install Oracle.MySQL

# Verificar
mysql --version   # 8.x.x
```

> Durante la instalación de MySQL el instalador pedirá una **contraseña para `root`**.
> Anotala — la vas a necesitar en los pasos siguientes.
>
> Si después de instalar el comando `mysql` no se reconoce, añadí
> `C:\Program Files\MySQL\MySQL Server 8.x\bin` a tu variable de entorno `PATH`.

---

## 2. Clonar el repositorio

```bash
git clone <URL-del-repositorio>
cd <carpeta-del-repo>
```

---

## 3. Configurar variables de entorno

```bash
# En Git Bash o PowerShell
cp .env.example .env
```

Abrí `.env` con cualquier editor y completá los valores para tu instalación local:

```dotenv
DB_HOST=localhost
DB_PORT=3306
DB_NAME=CD_Grupo4_local
DB_USER=root
DB_PASSWORD=tu_contraseña_de_root
```

> `.env` está en `.gitignore` y **nunca debe subirse al repositorio**.
> Cada integrante del equipo tiene su propio `.env` con su contraseña local.

---

## 4. Crear la base de datos local

Abrí **Git Bash** (viene incluido con Git for Windows) en la raíz del proyecto.

### 4a. Crear la base de datos vacía

```bash
mysql -u root -p -e "
  CREATE DATABASE IF NOT EXISTS CD_Grupo4_local
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;"
```

### 4b. Ejecutar los scripts en orden

Los archivos SQL referencian la base de producción (`USE CD_Grupo4;`).
El siguiente bloque la reemplaza por `CD_Grupo4_local` al vuelo:

```bash
SED='s/USE CD_Grupo4;/USE CD_Grupo4_local;/g'

sed "$SED" sql/schema.sql   | mysql -u root -p
sed "$SED" sql/triggers.sql | mysql -u root -p
sed "$SED" sql/seed.sql     | mysql -u root -p
```

> MySQL pedirá la contraseña de root cada vez. Si preferís evitar la confirmación
> repetida, podés usar la variable de entorno `MYSQL_PWD`:
>
> ```bash
> export MYSQL_PWD=tu_contraseña_de_root
> SED='s/USE CD_Grupo4;/USE CD_Grupo4_local;/g'
> sed "$SED" sql/schema.sql   | mysql -u root
> sed "$SED" sql/triggers.sql | mysql -u root
> sed "$SED" sql/seed.sql     | mysql -u root
> unset MYSQL_PWD
> ```

### 4c. Verificar el resultado

```bash
mysql -u root -p CD_Grupo4_local -e "
  SELECT 'USUARIO'    AS tabla, COUNT(*) AS filas FROM USUARIO
  UNION ALL SELECT 'EVENTO',     COUNT(*) FROM EVENTO
  UNION ALL SELECT 'ENTRADA',    COUNT(*) FROM ENTRADA
  UNION ALL SELECT 'TOKEN_QR',   COUNT(*) FROM TOKEN_QR
  UNION ALL SELECT 'DISPOSITIVO',COUNT(*) FROM DISPOSITIVO;"
```

Resultado esperado: 5 usuarios, 1 evento, 5 entradas, 5 tokens, 1 dispositivo.

### Resetear la BD a estado limpio

Para volver al estado inicial del seed (eliminar todos los datos de prueba acumulados):

```bash
mysql -u root -p -e "
  DROP DATABASE IF EXISTS CD_Grupo4_local;
  CREATE DATABASE CD_Grupo4_local
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

export MYSQL_PWD=tu_contraseña_de_root
SED='s/USE CD_Grupo4;/USE CD_Grupo4_local;/g'
sed "$SED" sql/schema.sql   | mysql -u root
sed "$SED" sql/triggers.sql | mysql -u root
sed "$SED" sql/seed.sql     | mysql -u root
unset MYSQL_PWD
```

---

## 5. Levantar el backend

El proyecto incluye **Maven Wrapper** (`mvnw` / `mvnw.cmd`), así que no necesitás
tener Maven instalado globalmente. La primera ejecución descarga Maven 3.9.9 (~10 MB).

Desde la carpeta `backend/`:

```bash
# Git Bash / macOS / Linux
cd backend
./mvnw spring-boot:run
```

```powershell
# PowerShell / CMD en Windows
cd backend
.\mvnw.cmd spring-boot:run
```

El backend queda disponible en **http://localhost:8080**.

> El archivo `.env` debe estar **un nivel arriba** de `backend/`
> (es decir, en la raíz del repositorio), que es donde Spring Boot lo busca.

### Alternativa: compilar el JAR y correrlo directamente

```bash
cd backend
./mvnw package -DskipTests
java -jar target/ticketing-0.0.1-SNAPSHOT.jar
```

Útil si `spring-boot:run` falla por conflictos con el proceso Maven.

---

## 6. Levantar el frontend

En **otra terminal**, desde la carpeta `frontend/`:

```bash
cd frontend
npm install      # instala dependencias — solo necesario la primera vez
npm run dev
```

El frontend queda disponible en **http://localhost:3000**.
Todas las llamadas a `/api/*` se redirigen automáticamente al backend en 8080.

---

## 7. Usuarios de prueba

| Email | Contraseña | Rol |
|---|---|---|
| `admin@ticketing.com` | `admin123` | ADMINISTRADOR |
| `func@ticketing.com` | `func123` | FUNCIONARIO |
| `user1@test.com` | `user123` | USUARIO_GENERAL |
| `user2@test.com` | `user123` | USUARIO_GENERAL |
| `user3@test.com` | `user123` | USUARIO_GENERAL |

### Datos pre-cargados por el seed

| Entidad | Detalle |
|---|---|
| Estadio | Estadio Centenario · Montevideo, Uruguay |
| Sectores | A ($150) · B ($120) · C ($200) · D ($300) |
| Evento | Uruguay vs Brasil · 20/6/2026 18:00 · sectores A y B |
| Entradas | 5 entradas Activas de `user1` en sector A |
| Tokens QR | `QR-E1-001` … `QR-E5-001` (uno por entrada) |
| Dispositivo | ID 1 · asignado a `func@ticketing.com` |

---

## Notas

- Las contraseñas se almacenan en texto plano (sin hash) por alcance de la demo (DEC-08).
- El flujo de pago no está implementado; las ventas nuevas quedan en estado `Pendiente` (DEC-08).
- Para correr la suite de tests de triggers: `mysql -u root -p CD_Grupo4_local < sql/test_triggers.sql`.
