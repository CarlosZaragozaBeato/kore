# CCollector para agentes

Guía para que un agente de IA lea y escriba datos de CCollector. Todo es JSON
sobre HTTP; los datos son tuyos y portables.

## Empezar aquí

1. **Manifest** — `GET /api/v1/agent/manifest` (sin sesión): mapa de todos los
   recursos y cómo leerlos/escribirlos. Empieza siempre por aquí.
2. **OpenAPI** — `GET /q/openapi` (spec) y `/q/swagger-ui` (UI interactiva):
   contrato completo y tipado de cada endpoint.
3. **Contexto** — `GET /api/v1/agent/context` (con sesión): toda la sesión del
   usuario (mismo formato que el export) + el resumen analítico, en una llamada.

## Sesión

No hay autenticación real (la app es de uso propio y no se expone). Cada
petición que lee/escribe datos del usuario lleva el header:

```
X-CCollector-Username: <username>
```

Para crear/recuperar una sesión: `POST /api/v1/auth/login` con
`{"username": "..."}`.

## Convención de respuestas

La mayoría de endpoints envuelven la respuesta:

```json
{ "success": true, "data": { ... }, "error": null }
```

Excepciones (documento crudo, sin envoltorio): `GET /session/export`,
`GET /analytics/summary`, `GET /agent/manifest`, `GET /agent/context`.

## Recursos

Todos siguen el mismo patrón CRUD bajo `/api/v1` (ver el manifest para las rutas
exactas). El **POST de cada recurso sirve también para "importar"** un recurso
generado por un agente.

| Recurso | Ruta | Notas |
|---|---|---|
| Entrenamientos | `/workouts` | origen `MANUAL` o `SUUNTO`; ritmo derivado |
| Planes | `/plans` | sesiones planificadas + adherencia calculada |
| Ejercicios | `/exercises` | catálogo de gimnasio |
| Rutinas | `/routines` | ejercicios con series/reps/descanso |
| Sesiones de fuerza | `/strength-sessions` | registro de lo realizado |
| Recetas | `/recipes` | ingredientes, pasos, macros |
| Dietas | `/diet-plans` | objetivos + comidas por día |
| Analítica | `/analytics/summary` | solo lectura (totales + series + carga) |
| Ajustes Suunto | `/suunto/settings` | los secretos nunca se devuelven |

## Import / export de sesión completa

- **Export** — `GET /api/v1/session/export`: vuelca usuario + workouts + planes +
  gimnasio + nutrición a un único documento portable. No incluye ids de base de
  datos ni las credenciales de Suunto.
- **Import** — `POST /api/v1/session/import`: reconstruye una sesión desde ese
  documento en un dispositivo nuevo. Rechaza con 409 si el username ya existe.

### Versionado del esquema

El documento lleva `schemaVersion`. Versión actual: **4**.

| Versión | Añade |
|---|---|
| 1 | usuario + workouts |
| 2 | planes de entrenamiento |
| 3 | gimnasio (ejercicios, rutinas, sesiones de fuerza) |
| 4 | nutrición (recetas, dietas) |

Es **retrocompatible**: un documento de versión menor se importa sin las
secciones que no incluya. El import rechaza versiones mayores que la soportada.

## Flujos típicos de agente

- **Generar un plan**: `GET /agent/context` → razona sobre workouts y carga →
  `POST /api/v1/plans` con el plan generado (nombre, fechas, sesiones).
- **Analizar carga y sugerir descarga**: lee `analytics` del contexto (series
  semanal de `load`) → propone ajustes.
- **Proponer dieta**: según volumen/objetivo → `POST /api/v1/diet-plans` con
  objetivos y comidas; y `POST /api/v1/recipes` para las recetas nuevas.
- **Backup/restore o migración completa**: `GET /session/export` →
  `POST /session/import`.
