# 2026-11-13 — Cadena de ingesta y programación

**Estado:** implementación activa y verificada localmente.

## Cambios

- Se añadió un comando único para sincronizar resúmenes, descargar todos los FIT pendientes y decodificar los archivos vigentes por lotes.
- Cada intento queda en `suunto_pipeline_runs` con etapas, contadores, hora y tipo de fallo, sin guardar respuestas ni credenciales.
- `/api/v1/status` muestra el intento más reciente y la hora del último éxito.
- Se añadieron unidades `systemd --user` para ejecución diaria a las 06:30 y un comando idempotente para arrancar PostgreSQL local cuando sea necesario.
- Se configuró la URL de la base local en el `.env` ignorado por Git.

## Verificación

- Las 11 pruebas locales pasan y `systemd-analyze verify` acepta ambas unidades.
- La primera ejecución completa contra Suunto terminó correctamente: tres resúmenes en la ventana consultada, cero FIT pendientes y cero decodificaciones pendientes.
- `/api/v1/status` informó de 580 entrenamientos, 580 FIT vigentes, 580 decodificados, último intento correcto y hora de último éxito.
- El temporizador quedó activo; su próxima ejecución figura para el 14 de noviembre de 2026 a las 06:30 CET.
- `Linger=no`: el gestor de usuario no permanece activo sin una sesión iniciada. `Persistent=true` recupera una ejecución perdida cuando vuelva a iniciarse.

## Siguiente paso

- Observar la primera ejecución programada. Después, valorar una copia de seguridad con restauración probada antes de ampliar el consumo de datos.
