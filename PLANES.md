# Planes Excel y datos de Suunto

El agente de planificación decide la duración y el contenido de cada bloque. Kore crea plantillas editables, extrae la evidencia histórica y añade los entrenamientos reales a una copia del plan.

## Objetivos locales

Los kilómetros mínimos semanales declarados se guardan solo en `data/planning/config.json`, fuera de Git. La estructura está en [planning-config.example.json](planning-config.example.json); copia ese archivo y sustituye cada `null` por el valor correspondiente. Las cifras son objetivos indicados por el usuario, no una recomendación calculada por Kore.

## Crear plantillas

```bash
.venv/bin/python plan_excel.py create --start 2026-11-09 --weeks 52
```

Se crean cuatro libros en `data/plans/templates/`, uno para 5k, 10k, 21k y 42k. `--start` debe ser lunes. Las 52 filas son capacidad editable, no la duración óptima del plan. El agente rellena en `Plan semanal` las columnas Bloque, Objetivo, Km planificados, Sesiones planificadas y Notas. La hoja `Configuración` conserva el objetivo local y la etiqueta del plan.

## Etiquetar actividades y actualizar un plan

Cada plantilla propone una etiqueta única, por ejemplo `kore_5k_20261109`. [Suunto permite añadir etiquetas personalizadas](https://www.suunto.com/es-es/Asistencia/faq-articles/suunto-app/how-can-i-use-tags-in-suunto-app/) desde la edición de una actividad. La fecha de la actividad determina la semana del plan; la etiqueta identifica a qué plan pertenece.

Después de etiquetar actividades, sincroniza Suunto y genera una copia del libro con los datos reales:

```bash
.venv/bin/python -m suunto_ingest.pipeline
.venv/bin/python plan_excel.py update data/plans/templates/plan-5k-2026-11-09.xlsx --match tag
```

`--match tag` solo incorpora actividades que tengan exactamente la etiqueta del plan en `userTags`. `--match date` incorpora todas las actividades de las semanas fechadas y sirve antes de empezar a usar etiquetas. El comando crea un nuevo libro en `data/plans/updated/`: deja el original intacto, añade la hoja `Suunto` con cada entrenamiento enlazado a semana y bloque, y rellena las columnas de kilómetros, sesiones y estado real. Las semanas futuras quedan sin resultados; la semana actual figura como «En curso». Si cambia una etiqueta en Suunto, comprueba que aparezca en el siguiente resumen sincronizado antes de confiar en el modo por etiqueta; esa propagación aún no se ha probado con una edición nueva.

En modo por etiqueta, una semana sin actividades coincidentes queda con las métricas reales vacías y el estado «Sin actividades etiquetadas». Esto no significa que no se haya entrenado; puede faltar la etiqueta o la sincronización.

## Análisis histórico para el agente

```bash
.venv/bin/python analysis_training.py
```

Se crea un Excel privado en `data/analysis/` con todas las semanas de carrera, sesiones individuales, vueltas, desnivel, otros deportes, cobertura de TSS y comparación con los objetivos locales. El archivo `data/analysis/agent-context.md` recoge los datos agregados y las instrucciones para el agente que diseñará los planes. No se exportan registros por segundo ni coordenadas. Tanto los libros como el contexto son privados y están excluidos de Git.
