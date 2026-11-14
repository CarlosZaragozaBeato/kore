# Catálogo de sesiones de Kore

`resources/session_catalog.json` contiene las piezas que el motor puede seleccionar. Es un recurso versionado y no contiene datos personales. Se valida con:

```bash
.venv/bin/python session_catalog.py
```

La primera versión tiene 13 recursos: descanso, recuperación, rodaje fácil, tirada aeróbica, evaluación y cuatro niveles de velocidad y de trabajo sostenido. Todos empiezan con `review_status=draft` por defecto. Esto indica que las dosis proceden del prototipo y todavía deben revisarse antes de tratarlas como biblioteca deportiva estable.

## Contrato de una sesión

Cada entrada declara un identificador estable, función, distancias compatibles, objetivos, fases, intensidad, colocación, calentamiento, parte principal, recuperación y vuelta a la calma. Las sesiones estructuradas añaden grupo de progresión, nivel de entrada de 0 a 3, dosis mínima/prevista/máxima y enlaces a su progresión y regresión inmediatas.

El validador rechaza identificadores duplicados, enlaces desconocidos, saltos en los niveles y recursos marcados como revisados sin referencias. Un recurso nuevo debe permanecer como `draft` hasta revisar sus dosis y sus condiciones de uso. Para cambiarlo a `reviewed`, debe incluir al menos una referencia en `source_refs`; la referencia respalda el alcance que se le atribuye, no convierte automáticamente una dosis concreta en adecuada para cualquier corredor.

## Cómo ampliar el catálogo con un agente

El agente puede proponer recursos, pero debe devolver objetos que cumplan este contrato. Por cada propuesta debe explicar:

- qué capacidad pretende desarrollar;
- para qué distancias, fases y niveles resulta elegible;
- dosis mínima, prevista y máxima;
- progresión y regresión;
- colocación respecto a otras cargas;
- fuente y qué afirmación concreta sustenta;
- supuestos que todavía requieren revisión.

Las propuestas se incorporan primero como `draft`. Después deben pasar el validador y pruebas de selección, comprobar que caben dentro de los kilómetros diarios y revisar que no producen combinaciones contradictorias. El motor decide qué recurso usar y guarda esa decisión en la hoja `Decisiones de sesión`; el agente no escribe directamente el plan activo.

## Selección actual

El motor recibe el nivel de entrada para velocidad y sostenido. La cadencia elegida en el formulario determina cada cuántas semanas de desarrollo intenta avanzar un nivel. Si el nivel solicitado no es compatible con la distancia y fase, elige el nivel elegible más alto que no lo supere y registra la diferencia. La mezcla puede combinar velocidad y sostenido o repetir la necesidad principal. Las descargas, la afinación, la competición y la primera semana con fatiga elevada no incorporan calidad estructurada. Los rodajes y la tirada se eligen también desde el catálogo.

Esta lógica es deliberadamente explícita. `training_level.py` propone un nivel candidato desde el histórico de Suunto, muestra la evidencia y mantiene la elección final editable. La detección describe exposición compatible; no cambia el estado `draft` de los recursos ni valida sus dosis.
