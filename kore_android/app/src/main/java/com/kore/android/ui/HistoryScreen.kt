package com.kore.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class HistoryFilter(val label: String) {
    Blocks("Bloques"), Workouts("Entrenos"), Shoes("Zapatillas"),
}

@Composable
fun HistoryScreen(padding: PaddingValues) {
    var filter by remember { mutableStateOf(HistoryFilter.Blocks) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoBadge()
                Text("Histórico", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Recupera lo importante sin cargar todo a la vez.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.label) },
                    )
                }
            }
        }
        when (filter) {
            HistoryFilter.Blocks -> blocks()
            HistoryFilter.Workouts -> workouts()
            HistoryFilter.Shoes -> shoes()
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.blocks() {
    item { SectionTitle("Bloques anteriores", "3 guardados") }
    item {
        HistoryBlock("Base aeróbica", "8 semanas", "632 km", "Completado · Sep–Oct 2026", 92)
    }
    item {
        HistoryBlock("Velocidad 5K", "6 semanas", "448 km", "Completado · Jul–Ago 2026", 87)
    }
    item {
        HistoryBlock("Media maratón", "14 semanas", "1.184 km", "Completado · Mar–Jun 2026", 94)
    }
    item {
        KoreCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
            Label("Análisis externo")
            Text("Analizar todos los bloques", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Exporta la sesión completa para comparar ciclos, progresión y respuesta a distintas sesiones.")
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.workouts() {
    item { SectionTitle("Entrenamientos destacados", "12 guardados") }
    item { SavedWorkout("6 × 1 km", "14,2 km · 3:45/km en series", "17 nov 2026", "Sesión de referencia") }
    item { SavedWorkout("Test 5K", "18:21 · PB", "28 oct 2026", "Marca personal") }
    item { SavedWorkout("Tirada progresiva", "24 km · últimos 8 km a 4:06", "11 oct 2026", "Mejor tirada") }
    item { SavedWorkout("30 min umbral", "7,4 km · FC 168", "22 sep 2026", "Diagnóstico") }
}

private fun androidx.compose.foundation.lazy.LazyListScope.shoes() {
    item { SectionTitle("Zapatillas", "Rotación actual") }
    item { ShoeCard("ASICS Novablast 5", "Rodajes y tiradas", 482, 700, Color(0xFF5B8DEF)) }
    item { ShoeCard("Nike Vaporfly 3", "Series y competición", 128, 350, Color(0xFFE36B5D)) }
    item { ShoeCard("Adidas Adizero SL2", "Rodajes suaves", 611, 650, Color(0xFF2E9D63)) }
    item {
        KoreCard(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) {
            Label("Aviso")
            Text("Adizero SL2 cerca del límite", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Revisa su estado antes de asignarla a otra tirada larga.")
        }
    }
}

@Composable
private fun HistoryBlock(title: String, duration: String, distance: String, date: String, adherence: Int) {
    KoreCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Abrir bloque")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            HistoryMetric("Duración", duration)
            HistoryMetric("Volumen", distance)
            HistoryMetric("Cumplimiento", "$adherence %")
        }
    }
}

@Composable
private fun SavedWorkout(title: String, summary: String, date: String, badge: String) {
    KoreCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(date, style = MaterialTheme.typography.bodySmall)
            }
            Text(badge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ShoeCard(name: String, use: String, current: Int, limit: Int, color: Color) {
    KoreCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.DirectionsRun, contentDescription = null, tint = color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(use, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$current km", style = MaterialTheme.typography.titleMedium)
        }
        androidx.compose.material3.LinearProgressIndicator(
            progress = { (current.toFloat() / limit).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = color,
        )
        Text("Límite orientativo: $limit km", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HistoryMetric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
