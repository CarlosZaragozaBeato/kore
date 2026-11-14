# Libro activo y enlace con Suunto

Kore mantiene un único libro operativo en `data/plans/active/plan-activo.xlsx`. Se crea desde la aplicación local; cada sustitución o escritura archiva antes una copia privada en `data/plans/archive/`.

## Contrato diario

- `Plan diario`: una fila por fecha. Las columnas planificadas se conservan; Suunto actualiza únicamente resultados y estado.
- `Plan semanal`: volumen previsto frente a resultados válidos disponibles.
- `Configuración`: parámetros, fechas, estado e identificador estable.
- `Suunto`: detalle de todas las actividades con la etiqueta exacta.
- `Reglas y decisiones` y `Decisiones de sesión`: entradas y motivo de cada recurso elegido.
- `Feedback` y `Adaptaciones`: aparecen cuando existen observaciones o cambios confirmados.

La etiqueta tiene formato `kore_<distancia>_<AAAAMMDD>`. Primero se exige coincidencia exacta y después se asigna la actividad según su inicio local en `Europe/Madrid`. La ausencia de coincidencia no se convierte en cero kilómetros ni descanso confirmado.

## Actualización

La cadena diaria completa termina actualizando el plan:

```bash
.venv/bin/python -m suunto_ingest.pipeline
```

Si la base ya está sincronizada:

```bash
.venv/bin/python -m plan_daily_sync
```

La actualización es idempotente: reconstruye resultados desde claves Suunto únicas, agrupa varias carreras del día y conserva las decisiones del plan. Un FIT pendiente o métricas incompletas quedan visibles en vez de producir totales parciales.

Los libros antiguos de demostración se conservan localmente bajo `data/plans/legacy/`; no participan en la aplicación ni en las pruebas.

## Finalizar o retirar

`http://127.0.0.1:8092/plans` muestra el activo y los archivos finalizados o retirados.

- Al ejecutar la primera sincronización posterior a la fecha final, Kore marca el libro como finalizado y lo mueve automáticamente a `data/plans/completed/`.
- **Finalizar bloque** permite cerrarlo manualmente antes, pero exige escribir su etiqueta exacta.
- **Retirar bloque activo** equivale a eliminarlo del flujo operativo: exige la misma confirmación y mueve el Excel intacto a `data/plans/discarded/`.
- Ninguna de estas acciones borra los archivos finalizados o retirados. El nombre conserva la etiqueta y la fecha del movimiento.

Después del movimiento deja de existir un activo y puede activarse otro borrador. `archive/` continúa reservado para copias previas a escrituras; no sustituye a `completed/` ni `discarded/`.
