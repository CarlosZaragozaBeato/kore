# Generador configurable de planes

El motor `plan_engine.py` produce un borrador determinista a partir de parámetros explícitos. `plan_app.py` ofrece un formulario local para cambiar esos parámetros y revisar el plan antes de activarlo. El borrador descargado usa el formato diario V2, pero carece de etiqueta activa; Suunto solo se enlaza cuando el usuario pulsa **Activar este bloque**. La activación archiva el libro anterior en `data/plans/archive/` y rechaza sustituirlo si contiene resultados reales.

## Ejecutar la aplicación

```bash
.venv/bin/uvicorn plan_app:app --host 127.0.0.1 --port 8092
```

Abrir `http://127.0.0.1:8092/`. La aplicación solo debe exponerse en localhost: permite activar un libro local y contiene información personal de entrenamiento. Los borradores en vista previa viven en memoria durante la sesión del servidor; el botón **Descargar Excel borrador** exporta el libro sin modificar el activo. El botón **Activar este bloque** es la confirmación explícita del usuario. Después se puede ejecutar `plan_excel.py update` para incorporar sesiones etiquetadas desde Suunto.

Al abrir el formulario, [la evaluación de nivel](TRAINING_LEVEL.md) usa el histórico local para rellenar volumen, tirada larga y niveles iniciales de velocidad y sostenido. Muestra la evidencia y confianza; todos los valores siguen siendo editables. El Excel conserva la sugerencia y la elección realizada.

La ruta `/feedback` registra RPE, disponibilidad, recuperación y molestias en el libro privado activo. La ruta `/adaptation` cruza esas observaciones con los resultados Suunto de la semana y propone mantener, reducir o permitir únicamente la progresión ya prevista. Consulta [PLAN_FEEDBACK.md](PLAN_FEEDBACK.md) para las reglas y límites. La evaluación no modifica el plan; reducir o mantener exige generar una comparación y confirmarla expresamente. La adaptación conserva los kilómetros y los días anteriores, archiva el libro previo y deja auditoría en el Excel.

## Parámetros y decisiones

| Selección | Efecto |
|---|---|
| Distancia del bloque y objetivo final | El bloque puede ser de 10 km aunque el objetivo final sea maratón; el motor conserva una tirada larga y registra la relación. |
| Desarrollo o competición | Una fecha de carrera calcula automáticamente el número de semanas hasta la semana del evento. Sin carrera se indica la duración del bloque. |
| Necesidad principal | Prioriza velocidad, ritmo sostenido o alterna ambos. |
| Volumen actual, objetivo e incremento máximo elegido | Define los km semanales del borrador. Las semanas reducidas y la afinación disminuyen el volumen. |
| Tirada larga reciente y aumento máximo elegido | Limita su evolución dentro del volumen semanal. |
| Días disponibles y día de tirada larga | Determina las filas con entrenamiento y descanso y la ubicación de sesiones estructuradas. |
| Número de sesiones estructuradas | Permite elegir cero, una o dos además de la tirada; nunca se fija a tres estímulos. |
| Nivel inicial de series y sostenido | Elige la entrada en un catálogo de cuatro escalones; no obliga a empezar por repeticiones de un minuto. |
| Fatiga actual | La opción elevada reduce la primera semana y omite su calidad estructurada. |

El [catálogo de sesiones](SESSION_CATALOG.md) contiene dosis mínima, prevista y máxima. Calentamiento, recuperación y vuelta a la calma figuran en la descripción y los km totales del día incluyen el trabajo. La aplicación muestra las dos primeras semanas y todos los volúmenes semanales; el Excel contiene el detalle del bloque, los parámetros usados y una hoja `Decisiones de sesión` con el recurso y motivo de cada selección. El motor no usa VO₂max para inventar ritmos: quedan por calibrar con una observación válida. El seguimiento Suunto sigue cruzando etiqueta exacta con fecha local en `Europe/Madrid`.

## Límites de esta primera versión

- El catálogo y la progresión son una estructura editable, no una prescripción clínica ni un cálculo de carga óptima. Kore sugiere el nivel desde exposición compatible en FIT, pero no interpreta esa sugerencia como preparación o tolerancia actual.
- La progresión dentro de un borrador se calcula antes de observar su ejecución. La revisión semanal puede reducir la dosis de una sesión futura o mantener el nivel anterior tras mostrar el cambio y recibir confirmación; todavía no recalcula volumen ni reestructura el resto del bloque.
- Las sesiones estructuradas se colocan según los días disponibles y separación de la tirada larga. El motor no estima aún una capacidad individual de tolerar esas sesiones; si hay menos de cuatro días no programa calidad estructurada.
- Una semana de descarga puede quedar por debajo del objetivo semanal. El aumento posterior recupera parte del volumen previo; el límite de incremento se aplica a las semanas de desarrollo, no a la comparación directa con la descarga.
- La fecha de competición debe estar dentro de las 40 semanas siguientes y en un día seleccionado para entrenar. Los días fuera de esa selección quedan como descanso.

La prioridad de desarrollo siguiente es ampliar el historial de decisiones y usarlo al crear el siguiente bloque, antes de plantear reglas separadas y revisables para el volumen.
