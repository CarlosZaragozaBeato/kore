# Sugerencia de nivel desde Suunto

`training_level.py` analiza una ventana móvil de 84 días de carreras con FIT vigente y propone niveles iniciales editables para las progresiones de velocidad y ritmo sostenido. La aplicación local muestra la evidencia, rellena los campos y conserva en el Excel la sugerencia, la elección final, la fecha y la ventana utilizadas.

## Qué observa

- kilómetros semanales de los últimos 28 días, expresados como una media de cuatro semanas;
- tirada más larga de los últimos 28 días y de toda la ventana;
- número y tamaño de las sesiones decodificadas;
- patrones repetidos de vuelta de trabajo más rápida seguida por recuperación más lenta;
- cobertura de sesiones que pueden alojar el calentamiento, dosis y vuelta a la calma del catálogo.

Las vueltas uniformes de un rodaje con autolap no cuentan como intervalos. Para reconocer exposición estructurada se exigen varias parejas de trabajo y recuperación, diferencia de ritmo y una forma compatible con el escalón. Una única sesión detectada provoca una regresión de un nivel. Dos o más permiten proponer el nivel observado, limitado por el tamaño reciente de las sesiones. La confianza solo puede ser `low` o `medium` porque el FIT no incluye el propósito, RPE ni la respuesta posterior.

Los umbrales de 6/8/10/12 km para velocidad y 35/45/60/70 minutos para sostenido son heurísticas transparentes de espacio disponible dentro de la sesión. No son umbrales fisiológicos ni evidencia de que la dosis sea apropiada. El nivel 3 sostenido tampoco se infiere automáticamente: una carrera continua no demuestra que se realizase con el propósito o intensidad del recurso.

## Límites y decisión final

La sugerencia describe exposición compatible. El usuario puede cambiarla antes de crear el borrador, y el Excel guarda ambos valores. Kore no deduce la limitación principal entre velocidad y sostenido a partir de estas detecciones. Esa decisión necesita una prueba válida, objetivo y sensaciones.

La elección conservadora sigue el principio de combinar carga externa con estado subjetivo. Un estudio en corredores recreativos ajustó la carga usando recuperación percibida, HRV y relación entre frecuencia cardíaca y velocidad, y observó respuestas individuales distintas; no usó únicamente volumen o vueltas del reloj. Otro estudio encontró que combinar duración con RPE daba una estimación más individualizada de los cambios de carga. Estas referencias justifican mantener la sugerencia editable y con confianza limitada, no las dosis concretas del catálogo: [entrenamiento individualizado y recuperación](https://pubmed.ncbi.nlm.nih.gov/35975912/), [duración y RPE para estimar carga](https://pubmed.ncbi.nlm.nih.gov/33064812/).

## Acceso local

La interfaz muestra la evaluación en `http://127.0.0.1:8092/`. El endpoint `GET /api/assessment` ofrece la misma estructura para una futura interfaz. Consulta PostgreSQL en modo de solo lectura y no escribe datos personales en Git. Si la base no está disponible, el formulario sigue funcionando con introducción manual.
