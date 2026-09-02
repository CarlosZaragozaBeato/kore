# Paleta de colores — Kore

App de entrenamiento y control de vida para atletas de running, triatlón e ironman.

## Filosofía

Cada disciplina del triatlón tiene un color propio que funciona como código visual en toda la app (una sesión de natación se pinta de turquesa, una de ciclismo de azul, etc.). El azul noche estructura la interfaz y transmite rendimiento y seriedad, ideal para pantallas densas de métricas (sueño, HRV, carga, nutrición). El naranja es el acento de energía y acción, y actúa como color de "meta / latido / hoy" que unifica la marca.

## Colores principales

| Rol | Nombre | HEX | RGB | Uso |
|-----|--------|-----|-----|-----|
| Estructura | Núcleo (azul noche) | `#0E1B2A` | 14, 27, 42 | Fondos, paneles, barra de navegación |
| Disciplina · Natación | Agua (turquesa) | `#17C3B2` | 23, 195, 178 | Sesiones de natación, anillo exterior |
| Disciplina · Ciclismo | Ciclismo (azul) | `#3A86FF` | 58, 134, 255 | Sesiones de bici, anillo medio |
| Disciplina · Carrera | Carrera (naranja) | `#FF7A1A` | 255, 122, 26 | Sesiones de running, acento, CTA |
| Neutro claro | Hueso | `#EAF2F8` | 234, 242, 248 | Texto sobre oscuro, superficies claras |

## Tonos de apoyo

| Rol | Nombre | HEX | Uso |
|-----|--------|-----|-----|
| Estructura + | Núcleo elevado | `#132537` | Cards y pistas de anillos sobre el fondo |
| Texto secundario | Gris azulado | `#7C93A8` | Etiquetas, subtítulos, texto atenuado |
| Borde claro | Gris hueso | `#D3DEE8` | Bordes y separadores en modo claro |

## Colores de estado (sugeridos)

| Estado | HEX | Uso |
|--------|-----|-----|
| Éxito / recuperado | `#2FBF71` | Buen descanso, objetivo cumplido |
| Aviso / fatiga | `#F5B300` | Carga alta, HRV bajo |
| Alerta / riesgo | `#E5484D` | Sobreentrenamiento, sesión perdida |

## Roles de interfaz

- **Color primario de acción (CTA, botones, "hoy"):** naranja `#FF7A1A`
- **Fondo de app y paneles de datos:** azul noche `#0E1B2A`
- **Superficie de tarjetas:** núcleo elevado `#132537` (modo oscuro) / `#EAF2F8` (modo claro)
- **Texto principal:** hueso `#EAF2F8` sobre oscuro / núcleo `#0E1B2A` sobre claro
- **Texto secundario:** gris azulado `#7C93A8`

## Código de disciplinas (referencia rápida)

```
Natación  →  #17C3B2  (turquesa)
Ciclismo  →  #3A86FF  (azul)
Carrera   →  #FF7A1A  (naranja)
```

## Tokens CSS

```css
:root {
  --color-core:        #0E1B2A;
  --color-core-raised: #132537;
  --color-swim:        #17C3B2;
  --color-bike:        #3A86FF;
  --color-run:         #FF7A1A;
  --color-bone:        #EAF2F8;
  --color-text-muted:  #7C93A8;
  --color-border:      #D3DEE8;

  --color-success:     #2FBF71;
  --color-warning:     #F5B300;
  --color-danger:      #E5484D;

  --color-action:      var(--color-run);
  --color-bg:          var(--color-core);
}
```
