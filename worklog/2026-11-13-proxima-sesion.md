# 2026-11-13 — Evidencia para planificar la próxima sesión

**Estado:** implementado y verificado localmente.

## Alcance

El primer caso de uso es reunir evidencia para planificar una próxima sesión de carrera. El deporte se puede cambiar mediante un parámetro de consulta.

## Cambios

- Se añadió `/api/v1/agent/next-session/evidence` con dos ventanas consecutivas de días completos. La duración por defecto es siete días y se puede ajustar de 1 a 90.
- Cada ventana incluye totales y desglose por deporte con cobertura de TSS. Se incluye la última sesión general, la última del deporte objetivo y la vigencia global de los datos.
- La API se configuró como servicio de usuario limitado a `127.0.0.1:8080`.
- Se actualizó el contrato OpenAPI y la documentación.

## Verificación

- La consulta HTTP real respondió 200; con siete días por ventana había 13 sesiones recientes y 12 previas, ambas con cobertura TSS completa.
- Los recuentos de ambas ventanas coincidieron con los días completos del contexto de 15 días y con la suma del desglose por deporte.
- `systemd` informó el servicio activo y `ss` confirmó escucha solo en `127.0.0.1:8080`.
- Las 13 pruebas automatizadas pasan, incluida una comprobación del cambio horario de primavera en las ventanas completas.

## Límite y siguiente paso

- El endpoint entrega evidencia, no una prescripción. Antes de recomendar una próxima sesión hay que incorporar disponibilidad, preferencias y señales subjetivas como fatiga o molestias, además de revisar la vigencia de Suunto.
