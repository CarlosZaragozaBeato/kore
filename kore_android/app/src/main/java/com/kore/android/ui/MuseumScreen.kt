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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Museum
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

@Composable
fun MuseumScreen(padding: PaddingValues) {
    var filter by remember { mutableStateOf("Todas") }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoBadge()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Museo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text("Sesiones que merecen ser recordadas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = {}) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Añadir")
                    }
                }
            }
        }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Elige un día de tu historia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Busca una actividad importada y conviértela en una pieza.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Explorar calendario") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todas", "PB", "Carreras", "Memorables").forEach { option ->
                    FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option) })
                }
            }
        }
        items(MockData.museum.size) { index ->
            MuseumCard(MockData.museum[index], featured = index == 0)
        }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) {
                Icon(Icons.Rounded.Museum, contentDescription = null)
                Text("El museo crece solo cuando tú decides", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Sin algoritmos seleccionando por ti: cada pieza tiene un motivo y una historia personal.")
            }
        }
    }
}

@Composable
private fun MuseumCard(piece: MuseumPiece, featured: Boolean) {
    KoreCard(containerColor = if (featured) piece.color else MaterialTheme.colorScheme.surface) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(12.dp).background(
                    if (featured) Color.White else piece.color,
                    CircleShape,
                ),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                piece.badge,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (featured) Color.White.copy(alpha = .82f) else piece.color,
            )
            Spacer(Modifier.weight(1f))
            Text(
                piece.date,
                style = MaterialTheme.typography.labelSmall,
                color = if (featured) Color.White.copy(alpha = .72f) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            piece.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = if (featured) Color.White else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "${piece.distance}  ·  ${piece.time}",
            style = MaterialTheme.typography.titleMedium,
            color = if (featured) Color.White else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            piece.story,
            color = if (featured) Color.White.copy(alpha = .9f) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Link,
                contentDescription = null,
                tint = if (featured) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (piece.badge.contains("PB")) "Vinculada a marca personal y Vaporfly 3" else "Actividad y zapatilla vinculadas",
                style = MaterialTheme.typography.bodySmall,
                color = if (featured) Color.White.copy(alpha = .72f) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
