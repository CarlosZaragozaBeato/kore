# 2026-11-13 — API de consulta y contexto para agentes

**Estado:** primera API local de solo lectura implementada y verificada.

## Cambios

- Se añadió `kore_api/app.py` con cuatro rutas v1: estado de ingestión, lista paginada, detalle con vueltas y contexto agregado por días.
- Los modelos de respuesta limitan los campos publicados. No exponen payloads originales, coordenadas ni las 1,8 millones de muestras; estas siguen en PostgreSQL para futuras consultas específicas.
- El contexto aporta duración, distancia, TSS total y cobertura de TSS por sesión, además de totales diarios y diez entrenamientos recientes. Son datos descriptivos, sin recomendaciones automáticas.
- Se guardó el contrato generado en `kore_api/openapi.json` y la guía de arranque en `kore_api/README.md`.

## Verificación

- Contra la base real, `status` devolvió 580 entrenamientos, 580 FIT y 580 decodificados.
- Lista, contexto y detalle devolvieron HTTP 200; el detalle incluyó vueltas. Una clave inexistente devolvió 404 y parámetros fuera de rango devolvieron 422.
- Diez pruebas locales correctas, incluidas las que revisan límites y que OpenAPI solo exponga operaciones GET sin `raw_payload` ni posiciones.

## Límites y siguiente paso

- La API solo debe escuchar en localhost hasta añadir autenticación.
- El contexto es descriptivo. Falta definir métricas de tendencia, calidad de datos y criterios para que un agente pueda razonar sobre carga y recuperación sin inferir información ausente.
- Falta programar la cadena de ingestión y comprobar una restauración completa de las copias de seguridad.
