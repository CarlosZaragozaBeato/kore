# Kore: instrucciones para agentes

## Objetivo

Kore debe extraer la información de entrenamiento de Suunto, conservarla en una base de datos local y prepararla para consulta, visualización y análisis por un agente que ayude a tomar decisiones sobre entrenamientos.

Prioridades actuales, en este orden:
1. Extracción fiable y recuperable de los datos de Suunto.
2. Conservación local de los datos originales y de los datos estructurados.
3. Procesos de tratamiento y consultas útiles para visualización y agentes.

El backend futuro no debe implementarse en Java. El servicio Quarkus actual es código heredado que sirve como referencia para la migración; no ampliar su arquitectura sin una decisión explícita. La propuesta técnica inicial es Python con PostgreSQL local y FastAPI para las consultas. Registrar en `worklog/` las decisiones y cualquier cambio de dirección.

## Antes y después de trabajar

- Leer el `AGENTS.md` de la carpeta superior y comprobar `git status` antes de modificar el repositorio. Respetar los cambios existentes.
- Registrar cada implementación, avance, problema y decisión relevante en `worklog/`, con fecha, evidencia, estado y siguiente paso. Actualizar una entrada cuando se resuelva un problema.
- Mantener las entradas útiles para continuar el trabajo: qué se cambió, por qué, cómo se verificó y qué queda pendiente. No registrar suposiciones como hechos comprobados.
- No incluir credenciales, tokens, datos personales, respuestas completas de Suunto ni archivos FIT en el repositorio o en el worklog. El `.env` raíz es local y está excluido de Git.
- Antes de considerar fiable la extracción, verificar el acceso real a Suunto, la persistencia de la rotación del refresh token, la reanudación tras fallos y que la base local conserva los datos entre arranques.

## Ubicaciones

- `worklog/`: historial versionado del proyecto.
- `.env`: credenciales locales, nunca versionadas. `services/suunto-service/.env` es un enlace local al mismo archivo para el servicio heredado.
- `services/suunto-service/`: implementación Java existente, solo como referencia hasta su sustitución.
- `infra/`: infraestructura local.
- `suunto_ingest/`: extractor Python, instrucciones de prueba y sincronización. La base y los JSON reales viven en `data/`, fuera de Git.
- `suunto_ingest/pipeline.py`: ejecuta resúmenes, FIT y decodificación en orden; registra intentos en PostgreSQL. `infra/systemd/` contiene las unidades para programarlo localmente.
- `infra/backup_local.py`: copia privada de la base y originales, y prueba de restauración aislada. Las copias viven en `data/backups/`, fuera de Git; no moverlas al repositorio.
- `kore_api/`: API local de solo lectura y contrato OpenAPI para visualización y agentes. El contexto del agente incluye vigencia, cobertura, periodos parciales y desglose por deporte; no convierte estos datos en recomendaciones. Mantenerla ligada a localhost mientras no tenga autenticación.
- `export_excel.py` y `EXPORT.md`: exportación local de semanas o rangos a `.xlsx`. Los libros contienen datos personales y van a `data/exports/`, fuera de Git.
- `plan_excel.py`, `analysis_training.py` y `PLANES.md`: plantillas de planes, enlace de entrenamientos y análisis histórico. Los objetivos concretos, libros y contexto del agente viven bajo `data/`, fuera de Git. No versionar estos datos.
- `training_profile.py`: genera en `data/analysis/` un perfil privado de entrenamiento observado para el agente de planificación. Incluir fecha y origen de la fatiga o limitaciones declaradas; no convertir métricas de Suunto en diagnósticos ni versionar el perfil.
