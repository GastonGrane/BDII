# Guía de ejecución (Linux)

Cómo correr el proyecto desde cero en Linux. En Windows los pasos son equivalentes
(ver `README.md`).

## 1. Requisitos previos

- **JDK 21** (`java -version` debe mostrar 21).
- **Node.js 20+** y `npm`.
- **MySQL 8.x** corriendo localmente.
- **Git**.

En Arch/Debian/Ubuntu se instalan con el gestor de paquetes correspondiente
(`pacman`, `apt`) o con [mise](https://mise.jdx.dev/) para Java/Node.

No hace falta instalar Maven: el proyecto incluye el **Maven Wrapper** (`./mvnw`), que
descarga Maven la primera vez (requiere conexión a internet).

## 2. Clonar y configurar variables de entorno

```bash
git clone <URL-del-repositorio>
cd BDII
cp .env.example .env
```

Editar `.env` con los datos de tu MySQL local:

```dotenv
DB_HOST=localhost
DB_PORT=3306
DB_NAME=CD_Grupo4_local
DB_USER=root
DB_PASSWORD=tu_contraseña
```

> `.env` está en `.gitignore` y no debe subirse.

## 3. Crear la base de datos y cargar los scripts

```bash
# Crear la base vacía
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS CD_Grupo4_local CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Cargar esquema, triggers y datos (los scripts dicen USE CD_Grupo4; lo reemplazamos al vuelo)
SED='s/USE CD_Grupo4;/USE CD_Grupo4_local;/g'
sed "$SED" sql/schema.sql   | mysql -u root -p CD_Grupo4_local
sed "$SED" sql/triggers.sql | mysql -u root -p CD_Grupo4_local
sed "$SED" sql/seed.sql     | mysql -u root -p CD_Grupo4_local
```

> Si MySQL bloquea la creación de funciones/triggers, habilitar una vez:
> `SET GLOBAL log_bin_trust_function_creators = 1;`

Verificar:

```bash
mysql -u root -p CD_Grupo4_local -e "
  SELECT 'USUARIO' t, COUNT(*) n FROM USUARIO
  UNION ALL SELECT 'EVENTO', COUNT(*) FROM EVENTO
  UNION ALL SELECT 'ENTRADA', COUNT(*) FROM ENTRADA
  UNION ALL SELECT 'TOKEN_QR', COUNT(*) FROM TOKEN_QR;"
```

Esperado: 5 usuarios, 2 eventos, 10 entradas, 10 tokens.

## 4. (Opcional) Ejecutar la suite de pruebas de triggers

```bash
sed 's/USE CD_Grupo4;/USE CD_Grupo4_local;/g' sql/test_triggers.sql | mysql -u root -p CD_Grupo4_local
```

Muestra una tabla con PASS/FAIL por cada regla probada.

## 5. Levantar el backend

```bash
cd backend
./mvnw spring-boot:run
```

Queda en `http://localhost:8080`. El `.env` debe estar en la raíz del repo (un nivel
arriba de `backend/`).

Alternativa (compilar el JAR):

```bash
cd backend
./mvnw package -DskipTests
java -jar target/ticketing-0.0.1-SNAPSHOT.jar
```

## 6. Levantar el frontend

En otra terminal:

```bash
cd frontend
npm install
npm run dev
```

Queda en `http://localhost:3000` (las llamadas `/api/*` se redirigen al backend).

## 7. Usuarios de prueba (cargados por el seed)

| Email | Contraseña | Rol |
|---|---|---|
| `admin@ticketing.com` | `admin123` | ADMINISTRADOR (PaisSede: Uruguay) |
| `func@ticketing.com` | `func123` | FUNCIONARIO (legajo LEG-001) |
| `user1@test.com` | `user123` | USUARIO_GENERAL |
| `user2@test.com` | `user123` | USUARIO_GENERAL |
| `user3@test.com` | `user123` | USUARIO_GENERAL |

## 8. Problemas comunes

- **`./mvnw: Permission denied`** → `chmod +x backend/mvnw`.
- **El backend no encuentra la BD** → revisar `.env` y que MySQL esté corriendo.
- **Triggers no se crean** → habilitar `log_bin_trust_function_creators` (ver paso 3).
- **Token “vencido” al validar** → es correcto: el token dura 30s. Abrir “Mis entradas”
  para obtener el token vigente y validar dentro de la ventana.
