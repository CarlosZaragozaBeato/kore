# 2026-11-13 — Plantillas, análisis histórico y enlace con Suunto

**Estado:** implementación comprobada localmente.

## Decisiones

- El número de semanas y las cargas planificadas quedan para el agente de planificación. Las filas precreadas de las plantillas son capacidad editable.
- Los objetivos semanales declarados se guardan en una configuración local ignorada por Git. El código no los presenta como umbrales óptimos.
- El enlace con Suunto admite dos modos: por fecha y por etiqueta exacta de plan. La fecha asigna cada sesión a una semana y bloque.

## Cambios

- Se añadieron cuatro plantillas de Excel, un actualizador que conserva el libro original y un análisis histórico con semanas, carreras, vueltas, desnivel y cobertura de datos.
- El contexto del agente se genera localmente junto al análisis e incluye el flujo para rellenar planes y la información que falta antes de cerrarlos.
- Se exponen las etiquetas `userTags` de los resúmenes Suunto en el exportador. El formato de etiqueta propuesto es `kore_<modalidad>_<inicio AAAAMMDD>`.

## Verificación

- Los resúmenes locales contienen `userTags` como lista de textos en algunas actividades; no se registraron sus valores en el repositorio.
- Se generaron y abrieron las cuatro plantillas, un plan actualizado por fecha y otro por etiqueta, el análisis histórico y el contexto privado.
- El modo por fecha añadió sesiones reales a la semana correspondiente. El modo por etiqueta dejó el plan sin sesiones cuando no había coincidencias, como se esperaba.
- Una semana sin coincidencias en modo por etiqueta queda en blanco y marcada como «Sin actividades etiquetadas», para no confundirla con una semana de cero entrenamiento.
- La suite completa de 20 pruebas pasa, incluidas las reglas de enlace por etiqueta, semanas incompletas y texto de etiquetas en Excel.

## Límites

- La propagación de una etiqueta recién editada en la app Suunto a través de la API no se ha verificado todavía.
- Las plantillas son estructura técnica. El agente debe decidir duración, bloques y sesiones con el usuario.
