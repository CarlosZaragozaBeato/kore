# 2026-11-12 — Próximo paso de la integración con Suunto

**Estado:** primera extracción real completada. La sincronización histórica y la base local se implementaron después; ver `2026-11-12-sincronizacion-historica.md`.

## Comprobaciones realizadas

- El `.env` local contiene los cuatro valores esperados por el servicio existente. No se han mostrado ni copiado sus valores.
- La carga del JWT de refresh declara `PARTNER`, alcance `workout` y vencimiento en marzo de 2027. Se leyó localmente sin verificar la firma: esto no acredita que Suunto acepte el token.
- La documentación oficial indica que la API de entrenamientos necesita un access token y una subscription key. La lista de entrenamientos aporta resúmenes; FIT aporta muestras y vueltas. Fuentes: <https://apizone.suunto.com/how-to-start> y <https://apizone.suunto.com/faq>.
- El servicio Java actual usa `/v3/workouts`, pero no se ha confirmado aún que su contrato y parámetros sigan vigentes para esta cuenta.

## Primera implementación propuesta

1. Crear un cliente Python de línea de comandos con configuración local y errores claros. Mantener la integración separada de FastAPI y de los cálculos de entrenamiento.
2. Guardar el refresh token vigente en almacenamiento local ignorado por Git. En cada renovación, persistir de forma atómica el token devuelto antes de iniciar otra petición. No depender del valor inicial del `.env` tras la primera renovación.
3. Validar con una llamada real de alcance mínimo: renovar el token y solicitar una sola página de entrenamientos. Registrar únicamente estado HTTP, número de elementos, campos disponibles y tiempos; no registrar tokens ni respuestas completas en el worklog.
4. Guardar la respuesta original en almacenamiento local privado y registrar su procedencia. Después añadir importación idempotente en PostgreSQL, con clave de entrenamiento y cursor de sincronización. **Realizado:** ver `2026-11-12-sincronizacion-historica.md`.
5. Añadir recuperación tras fallos, margen de solapamiento para modificaciones tardías, paginación y descarga de FIT por entrenamiento. **Realizado:** ver `2026-11-12-sincronizacion-historica.md` y `2026-11-12-fit-historico.md`.

## Criterios de aceptación de la primera extracción

- Suunto acepta las credenciales y devuelve una página de entrenamientos, aunque esté vacía.
- El refresh token renovado sobrevive a un reinicio y los secretos no aparecen en Git ni en logs.
- La respuesta original queda guardada localmente sin sobrescribir datos previos.
- Se documentan el endpoint y los parámetros realmente aceptados por Suunto.

## Implementación y resultado

- Se añadió `suunto_ingest/probe.py`, un comando Python sin dependencias externas que renueva el token, persiste el nuevo refresh token antes de consultar la API y guarda una página original de entrenamientos en `data/raw/suunto/workouts/`. `data/` está excluido de Git.
- La consulta real del 12 de noviembre de 2026 a `/v3/workouts`, con una ventana de 30 días, `limit=1`, `offset=0` y filtro por modificación, terminó correctamente y devolvió un entrenamiento. La respuesta tenía `payload` y `metadata`, y no contenía un error de API.
- El token renovado y la respuesta original se guardaron con permisos `600`. No se registraron valores de entrenamiento, identificadores personales ni tokens en el worklog.
- Las dos pruebas locales verifican la persistencia y reutilización del token renovado, los permisos de los archivos y el comportamiento ante un payload inválido. Resultado: 2 pruebas correctas.
- Se añadió documentación de uso en `suunto_ingest/README.md`. Las futuras extracciones guardarán también un `.meta.json` con la ventana y los parámetros de consulta.

## Problemas y decisiones pendientes

- `/v3/workouts` quedó validado para la primera página de esta cuenta. Quedan por verificar paginación, límites y descarga FIT.
- Aún no hay una política de copias de seguridad para los datos locales ni para el token renovado.
- El alcance `workout` no permite asumir acceso a datos de sueño o recuperación; revisar permisos separados si se incorporan más adelante.
- No ejecutar el servicio Java actual para hacer la prueba: su configuración `drop-and-create` puede borrar datos locales.
