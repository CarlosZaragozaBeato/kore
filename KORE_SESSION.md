# Sesión personal de Kore v2

La sesión Kore es el archivo canónico y portátil del corredor. La versión actual se identifica con `"koreSession": 2` y funciona sin backend ni base de datos.

## Secciones

| Sección | Responsabilidad |
|---|---|
| `profile` | Identidad local y fechas del archivo. |
| `activities` | Actividades normalizadas necesarias para histórico, relaciones y consulta offline. |
| `museum` | Sesiones especiales elegidas por el usuario y su relato personal. |
| `personalBests` | Todas las marcas actuales e históricas con evidencia. |
| `shoes` | Fichas, valoraciones, uso por fases y ciclo de vida del calzado. |
| `routes` | Recorridos guardados manualmente o importados desde una fuente externa. |
| `sources` | Conexiones y cursores por proveedor; nunca secretos. |
| `settings` | Preferencias portátiles. |

## Actividad canónica

Una `activity` utiliza campos Kore estables: `id`, `sport`, `startedAt`, `durationSeconds`, `distanceMeters`, métricas normalizadas, laps relevantes y relaciones. Su procedencia vive exclusivamente en:

```json
{
  "sourceRef": {
    "provider": "suunto",
    "externalId": "...",
    "importedAt": "..."
  }
}
```

El adaptador convierte cada proveedor a este idioma. Ninguna pantalla depende de nombres de campos Suunto.

## Pieza de museo

Una `museumEntry` contiene:

- Identificador propio y `activityId` cuando existe actividad importada.
- Snapshot de fecha, distancia, duración, ritmo y métricas esenciales.
- Título, relato, motivo, etiquetas y fecha de incorporación.
- `shoeIds` y `personalBestIds` relacionados.
- Referencias opcionales a adjuntos privados.

El snapshot evita que el recuerdo desaparezca si se desconecta o elimina la fuente externa.

## Marca personal

Cada `personalBest` conserva `distanceMeters`, `durationMillis`, `achievedAt`, `evidenceType`, `activityId`, `museumEntryId` y notas. No se sobrescribe una marca anterior: la actual se calcula como el mejor registro válido de la distancia y el resto permanece en la cronología.

## Zapatilla

Cada `shoe` conserva identidad, `acquiredAt`, precio/moneda, tipo, límite, estado, notas y valoraciones. El kilometraje se obtiene de actividades asignadas más un ajuste explícito; no se mantiene un segundo acumulado silencioso.

`usagePhases` describe la intención por intervalo de kilómetros:

```json
[
  {"fromKm": 0, "toKm": 200, "uses": ["intervals", "race"]},
  {"fromKm": 200, "toKm": 700, "uses": ["easy", "long-run"]}
]
```

## Ruta

Cada `route` conserva identidad, nombre, distancia, ubicación, superficie, desnivel, notas, fecha de creación y `sourceRef`. La primera implementación admite creación manual; GPX y proveedores se normalizarán al mismo modelo.

La geometría completa no se incrusta obligatoriamente en el JSON principal. Podrá vivir como adjunto privado, mientras la sesión mantiene resumen y referencia estable.

## Límites y seguridad

La sesión no incluye credenciales, tokens, respuestas originales, GPS punto a punto ni muestras de alta frecuencia. Estos elementos viven en almacenamiento seguro o caché/adjuntos privados. Importar valida antes de sustituir la sesión; los campos desconocidos se conservan y las versiones futuras se rechazan.

El núcleo Kotlin migra v1 a v2 añadiendo el nuevo archivo runner y preservando íntegramente los campos anteriores como datos desconocidos recuperables.
