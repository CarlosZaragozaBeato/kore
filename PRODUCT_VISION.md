# Visión de producto de Kore

## Propósito

Kore es el archivo personal de la vida de un runner: una aplicación Android local-first donde conservar zapatillas, entrenamientos memorables y la evolución de todas las marcas personales. No planifica bloques ni prescribe entrenamiento.

La experiencia debe sentirse como un museo personal, no como una tabla de actividad. Cada elemento importante puede tener contexto, relato, relaciones y una presentación cuidada. La sesión portátil del usuario es la fuente de verdad; no existe backend ni base de datos.

## Áreas de la aplicación

- **Inicio:** resumen vital del corredor, últimas incorporaciones y recuerdos destacados.
- **Museo:** sesiones elegidas explícitamente por el usuario, con historia, métricas, etiquetas, zapatillas y marcas vinculadas.
- **Marcas:** PB actuales e histórico completo por distancia, incluyendo cuándo y en qué actividad se lograron.
- **Zapatillas:** rotación activa, kilometraje, coste, valoraciones, usos por etapas e histórico retirado.
- **Ajustes:** sesión portable, fuentes externas, importación/exportación y privacidad.

## Idioma propio e independencia de proveedor

Kore nunca usa entidades Suunto como modelo interno. Define un vocabulario canónico:

- `activity`: actividad deportiva normalizada.
- `sourceRef`: referencia al proveedor y al identificador externo.
- `museumEntry`: pieza seleccionada para el museo, con snapshot y relato personal.
- `personalBest`: marca lograda en una distancia y su evidencia.
- `shoe`: zapatilla y su ciclo de vida.

Suunto es el primer adaptador. Garmin, COROS, Polar, Strava, un FIT o una entrada manual deben poder producir las mismas entidades sin cambiar el archivo de sesión ni la interfaz.

## Zapatillas

Cada zapatilla conserva:

- Marca, modelo, variante y tipo.
- Fecha de alta, precio y moneda.
- Kilómetros iniciales, asignados y corregidos manualmente.
- Límite orientativo y estado: activa, en observación o retirada.
- Valoraciones por comodidad, respuesta, estabilidad, agarre y durabilidad.
- Notas generales y notas históricas fechadas.
- Fases de uso, por ejemplo: `0–200 km: series` y `200–700 km: rodajes`.
- Actividades vinculadas para reconstruir su kilometraje sin duplicar el entrenamiento.

El límite es una ayuda personal, no una afirmación médica o del fabricante.

## Museo de sesiones

Importar actividades no las convierte automáticamente en piezas del museo. El usuario elige un día o actividad y crea una pieza con título, motivo, relato, etiquetas y vínculos a zapatillas/PB.

La pieza guarda un snapshot de las métricas relevantes para que el recuerdo siga siendo visible aunque el proveedor deje de estar conectado. FIT, GPS y muestras masivas pueden mantenerse como adjuntos privados opcionales.

## Marcas personales

Kore conserva todas las marcas, no solo la mejor actual. Cada registro incluye distancia, tiempo, ritmo derivado, fecha, tipo de evidencia, actividad y pieza de museo relacionada. La marca actual se deriva de la mejor válida; las anteriores forman una línea temporal.

Las marcas pueden proceder de carrera oficial, entrenamiento, estimación o entrada manual, y deben mostrar ese origen sin mezclarlas silenciosamente.

## Privacidad y portabilidad

La aplicación funciona sin cuenta Kore, backend o base de datos. La sesión `*.kore.json` contiene el archivo semántico completo. Credenciales y tokens viven separados en almacenamiento seguro Android y nunca se exportan. Los binarios grandes pueden añadirse en un paquete de exportación opcional.

## Secuencia de implementación

1. Contrato de sesión v2 y migración preservadora desde v1.
2. Mockup nuevo: Inicio, Museo, Marcas, Zapatillas y Ajustes.
3. Mutaciones Kotlin para zapatillas, museo y PB con pruebas.
4. Adaptador canónico y selección de actividades Suunto.
5. Asignación de zapatillas y cálculo verificable de kilometraje.
6. Importación/exportación completa y adaptadores adicionales.
