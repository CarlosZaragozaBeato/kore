# Generador configurable de planes

El motor `plan_engine.py` produce un borrador determinista a partir de parámetros explícitos. `plan_app.py` ofrece un formulario local para cambiar esos parámetros y revisar el plan antes de activarlo. El borrador descargado usa el formato diario V2, pero carece de etiqueta activa; Suunto solo se enlaza cuando el usuario pulsa **Activar este bloque**. La activación archiva el libro anterior en `data/plans/archive/` y rechaza sustituirlo si contiene resultados reales.

## Ejecutar la aplicación

```bash
.venv/bin/uvicorn plan_app:app --host 127.0.0.1 --port 8092
```

Abrir `http://127.0.0.1:8092/`. La aplicación solo debe exponerse en localhost: permite activar un libro local y contiene información personal de entrenamiento. Los borradores en vista previa viven en memoria durante la sesión del servidor; el botón **Descargar Excel borrador** exporta el libro sin modificar el activo. El botón **Activar este bloque** es la confirmación explícita del usuario. Después se puede ejecutar `plan_excel.py update` para incorporar sesiones etiquetadas desde Suunto.

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

- El catálogo y la progresión son una estructura editable, no una prescripción clínica ni un cálculo de carga óptima. El nivel inicial lo introduce el usuario; Kore no lo estima todavía del FIT.
- La progresión dentro de un borrador se calcula antes de observar su ejecución. Para adaptar semanas futuras a resultados y sensaciones, hay que generar otro borrador y revisar los cambios antes de sustituir el activo.
- Las sesiones estructuradas se colocan según los días disponibles y separación de la tirada larga. El motor no estima aún una capacidad individual de tolerar esas sesiones; si hay menos de cuatro días no programa calidad estructurada.
- Una semana de descarga puede quedar por debajo del objetivo semanal. El aumento posterior recupera parte del volumen previo; el límite de incremento se aplica a las semanas de desarrollo, no a la comparación directa con la descarga.
- La fecha de competición debe estar dentro de las 40 semanas siguientes y en un día seleccionado para entrenar. Los días fuera de esa selección quedan como descanso.

La prioridad de desarrollo siguiente es comparar el borrador con el histórico real y mostrar por qué se eligió cada nivel de sesión, cuánto difiere del trabajo reciente y qué condiciones harían mantener o cambiar la progresión. Después puede automatizarse la revisión de semanas futuras conservando las anteriores.
