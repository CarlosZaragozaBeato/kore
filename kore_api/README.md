# API local de entrenamiento

Arranque desde la raíz del repositorio, con PostgreSQL activo:

```bash
KORE_DATABASE_URL=postgresql://kore_user:kore_password@127.0.0.1:55432/kore_db \
  .venv/bin/uvicorn kore_api.app:app --host 127.0.0.1 --port 8080
```

La API es de solo lectura y sirve datos privados de entrenamiento. Escucha únicamente en localhost y todavía no tiene autenticación; no cambiar el host a `0.0.0.0` sin añadir control de acceso.

## Contrato v1

- `GET /api/v1/status`: número de entrenamientos, FIT descargados y FIT decodificados.
- `GET /api/v1/workouts?limit=20&offset=0`: lista paginada de resúmenes. `limit` admite 1–100.
- `GET /api/v1/workouts/{workout_key}`: resumen, cantidad de muestras y vueltas de una sesión; responde 404 si no existe.
- `GET /api/v1/agent/context?days=28`: totales y desglose diario de los últimos 1–365 días, más diez entrenamientos recientes. `tss_sessions` indica cuántas sesiones tienen TSS; un `tss_total` de cero no significa que todas tengan carga nula.

El contrato generado está en [openapi.json](openapi.json). FastAPI sirve documentación interactiva en `/docs` mientras la aplicación está activa. Las respuestas omiten el JSON original, las muestras individuales y las coordenadas. Esta capa entrega datos y métricas; todavía no genera recomendaciones de entrenamiento.
