# Sistema de Ticketing — Mundial 2026

**Trabajo Obligatorio · Bases de Datos II · UCU**
**Grupo 4:** Sharon Bentos · Gastón Grané · Axel Hernández

## Descripción

Demo cliente/servidor de un sistema integral de ticketing para los partidos del Mundial
2026. Permite comercializar, transferir y validar entradas. La entrada es **dinámica**: no
es una imagen estática, sino un activo digital con un dueño que puede cambiar por
transferencia y un **token que muta cada 30 segundos** para evitar fraude por captura de
pantalla, manteniendo la cadena histórica de custodia.

## Qué problema resuelve

- Venta centralizada de entradas con comisión, límite de 5 por transacción y control de
  aforo (capacidad como límite duro).
- Separación entre **comprador original** y **propietario actual**, con transferencias que
  requieren aceptación (máximo 3 por entrada antes de validarse).
- Validación de ingreso con **token dinámico**, **dispositivo autorizado** y **funcionario**,
  con consumo irreversible y sin doble validación.
- Roles con permisos: administrador por país sede, funcionario de validación y usuario
  general.

## Tecnologías

- **Backend:** Java 21 + Spring Boot 3.3.5 (API REST, puerto 8080).
- **Base de datos:** MySQL 8.x (InnoDB), esquema y lógica en SQL (triggers, procedimientos,
  vistas). `ddl-auto=none`: el esquema lo crean los scripts, no Hibernate.
- **Frontend:** React 18 + Vite (puerto 3000).

## Estructura del repositorio

```
/
├── backend/      API REST en Java/Spring Boot (incluye Maven Wrapper ./mvnw)
├── frontend/     Interfaz React + Vite
├── sql/
│   ├── schema.sql        Tablas, claves, restricciones, índices, vistas
│   ├── triggers.sql      Triggers, procedimientos y vistas de negocio
│   ├── seed.sql          Datos de prueba
│   └── test_triggers.sql Suite de pruebas de triggers
├── docs/         Documentación del obligatorio (ver abajo)
├── .env.example  Plantilla de variables de entorno
└── README.md
```

## Documentación principal

| Documento | Contenido |
|---|---|
| [docs/00_INDICE.md](docs/00_INDICE.md) | Mapa de toda la documentación. |
| [docs/01_FALTANTES_MAIN.md](docs/01_FALTANTES_MAIN.md) | Auditoría de lo que faltaba en `main`. |
| [docs/02_CUMPLIMIENTO_RAMA.md](docs/02_CUMPLIMIENTO_RAMA.md) | Qué corrige/agrega la rama de corrección. |
| [docs/03_CUMPLIMIENTO_LETRA.md](docs/03_CUMPLIMIENTO_LETRA.md) | Matriz requisito por requisito. |
| [docs/04_MODELO_DATOS.md](docs/04_MODELO_DATOS.md) | MER conceptual, modelo lógico y físico; respeto del MER. |
| [docs/05_BITACORA_MODELADO.md](docs/05_BITACORA_MODELADO.md) | Decisiones, supuestos, cardinalidades, restricciones. |
| [docs/06_REGLAS_NEGOCIO.md](docs/06_REGLAS_NEGOCIO.md) | Reglas obligatorias y su implementación. |
| [docs/07_GUIA_EJECUCION.md](docs/07_GUIA_EJECUCION.md) | Cómo correr el proyecto (Linux). |
| [docs/08_GUIA_DEFENSA.md](docs/08_GUIA_DEFENSA.md) | Cómo defender el proyecto oralmente. |
| [docs/09_FLUJO_DEMO.md](docs/09_FLUJO_DEMO.md) | Paso a paso de la demo. |
| [docs/10_ENDPOINTS_O_PRUEBAS.md](docs/10_ENDPOINTS_O_PRUEBAS.md) | Endpoints REST con ejemplos cURL. |
| [docs/11_DECISIONES_TECNICAS.md](docs/11_DECISIONES_TECNICAS.md) | Decisiones de implementación. |
| [docs/12_LIMITACIONES_Y_OPCIONALES.md](docs/12_LIMITACIONES_Y_OPCIONALES.md) | Opcionales y limitaciones honestas. |

## Cómo ejecutar (resumen)

Requisitos: JDK 21, Node 20+, MySQL 8, Git. Guía completa en
[docs/07_GUIA_EJECUCION.md](docs/07_GUIA_EJECUCION.md).

```bash
# 1. Variables de entorno
cp .env.example .env   # completar DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD

# 2. Base de datos (crear y cargar scripts)
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS CD_Grupo4_local CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
SED='s/USE CD_Grupo4;/USE CD_Grupo4_local;/g'
sed "$SED" sql/schema.sql   | mysql -u root -p CD_Grupo4_local
sed "$SED" sql/triggers.sql | mysql -u root -p CD_Grupo4_local
sed "$SED" sql/seed.sql     | mysql -u root -p CD_Grupo4_local

# 3. Backend
cd backend && ./mvnw spring-boot:run     # http://localhost:8080

# 4. Frontend (otra terminal)
cd frontend && npm install && npm run dev # http://localhost:3000
```

## Scripts SQL

En la carpeta `sql/`: `schema.sql` (estructura), `triggers.sql` (reglas en BD),
`seed.sql` (datos de prueba) y `test_triggers.sql` (pruebas). Se ejecutan en ese orden.

## Usuarios de prueba

| Email | Contraseña | Rol |
|---|---|---|
| `admin@ticketing.com` | `admin123` | ADMINISTRADOR (PaisSede: Uruguay) |
| `func@ticketing.com` | `func123` | FUNCIONARIO (legajo LEG-001) |
| `user1@test.com` | `user123` | USUARIO_GENERAL |
| `user2@test.com` | `user123` | USUARIO_GENERAL |
| `user3@test.com` | `user123` | USUARIO_GENERAL |

Datos precargados: 1 estadio (Centenario) con sectores A/B/C/D, 2 eventos, ventas de
ejemplo para user1/user2/user3, 1 dispositivo y tokens por entrada.

## Cómo probar los endpoints

Ejemplos de cURL para todo el flujo en
[docs/10_ENDPOINTS_O_PRUEBAS.md](docs/10_ENDPOINTS_O_PRUEBAS.md).

## Estado del proyecto

Prototipo funcional con todos los requisitos obligatorios implementados o documentados.
La matriz de cumplimiento está en [docs/03_CUMPLIMIENTO_LETRA.md](docs/03_CUMPLIMIENTO_LETRA.md).
La rama de corrección es `correccion-bdii-ticketing-100`.

## Notas

- Las contraseñas se almacenan en texto plano por alcance de la demo.
- El token vence a los 30 segundos: para validar, usar el token vigente que muestra
  "Mis entradas" o `GET /api/entradas/{id}/token`.
- El MER conceptual del equipo (`BDIIFifaSegundaVersion.drawio.png`) es la base del modelo;
  su descripción textual está en [docs/04_MODELO_DATOS.md](docs/04_MODELO_DATOS.md).
