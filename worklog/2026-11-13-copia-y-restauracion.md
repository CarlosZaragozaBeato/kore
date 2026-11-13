# 2026-11-13 — Copia y restauración aislada

**Estado:** copia y restauración verificadas; programación semanal activa.

## Cambios

- Se añadió una copia privada conjunta de PostgreSQL, respuestas originales, FIT y token rotado de Suunto; el `.env` queda fuera.
- La copia toma bloqueos de las operaciones de ingesta y guarda hashes y recuentos en un manifiesto.
- Se añadió una comprobación que restaura la base en un clúster temporal, compara recuentos y verifica los FIT frente a los hashes restaurados.
- El decodificador usa el mismo bloqueo de Suunto que los demás procesos para evitar cambios durante la copia.
- Se añadieron unidades `systemd --user` para crear y verificar una copia cada domingo a las 07:15.

## Verificación

- La copia nueva y su manifiesto se guardaron en `data/backups/`, fuera de Git.
- La restauración temporal reprodujo 580 entrenamientos, 580 sesiones, 7.056 vueltas y 1.813.756 muestras; validó el token y los hashes de los 580 FIT.
- Pasan las 11 pruebas existentes.
- `systemd-analyze verify` aceptó las unidades; el temporizador está habilitado y activo, con primera ejecución prevista para el 15 de noviembre de 2026 a las 07:15 CET.
- La carpeta de la copia tiene permisos `0700`; sus tres archivos, `0600`. La copia ocupa unos 79 MB y Git la ignora.

## Límite y siguiente paso

- Las copias permanecen en el mismo disco local. Una copia externa privada sigue siendo necesaria ante una avería del disco.
- Confirmar el primer ciclo programado y definir una política de retención y un destino privado fuera de este disco.
