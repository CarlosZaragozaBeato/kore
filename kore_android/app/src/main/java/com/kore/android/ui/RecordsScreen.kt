package com.kore.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Museum
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RecordsScreen(padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DemoBadge()
                    Text("Marcas personales", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("La mejor de hoy y todas las que te trajeron hasta aquí.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = {}) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Añadir")
                }
            }
        }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.inverseSurface) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("4 distancias principales", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.inverseOnSurface, fontWeight = FontWeight.Bold)
                        Text("11 mejoras registradas desde 2021", color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = .67f))
                    }
                }
            }
        }
        item { SectionTitle("Marcas actuales", "Verificadas y manuales") }
        items(MockData.currentBests.size) { index ->
            CurrentBestCard(MockData.currentBests[index], index)
        }
        item { SectionTitle("Historia del 10K", "4 marcas") }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                TimelineEntry("38:47", "12 MAY 2026", "Carrera oficial", true)
                TimelineEntry("39:28", "03 NOV 2025", "Carrera oficial", false)
                TimelineEntry("40:12", "16 MAR 2025", "Entrenamiento", false)
                TimelineEntry("42:06", "08 OCT 2023", "Primera referencia", false)
            }
        }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) {
                Label("Evidencia")
                Text("Cada marca conserva su origen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Carrera oficial, entrenamiento, estimación o entrada manual siempre aparecen diferenciados.")
            }
        }
    }
}

@Composable
private fun CurrentBestCard(pb: PersonalBest, index: Int) {
    KoreCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(.55f)) {
                Text(pb.distance, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(pb.pace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                Text(pb.time, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(pb.date, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(pb.improvement, color = Color(0xFF2E9D63), fontWeight = FontWeight.Bold)
                if (index < 3) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Museum, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Museo", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineEntry(time: String, date: String, source: String, current: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(if (current) 13.dp else 9.dp).background(
                    if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    CircleShape,
                ),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(date, style = MaterialTheme.typography.labelSmall)
    }
}
