# Kore: instrucciones para agentes

## Objetivo

Kore debe extraer la información de entrenamiento de Suunto, conservarla en una base de datos local y prepararla para consulta, visualización y análisis por un agente que ayude a tomar decisiones sobre entrenamientos.

Prioridades actuales, en este orden:
1. Extracción fiable y recuperable de los datos de Suunto.
2. Conservación local de los datos originales y de los datos estructurados.
3. Procesos de tratamiento y consultas útiles para visualización y agentes.

El backend es Python con PostgreSQL local y FastAPI. No introducir Java. El prototipo Quarkus se retiró del árbol vigente y permanece recuperable en el historial Git. Registrar en `worklog/` las decisiones y cualquier cambio de dirección.

## Antes y después de trabajar

- Leer el `AGENTS.md` de la carpeta superior y comprobar `git status` antes de modificar el repositorio. Respetar los cambios existentes.
- Registrar cada implementación, avance, problema y decisión relevante en `worklog/`, con fecha, evidencia, estado y siguiente paso. Actualizar una entrada cuando se resuelva un problema.
- Mantener las entradas útiles para continuar el trabajo: qué se cambió, por qué, cómo se verificó y qué queda pendiente. No registrar suposiciones como hechos comprobados.
- No incluir credenciales, tokens, datos personales, respuestas completas de Suunto ni archivos FIT en el repositorio o en el worklog. El `.env` raíz es local y está excluido de Git.
- Antes de considerar fiable la extracción, verificar el acceso real a Suunto, la persistencia de la rotación del refresh token, la reanudación tras fallos y que la base local conserva los datos entre arranques.

## Ubicaciones

- `worklog/`: historial versionado del proyecto.
- `.env`: credenciales locales, nunca versionadas.
- `infra/`: infraestructura local.
- `suunto_ingest/`: extractor Python, instrucciones de prueba y sincronización. La base y los JSON reales viven en `data/`, fuera de Git.
- `suunto_ingest/pipeline.py`: ejecuta resúmenes, FIT y decodificación en orden; registra intentos en PostgreSQL. `infra/systemd/` contiene las unidades para programarlo localmente.
- `infra/backup_local.py`: copia privada de la base y originales, y prueba de restauración aislada. Las copias viven en `data/backups/`, fuera de Git; no moverlas al repositorio.
- `kore_api/`: API local de solo lectura y contrato OpenAPI para visualización y agentes. Incluye contexto y evidencia de dos ventanas completas para planificar la próxima sesión; no convierte estos datos en recomendaciones. `infra/systemd/kore-api.service` la mantiene disponible en localhost mientras haya sesión de usuario. No abrirla a la red sin autenticación.
- `plan_workbook.py`, `plan_daily_sync.py` y `PLANES.md`: contrato, persistencia atómica y actualización por etiqueta exacta y fecha local. Los libros privados y copias previas viven en `data/plans/`, fuera de Git.
- `plan_engine.py`, `plan_builder.py`, `planning_profiles.py`, `plan_app.py` y `PLAN_GENERATOR.md`: motor parametrizado y formulario local para generar borradores diarios sin agente. Activar un borrador es una acción explícita y archiva el activo anterior. Mantener la aplicación ligada a localhost y no versionar los libros.
- `resources/session_catalog.json`, `session_catalog.py` y `SESSION_CATALOG.md`: recursos versionados de sesiones, validación y reglas para ampliarlos. Las propuestas nuevas empiezan como borrador; no marcar un recurso como revisado sin referencias y revisión de sus supuestos.
- `training_level.py` y `TRAINING_LEVEL.md`: sugerencia conservadora de niveles iniciales a partir de exposición compatible en FIT. Mantener visibles cobertura, evidencia, límites y elección manual; no presentarla como estado de forma ni recuperación.
- `plan_feedback.py`, `plan_adaptation.py`, `plan_adjustment.py` y `PLAN_FEEDBACK.md`: registro subjetivo privado, propuesta semanal explicable y aplicación confirmada sobre fechas futuras. No modificar días pasados ni kilómetros mediante una adaptación, exigir vista previa y confirmación, y conservar como desconocidos los días sin datos Suunto.
- `README.md` y `ARCHITECTURE.md`: entrada operativa y mapa de componentes vigentes. Actualizarlos cuando cambien comandos, responsabilidades o datos privados.
