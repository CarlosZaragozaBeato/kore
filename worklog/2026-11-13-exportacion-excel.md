# 2026-11-13 — Exportación sencilla a Excel

**Estado:** implementado y comprobado localmente.

## Objetivo

Generar un `.xlsx` con los entrenamientos de la semana o de un rango de fechas indicado, usando la base local.

## Cambios

- Se añadió `export_excel.py`, con semana actual por defecto, `--week` para una semana ISO y `--from`/`--to` para un rango inclusivo.
- El libro incluye resumen, entrenamientos, días y vueltas. Conserva entrenamientos con FIT pendiente y deja vacías sus métricas derivadas.
- La salida por defecto se guarda de forma privada en `data/exports/`, fuera de Git. No se exportan muestras ni coordenadas.
- Se añadió `openpyxl` a las dependencias y una guía de uso en `EXPORT.md`.

## Verificación

- Se generó y abrió un libro para una semana y otro para un rango. Las hojas y los recuentos coincidieron con la consulta local.
- La carpeta y los libros tienen permisos privados; Git ignora `data/exports/`.
- Cuatro pruebas nuevas cubren semanas ISO, rangos, cambio horario, FIT ausente y salida fuera del repositorio versionado.

## Límite

- Este exportador muestra sesiones y vueltas; los registros por segundo no se incluyen para mantener el Excel manejable.
