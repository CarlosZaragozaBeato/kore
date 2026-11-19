# Ciclo funcional de zapatillas

## Objetivo

Convertir Zapatillas en el segundo dominio funcional del archivo Kore, sin backend ni base de datos.

## Implementación

- Alta con marca/modelo, tipo, fecha, kilometraje, límite, precio y notas.
- Dos fases de uso parametrizadas por un umbral de kilómetros.
- Valoraciones de comodidad, respuesta, estabilidad, agarre y durabilidad.
- Edición de la ficha y del kilometraje manual.
- Vista de detalle con fase actual, progreso, valoración media y coste por 10 km.
- Retirada confirmada y conservación íntegra en el histórico.
- Mutaciones en `session-core`, persistencia atómica y preservación de campos desconocidos.
- Separación estricta entre demostración y archivo personal.

## Decisión temporal

Hasta que Kore importe actividades y permita asignarlas a zapatillas, `currentKm` se mantiene como ajuste manual explícito. Después será posible calcular el kilometraje verificable sin duplicar acumulados.

## Verificación

- Pruebas del núcleo para alta, edición, retirada y preservación de datos futuros.
- APK debug compilado correctamente.
- Android Lint sin errores.

## Siguiente paso

Probar el flujo en dispositivo. Después implementar la selección de actividades canónicas para alimentar Museo, marcas y kilometraje de zapatillas.
