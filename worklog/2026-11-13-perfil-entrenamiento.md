# 2026-11-13 — Perfil privado para planificación

**Estado:** implementado y verificado localmente.

## Cambio

Se añadió un generador de perfil de entrenamiento observado en Markdown y JSON. Agrupa ventanas recientes, semanas completas, frecuencia, tiradas largas, cobertura de FIT/TSS y objetivos declarados. Acepta fatiga y limitaciones comunicadas por el usuario como datos fechados, separados de los registros de Suunto. Los resultados se escriben solo en `data/analysis/`, excluido de Git.

## Verificación

Las pruebas cubren la exclusión de la semana parcial, la cobertura de datos y la conservación del estado subjetivo. Se generó el informe con la base local y se comprobó que los archivos permanecen privados.

## Límite

El perfil describe actividad registrada; no permite concluir estado médico, recuperación ni carga óptima. El estado subjetivo debe actualizarse antes de planificar.
