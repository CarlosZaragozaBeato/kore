package com.kore.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProgressScreen(padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoBadge()
                Text("Estado para 10K sub 38", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Tu nivel siempre se calcula respecto al objetivo activo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.inverseSurface) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("MEDIA ACTUAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = .65f))
                        Text("78", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.inverseOnSurface, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null, tint = Color(0xFF68D391))
                            Text(" +3", color = Color(0xFF68D391), fontWeight = FontWeight.Bold)
                        }
                        Text("Peak 81", color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = .7f))
                    }
                }
                Text("Buena tendencia", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Estás más cerca del objetivo, pero falta consolidar velocidad específica.",
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = .75f),
                )
            }
        }
        item { SectionTitle("Evolución", "Últimas 8 semanas") }
        item {
            KoreCard {
                ProgressChart()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("S1", style = MaterialTheme.typography.labelSmall)
                    Text("S4", style = MaterialTheme.typography.labelSmall)
                    Text("HOY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { SectionTitle("Capacidades", "Ver evidencia") }
        items(MockData.fitness.size) { index ->
            val (label, score) = MockData.fitness[index]
            KoreCard(containerColor = MaterialTheme.colorScheme.surface) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text(score.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { score / 100f },
                    modifier = Modifier.fillMaxWidth().height(7.dp),
                    color = when {
                        score >= 85 -> Color(0xFF2E9D63)
                        score >= 75 -> MaterialTheme.colorScheme.primary
                        else -> Color(0xFFE36B5D)
                    },
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
        item { SectionTitle("Mejores marcas") }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                PersonalBestRow("5 km", "18:21", "3:40/km", "+12 s del objetivo")
                PersonalBestRow("10 km", "39:08", "3:55/km", "Objetivo 38:00")
                PersonalBestRow("21,1 km", "1:28:42", "4:12/km", "Sin objetivo activo")
            }
        }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .13f)) {
                Label("Siguiente evaluación")
                Text("Test de umbral · 30 min", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Programado para la semana 4. Actualizará umbral, resistencia específica y ritmos orientativos.")
            }
        }
    }
}

@Composable
private fun ProgressChart() {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        repeat(4) { index ->
            val y = size.height * index / 3f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        val values = listOf(.28f, .34f, .31f, .46f, .52f, .58f, .66f, .74f)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = size.width * index / (values.size - 1)
            val y = size.height * (1f - value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 7f, cap = StrokeCap.Round))
        values.forEachIndexed { index, value ->
            drawCircle(
                color = lineColor,
                radius = if (index == values.lastIndex) 9f else 5f,
                center = Offset(size.width * index / (values.size - 1), size.height * (1f - value)),
            )
        }
    }
}

@Composable
private fun PersonalBestRow(distance: String, time: String, pace: String, note: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(distance, modifier = Modifier.weight(.7f), fontWeight = FontWeight.Bold)
        Column(Modifier.weight(1f)) {
            Text(time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(pace, style = MaterialTheme.typography.bodySmall)
        }
        Text(note, modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
