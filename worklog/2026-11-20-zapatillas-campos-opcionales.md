# Campos opcionales de zapatillas

## Cambio

- `usagePhases` puede ser una lista vacía.
- El formulario admite ninguna fase, una fase sin límite o dos fases separadas por kilometraje.
- `notes` continúa siendo nullable y ahora se identifica claramente como opcional.
- El detalle muestra un estado explícito cuando no existen fases.
- El botón de guardado ya no exige completar fases ni notas.

## Verificación

- Once pruebas del contrato superadas, incluida una zapatilla sin fases ni notas.
- APK debug compilado correctamente.
- Android Lint: 0 errores; permanecen seis avisos de versiones y cinco sugerencias Compose ya conocidas.
