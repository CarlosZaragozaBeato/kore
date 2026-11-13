# Exportar entrenamientos a Excel

Desde la raíz de Kore, con PostgreSQL local activo:

```bash
.venv/bin/python -m pip install -r requirements.txt
.venv/bin/python export_excel.py
```

Sin argumentos exporta la semana actual, de lunes a domingo, en la zona horaria `Europe/Madrid`. Para otra semana ISO o un rango inclusivo:

```bash
.venv/bin/python export_excel.py --week 2026-W46
.venv/bin/python export_excel.py --from 2026-11-01 --to 2026-11-13
```

El archivo se guarda con un nombre único en `data/exports/` y el comando muestra su ruta. También puedes elegir el destino con `--output /ruta/privada/entrenamientos.xlsx`; si ya existe, el comando se detiene para no sobrescribirlo. Dentro del repositorio solo se permite escribir en `data/`, que está excluido de Git. La carpeta de exportación tiene permisos `0700` y cada Excel, `0600`.

El libro contiene cuatro hojas:

- **Resumen:** fechas, número de entrenamientos, duración, distancia, TSS y cobertura de FIT.
- **Entrenamientos:** una fila por entrenamiento, con hora local, deporte y métricas disponibles.
- **Resumen diario:** todos los días del periodo, incluso los que no tienen entrenamientos.
- **Vueltas:** laps de los FIT vigentes decodificados.

La hoja de entrenamientos muestra también las etiquetas `userTags` y el desnivel acumulado recibidos de Suunto. Para crear planes por bloques y asociar entrenamientos, consulta [PLANES.md](PLANES.md).

Si falta un FIT o está pendiente de decodificar, el entrenamiento sigue visible y sus métricas derivadas quedan vacías. La exportación lee la base local; no llama a Suunto ni modifica datos. No incluye muestras individuales ni coordenadas. El Excel contiene información privada de entrenamiento y debe guardarse en un lugar privado.
