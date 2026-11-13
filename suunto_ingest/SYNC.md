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

La primera ejecución consulta todo el histórico disponible; las siguientes vuelven a consultar una ventana de 24 horas por posibles modificaciones tardías. Cada página se guarda como JSON original en `data/raw/suunto/workouts/` y se incorpora a `suunto_workouts` usando `workoutKey` como clave. El cursor de `suunto_sync_state` solo avanza tras completar todas las páginas. Una interrupción obliga a repetir la ventana y no duplica filas. Un bloqueo de PostgreSQL impide dos sincronizaciones simultáneas.

Para descargar los FIT pendientes después de sincronizar los resúmenes:

```bash
KORE_DATABASE_URL=postgresql://kore_user:kore_password@127.0.0.1:55432/kore_db \
  .venv/bin/python -m suunto_ingest.fit --max-files 50
```

Los FIT se guardan en `data/raw/suunto/fit/` y su estado, tamaño y hash SHA-256 quedan en `suunto_workouts`. El proceso retoma únicamente los pendientes, limita el ritmo a una petición por segundo y espera antes de reintentar una vez si Suunto devuelve HTTP 401. Si vuelve a fallar, se detiene y conserva lo descargado. Suunto puede limitar las llamadas; no hay una causa confirmada para los 401 transitorios observados durante la carga inicial.

Para decodificar los FIT locales y consultar sus sesiones, vueltas y muestras en PostgreSQL:

```bash
KORE_DATABASE_URL=postgresql://kore_user:kore_password@127.0.0.1:55432/kore_db \
  .venv/bin/python -m suunto_ingest.decode --max-files 50
```

Las tablas son `suunto_fit_sessions`, `suunto_fit_laps` y `suunto_fit_records`. El decodificador marca el hash y su versión en `suunto_workouts`; vuelve a procesar un entrenamiento solo si cambia el FIT o el decodificador. Las coordenadas de `suunto_fit_records` están en grados. Los FIT originales permanecen intactos para reprocesarlos.

La base local, los JSON, los FIT y `data/secrets/suunto-token.json` contienen datos privados y requieren copia de seguridad. Se creó una copia inicial de la base y un archivo de originales más token en `data/backups/`; no se ha probado una restauración completa. No arranques el servicio Java anterior contra esta base: tiene configurado `drop-and-create`.

Pruebas:

```bash
.venv/bin/python -m unittest discover -s tests -v
```
