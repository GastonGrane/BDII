# Índice de documentación — Sistema de Ticketing Mundial 2026

Grupo 4 · Bases de Datos II · UCU

Este es el mapa general de la documentación del proyecto. Cada documento cubre un
aspecto distinto del obligatorio: análisis, modelo de datos, reglas de negocio,
ejecución, defensa y trazabilidad de cumplimiento.

## Documentos

| Archivo | Para qué sirve |
|---|---|
| [01_FALTANTES_MAIN.md](01_FALTANTES_MAIN.md) | Auditoría de lo que faltaba en la rama `main` antes de esta corrección. |
| [02_CUMPLIMIENTO_RAMA.md](02_CUMPLIMIENTO_RAMA.md) | Qué se corrigió y agregó en la rama `correccion-bdii-ticketing-100`. |
| [03_CUMPLIMIENTO_LETRA.md](03_CUMPLIMIENTO_LETRA.md) | Matriz requisito por requisito contra la letra del obligatorio. |
| [04_MODELO_DATOS.md](04_MODELO_DATOS.md) | MER conceptual, modelo lógico y físico; respeto del MER del equipo; normalización. |
| [05_BITACORA_MODELADO.md](05_BITACORA_MODELADO.md) | Decisiones, supuestos, cardinalidades y restricciones no estructurales. |
| [06_REGLAS_NEGOCIO.md](06_REGLAS_NEGOCIO.md) | Reglas obligatorias, dónde se implementan y cómo se prueban. |
| [07_GUIA_EJECUCION.md](07_GUIA_EJECUCION.md) | Cómo correr el proyecto desde cero (orientado a Linux). |
| [08_GUIA_DEFENSA.md](08_GUIA_DEFENSA.md) | Cómo defender el proyecto oralmente y preguntas frecuentes. |
| [09_FLUJO_DEMO.md](09_FLUJO_DEMO.md) | Paso a paso para mostrar la demo completa en la defensa. |
| [10_ENDPOINTS_O_PRUEBAS.md](10_ENDPOINTS_O_PRUEBAS.md) | Endpoints REST con ejemplos cURL y respuestas esperadas. |
| [11_DECISIONES_TECNICAS.md](11_DECISIONES_TECNICAS.md) | Decisiones de implementación (motor, framework, transacciones, índices). |
| [12_LIMITACIONES_Y_OPCIONALES.md](12_LIMITACIONES_Y_OPCIONALES.md) | Opcionales implementados/no implementados y limitaciones honestas. |

## Documentación complementaria (preexistente)

| Archivo | Contenido |
|---|---|
| [decisiones.md](decisiones.md) | Registro de decisiones de diseño (DEC-01 … DEC-08). |
| [explicacion_sql.md](explicacion_sql.md) | Explicación del esquema y los triggers. |
| [explicacion_backend.md](explicacion_backend.md) | Explicación de la arquitectura del backend. |
| [explicacion_frontend.md](explicacion_frontend.md) | Explicación del frontend React. |
| [diagramas/](diagramas/) | Diagramas de arquitectura y de secuencia de compra. |

## Scripts de base de datos

| Archivo | Contenido |
|---|---|
| `../sql/schema.sql` | Creación de tablas, claves, restricciones e índices. |
| `../sql/triggers.sql` | Triggers, procedimientos y vistas de negocio. |
| `../sql/seed.sql` | Datos de prueba (usuarios, estadio, evento, ventas). |
| `../sql/test_triggers.sql` | Suite de pruebas de triggers y procedimientos. |

## Orden de lectura sugerido

1. `README.md` (raíz del repo) — visión general.
2. `00_INDICE.md` (este archivo).
3. `03_CUMPLIMIENTO_LETRA.md` — qué cumple el proyecto.
4. `04_MODELO_DATOS.md` — el modelo de datos y su justificación.
5. `09_FLUJO_DEMO.md` y `07_GUIA_EJECUCION.md` — para correr y mostrar la demo.
