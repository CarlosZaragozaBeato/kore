# Sincronización de entrenamientos

Instala la dependencia de PostgreSQL en un entorno local:

```bash
python -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
```

La base prevista para un entorno nuevo está en `infra/docker-compose.yml` y escucha solo en `127.0.0.1:5432`. El sincronizador utiliza esa conexión por defecto. También admite `KORE_DATABASE_URL` en el entorno o en `.env` si la base escucha en otro puerto.

En esta máquina Docker no estaba disponible para el usuario. Se preparó una instancia PostgreSQL 18 local en `data/postgres`, con su runtime en `data/postgres-runtime`, escuchando en `127.0.0.1:55432`. Ambos directorios están excluidos de Git. Para controlarla:

```bash
./infra/postgres-local.sh status
./infra/postgres-local.sh start
./infra/postgres-local.sh stop
```

Para sincronizar con esa instancia:

```bash
KORE_DATABASE_URL=postgresql://kore_user:kore_password@127.0.0.1:55432/kore_db \
  .venv/bin/python -m suunto_ingest.sync
```

Para ejecutar toda la cadena (resúmenes, FIT pendientes y decodificación) y registrar su resultado:

```bash
.venv/bin/python -m suunto_ingest.pipeline
```

Lee `KORE_DATABASE_URL` de `.env` o del entorno. La ejecución guarda cada intento en `suunto_pipeline_runs`, con hora de inicio y fin, etapa, contadores y tipo de error. Un intento interrumpido queda identificado al iniciar el siguiente. Si falla, los avances de cada etapa se conservan para reanudar. El endpoint `/api/v1/status` muestra el último intento y el último éxito.

En esta máquina se preparó `infra/systemd/kore-suunto-sync.service` y `.timer` para ejecutar la cadena cada día a las 06:30, hora local. El servicio asegura que la base local esté arrancada. Instalación de las unidades del repositorio:

```bash
systemctl --user link "$PWD/infra/systemd/kore-suunto-sync.service" "$PWD/infra/systemd/kore-suunto-sync.timer"
systemctl --user enable --now kore-suunto-sync.timer
systemctl --user list-timers kore-suunto-sync.timer
```

La unidad usa la ruta actual de este repositorio dentro de `~/Documents/develop`; si se mueve, hay que actualizarla. `Persistent=true` ejecuta una sincronización pendiente al volver a iniciar el gestor de usuario. El temporizador depende de la sesión de usuario mientras no se habilite `linger`.

La primera ejecución consulta todo el histórico disponible; las siguientes vuelven a consultar una ventana de 24 horas por posibles modificaciones tardías. Cada página se guarda como JSON original en `data/raw/suunto/workouts/` y se incorpora a `suunto_workouts` usando `workoutKey` como clave. El cursor de `suunto_sync_state` solo avanza tras completar todas las páginas. Una interrupción obliga a repetir la ventana y no duplica filas. Un bloqueo de PostgreSQL impide dos sincronizaciones simultáneas.

Para descargar los FIT pendientes después de sincronizar los resúmenes:

```bash
KORE_DATABASE_URL=postgresql://kore_user:kore_password@127.0.0.1:55432/kore_db \
  .venv/bin/python -m suunto_ingest.fit --max-files 50
```

Los FIT se guardan en `data/raw/suunto/fit/` y su estado, tamaño y hash SHA-256 quedan en `suunto_workouts`. El proceso retoma únicamente los pendientes, limita el ritmo a una petición por segundo y espera antes de reintentar una vez si Suunto devuelve HTTP 401. Si vuelve a fallar, se detiene y conserva lo descargado. Suunto puede limitar las llamadas; no hay una causa confirmada para los 401 transitorios observados durante la carga inicial.

Si Suunto cambia `lastModified` de un entrenamiento, la sincronización marca su FIT como pendiente. La siguiente descarga consulta de nuevo a Suunto aunque exista un archivo local anterior; solo lo sustituye después de recibir y validar el archivo nuevo. Hasta entonces, la API no usa sus métricas decodificadas como vigentes.

Para decodificar los FIT locales y consultar sus sesiones, vueltas y muestras en PostgreSQL:

```bash
KORE_DATABASE_URL=postgresql://kore_user:kore_password@127.0.0.1:55432/kore_db \
  .venv/bin/python -m suunto_ingest.decode --max-files 50
```

Las tablas son `suunto_fit_sessions`, `suunto_fit_laps` y `suunto_fit_records`. El decodificador marca el hash y su versión en `suunto_workouts`; vuelve a procesar un entrenamiento solo si cambia el FIT o el decodificador. Las coordenadas de `suunto_fit_records` están en grados. Los FIT originales permanecen intactos para reprocesarlos.

La base local, los JSON, los FIT y `data/secrets/suunto-token.json` contienen datos privados. Para crear una copia conjunta en `data/backups/`:

```bash
.venv/bin/python -m infra.backup_local create
```

Usa `create --verify` para ejecutar inmediatamente la prueba de restauración sobre la copia recién creada.

El comando espera a que no haya una operación Suunto en curso y crea `database.dump`, `raw-and-token.tar.gz` y `manifest.json` en una carpeta privada. La copia incluye el token rotado, pero no el `.env`. Para probar una copia sin tocar la base activa:

```bash
.venv/bin/python -m infra.backup_local verify data/backups/kore-<fecha>-<id>
```

La verificación comprueba los hashes, restaura la base en un clúster PostgreSQL temporal, compara los recuentos y contrasta cada FIT restaurado con el hash almacenado en la base. El clúster se elimina al terminar. Estas copias están en el mismo disco que la base: para protegerse de una avería del disco hay que guardarlas también en otro soporte privado. No arranques el servicio Java anterior contra esta base: tiene configurado `drop-and-create`.

En esta máquina, `infra/systemd/kore-backup.timer` ejecuta `create --verify` cada domingo a las 07:15. Para instalarlo:

```bash
systemctl --user link "$PWD/infra/systemd/kore-backup.service" "$PWD/infra/systemd/kore-backup.timer"
systemctl --user enable --now kore-backup.timer
```

El temporizador se recupera al iniciar sesión si se perdió una ejecución. No elimina copias antiguas automáticamente; revisa el espacio disponible y mueve copias privadas a otro soporte.

Pruebas:

```bash
.venv/bin/python -m unittest discover -s tests -v
```
