# API local de entrenamiento

Arranque desde la raíz del repositorio, con PostgreSQL activo:

```bash
KORE_DATABASE_URL=postgresql://kore_user:kore_password@127.0.0.1:55432/kore_db \
  .venv/bin/uvicorn kore_api.app:app --host 127.0.0.1 --port 8080
```

La API es de solo lectura y sirve datos privados de entrenamiento. Escucha únicamente en localhost y todavía no tiene autenticación; no cambiar el host a `0.0.0.0` sin añadir control de acceso.

## Contrato v1

- `GET /api/v1/status`: número de entrenamientos, FIT vigentes y FIT decodificados con la versión actual del parser; último intento de la cadena de ingesta y hora del último éxito, si existen.
- `GET /api/v1/workouts?limit=20&offset=0`: lista paginada de resúmenes. `limit` admite 1–100.
- `GET /api/v1/workouts/{workout_key}`: resumen, cantidad de muestras y vueltas de una sesión; responde 404 si no existe.
- `GET /api/v1/agent/context?days=28`: últimos 1–365 días de calendario en `Europe/Madrid`, incluyendo hoy. Devuelve todos los días, también los de descanso, agregados semanales, desglose por deporte, diez entrenamientos recientes y cobertura de TSS (`tss_sessions` y `tss_coverage`). Un `tss_total` de cero no significa que todas las sesiones tengan carga nula.
- `GET /api/v1/agent/next-session/evidence?target_sport=running&window_days=7`: dos ventanas consecutivas de días completos, la reciente y la anterior. Devuelve sesiones, duración, distancia, TSS y cobertura por deporte, la última sesión de cualquier deporte y la última del deporte objetivo. `window_days` admite 1–90 y el deporte objetivo por defecto es `running`. Incluye `availability` para conocer la vigencia de los datos.

Cada día incluye `partial` para señalar que hoy aún no ha terminado. Cada semana incluye `complete`, que solo es verdadero si sus siete días han terminado y están dentro de la ventana solicitada. El desglose `sports` incluye sesiones, duración, distancia, TSS y número de sesiones con TSS, frecuencia cardíaca media o potencia media. Las cifras de TSS se exponen tal como vienen del FIT; la API no infiere fatiga ni prescribe entrenamiento.

`availability` indica el instante de consulta, el último resultado y éxito de la cadena de ingesta, las fechas del último resumen y del último FIT vigente decodificado y cuántos FIT siguen pendientes, no disponibles o pendientes de decodificación. Estos contadores de FIT cubren toda la base, no solo la ventana elegida. Un día sin sesiones puede representar descanso o datos aún no sincronizados; consulta `availability` antes de interpretarlo.

El contrato generado está en [openapi.json](openapi.json). FastAPI sirve documentación interactiva en `/docs` mientras la aplicación está activa. Las respuestas omiten el JSON original, las muestras individuales y las coordenadas. Las métricas derivadas de un FIT pendiente de actualización quedan fuera del contexto hasta que se vuelva a descargar y decodificar. Esta capa entrega datos y métricas; todavía no genera recomendaciones de entrenamiento.

La comparación para la próxima sesión excluye el día de hoy porque puede estar incompleto. La última sesión sí puede ser de hoy. Las ventanas siempre usan días de calendario en `Europe/Madrid`, incluso durante los cambios horarios. El endpoint ofrece evidencia para planificar una sesión, sin convertir diferencias de volumen o TSS en una prescripción.

En esta máquina, la API local está configurada como servicio de usuario. Para instalarlo desde la raíz del repositorio:

```bash
systemctl --user link "$PWD/infra/systemd/kore-api.service"
systemctl --user enable --now kore-api.service
systemctl --user status kore-api.service
```

El servicio arranca PostgreSQL local si es necesario y escucha solo en `127.0.0.1:8080`. Con `Linger=no`, se inicia al abrir una sesión de usuario; no queda activo tras cerrar la última sesión.
