# Limitaciones y opcionales

## Obligatorio cumplido

Todos los requisitos obligatorios de la letra están implementados o documentados con
evidencia. Ver la matriz en [03_CUMPLIMIENTO_LETRA.md](03_CUMPLIMIENTO_LETRA.md). En
particular, la corrección de esta rama cubre las piezas centrales que faltaban:

- Token dinámico de 30 segundos con vencimiento y rechazo de vencidos.
- Control de aforo (capacidad como límite duro) con concurrencia.
- Ranking de compradores y eventos más vendidos.
- Jurisdicción del administrador.
- Teléfonos múltiples.
- Cadena de custodia consultable.
- Altas de funcionarios, administradores, dispositivos y asignaciones desde la app.
- Cobertura de sectores por funcionario (RNE 5) como reporte.

## Opcionales implementados

- **Índices** de optimización física (FK y consultas frecuentes).
- **Comisión con histórico temporal** (más que el mínimo: permite variar la tasa en el
  tiempo conservando el histórico).
- **Reportes desde el panel de administrador** (eventos más vendidos, ranking de
  compradores, cobertura).

## Opcionales no implementados

- **Generación visual del QR**: el `codigoQR` se entrega y maneja como texto. La
  generación de la imagen QR puede agregarse en el frontend (p.ej. con una librería de QR)
  sin tocar la lógica de seguridad.
- **Escaneo real con cámara**: la validación se hace ingresando/pegando el `codigoQR`. El
  escaneo óptico es una extensión de frontend.
- **Docker / despliegue contenedorizado**: no incluido; el proyecto corre con MySQL local +
  `./mvnw` + `npm`.
- **Alta disponibilidad / escalabilidad horizontal**: fuera del alcance de un prototipo
  académico.
- **Reportes estadísticos avanzados** (más allá de los dos rankings y la cobertura).

## Limitaciones honestas

- **Contraseñas en texto plano**: por alcance del prototipo (DEC-08). Mejora futura:
  hashing con BCrypt vía Spring Security.
- **Autenticación por sesión HTTP** (sin JWT/OAuth): suficiente para la demo.
- **Duración de evento fija (4h)** para detectar solapamiento (DEC-05). Mejora futura:
  columna `FechaFin` real.
- **Regeneración del token bajo demanda** (no por job en segundo plano): decisión de diseño
  por volumen (DEC-02); el efecto funcional es el mismo.
- **MER en imagen**: el archivo `BDIIFifaSegundaVersion.drawio.png` es la base conceptual y
  debe adjuntarse en `docs/diagramas/`. La descripción textual del modelo está en
  [04_MODELO_DATOS.md](04_MODELO_DATOS.md).

## Próximos pasos

1. Adjuntar la imagen del MER y el informe PDF final en `docs/`.
2. Agregar generación visual de QR y, opcionalmente, escaneo por cámara.
3. Hashing de contraseñas (BCrypt) y endurecimiento de sesión.
4. `FechaFin` real en EVENTO y limpieza periódica de tokens inactivos.
5. Más reportes (recaudación por evento, ocupación por sector).
