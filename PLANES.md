# Planes diarios de Kore

`plan_excel.py` genera cuatro libros base privados en `data/plans/defaults/` y mantiene **un solo libro activo** en `data/plans/active/plan-activo.xlsx`. Al reemplazar un libro, guarda antes una copia en `data/plans/archive/`. Todos los XLSX y los perfiles quedan fuera de Git; el generador y las pruebas se versionan.

## Crear y activar

```bash
.venv/bin/python plan_excel.py defaults
.venv/bin/python plan_excel.py activate 10k --start 2026-11-16 --no-race
.venv/bin/python plan_excel.py update
```

`defaults` crea ciclos ilustrativos de 10, 12, 16 y 22 semanas para 5, 10, 21 y 42 km. Sus fechas sirven para leer la secuencia; no son fechas de inicio confirmadas y no tienen etiqueta activa. `activate` requiere una fecha real de inicio en lunes y una modalidad elegida por el usuario. `--no-race` convierte la última semana en una evaluación opcional. El libro activo local existente es una copia de 10 km generada durante el desarrollo; debe tratarse como provisional hasta que el usuario confirme sus parámetros.

`data/plans/examples/demo-enlace-diario-10k.xlsx` es una demostración **sintética y separada** del activo: muestra lunes 16, recovery 10 km previstos y una carrera ficticia etiquetada de 9,8 km. No se debe importar como entrenamiento real.

## Hojas y enlace diario

- `Plan diario` es la hoja principal: **una fila por fecha**, agrupada por semana y bloque. Las columnas A–I, S y U–X guardan lo previsto; J–R muestran resultados. T permite confirmar manualmente un día de descanso. U identifica tres sesiones clave por semana y V–X indican dosis mínima, prevista y máxima. La primera fila del bloque activo dice `lunes 16/11/2026 | Recovery | 10 km`.
- `Plan semanal` resume los valores calculados desde las filas diarias. Los kilómetros reales siguen vacíos si no hay actividad válida; una ausencia no se convierte en cero.
- `Configuración` guarda modalidad, inicio real, final derivado, semanas, identificador y etiqueta estable `kore_<modalidad>_<inicio>`, estado, día/semana actual y días restantes. El progreso se calcula respecto a la fecha de inicio, en `Europe/Madrid`.
- `Suunto` conserva el detalle de **todas** las actividades etiquetadas, incluidas varias el mismo día.
- `Ritmos` muestra una tabla orientativa y versiones. El valor individual, su fuente y fecha se configuran únicamente en el libro privado.

`update` consulta la base PostgreSQL local en modo de solo lectura. Usa la misma condición de frescura FIT del exportador existente en la rama `codex/excel-export`: sesión 0, `fit_status=ok`, hash y versión de parser vigentes. Primero exige la **etiqueta exacta** del bloque dentro de `userTags` y luego sitúa cada actividad en el día de su inicio local, no en el día UTC ni en la semana solamente. Agrupa las carreras decodificadas del día, calcula distancia, tiempo, ritmo, FC ponderada por tiempo y TSS disponible. La hoja `Suunto` conserva cada actividad con su clave. Una actualización repetida reconstruye los resultados a partir de claves únicas y deja intactas las columnas planificadas.

Estados diarios: `Pendiente` para fechas futuras; `Sin actividad etiquetada` cuando no hay coincidencia en una fecha de carrera pasada; `Descanso sin confirmar` para un descanso sin coincidencia; `Descanso cumplido` solo si se marca `Descanso confirmado` en la columna T; `FIT pendiente` cuando hay coincidencia sin FIT vigente; `Métricas incompletas` si falta distancia o tiempo de alguna carrera decodificada; `Actividad etiquetada` con carrera decodificada; `Otro deporte etiquetado` si solo hay otro deporte. No se infiere que no se entrenó por falta de etiqueta o sincronización. Un TSS ausente deja vacío el total diario, aunque el detalle conserva los TSS disponibles.

Los mínimos semanales se leen desde `data/planning/config.json`, fuera de Git, y **no son obligaciones para cada semana**. Adaptación, descarga y evaluación pueden quedar por debajo. El estado subjetivo también debe actualizarse antes de activar o modificar un bloque.

Cada semana tiene tres sesiones **estructuradas**, martes, jueves y domingo, con calentamiento y vuelta a la calma incluidos dentro de la distancia total. No significa tres esfuerzos máximos: la primera semana y las descargas emplean dosis cortas o controladas. Miércoles añade strides si hay frescura; lunes recovery, sábado fácil y viernes descanso. La semana 7 del 10 km contiene como dosis prevista `6×1 km`, `8 km progresivos` y `3×2 km` dentro de la tirada. Los techos de la columna X son límites de decisión, no una invitación a alcanzarlos. El esfuerzo de competición sigue por calibrar con una prueba real. Una [revisión de entrenamiento en corredores de fondo](https://pubmed.ncbi.nlm.nih.gov/35418513/) describe el uso de sesiones de umbral e intervalos con días fáciles entre cargas; este patrón orienta la distribución, sin demostrar que tres sesiones intensas sean adecuadas para una persona concreta.

## Ritmos

La hoja de ritmos usa una ecuación de consumo de oxígeno en llano, `VO₂ ≈ 0,2 × velocidad (m/min) + 3,5`, y fracciones amplias para cada tipo de sesión. Es una **aproximación**, no una medición de economía de carrera, umbral, VDOT ni ritmo de competición. Cambiar el valor editable en `Ritmos!B1` recalcula la tabla general; no modifica automáticamente las prescripciones anteriores ni los resultados. Al revisar ritmos futuros, registrar nueva versión y observación fechada.

Referencias: [ecuación ACSM](https://www.acsm.org/docs/default-source/event-documents/annual-meeting-documents/abstracts/2019/2019-new/acsm19_abstracts_f.pdf), [economía de carrera](https://pubmed.ncbi.nlm.nih.gov/27747844/), [cambios bruscos en la sesión más larga](https://bjsm.bmj.com/content/early/2025/07/07/bjsports-2024-109380).
