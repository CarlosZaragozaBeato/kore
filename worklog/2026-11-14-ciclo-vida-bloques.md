# Ciclo de vida de bloques

## Estado y objetivo

Implementado un sistema recuperable para sacar un Excel del estado activo cuando termina o cuando el usuario decide retirarlo.

## Comportamiento

- La primera sincronización posterior a la fecha final marca el libro como finalizado, añade fecha y motivo de cierre y lo mueve a `data/plans/completed/`.
- La interfaz `/plans` permite finalizar anticipadamente o retirar el activo.
- Ambas acciones manuales requieren escribir la etiqueta exacta del bloque.
- Retirar mueve atómicamente el archivo intacto a `data/plans/discarded/`; no existe borrado físico.
- La pantalla lista nombres de libros finalizados y retirados. Tras mover el activo puede activarse un nuevo borrador.

## Verificación

Las pruebas sintéticas comprueban cierre, marca de estado y motivo, negativa al cierre automático antes de tiempo, movimiento binariamente idéntico al retirar y cierre automático encadenado desde la actualización diaria. Las 53 pruebas del repositorio pasan después de integrar la interfaz y la documentación.

## Límite y siguiente paso

Todavía no hay una acción de restauración desde `discarded/` o `completed/`. Los archivos son recuperables manualmente, pero una restauración desde la interfaz deberá validar que no haya otro activo y exigir confirmación explícita.
