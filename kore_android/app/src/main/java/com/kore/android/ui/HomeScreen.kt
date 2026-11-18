package com.kore.android.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    profileLabel: String,
    padding: PaddingValues,
    onOpenPlan: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoBadge()
                Text(
                    "Buenos días, $profileLabel",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "Miércoles, 18 de noviembre",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.inverseSurface) {
                LabelOnDark("OBJETIVO ACTUAL")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "10K",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "SUB 38:00",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Text(
                    "Bloque específico · Semana 3 de 8",
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = .72f),
                )
                LinearProgressIndicator(
                    progress = { .375f },
                    modifier = Modifier.fillMaxWidth().height(7.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = .16f),
                )
            }
        }
        item {
            SectionTitle("Hoy", "Semana completa")
        }
        item {
            KoreCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Rounded.DirectionsRun,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Label("Rodaje fácil")
                        Text("12 km + 6 strides", style = MaterialTheme.typography.titleLarge)
                    }
                    Text("DÍA 17", style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    "Mantén un esfuerzo cómodo. Añade seis progresivos de 20 segundos si terminas con buenas sensaciones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    MiniMetric("Ritmo", "5:05–5:30")
                    MiniMetric("Pulso", "Z1–Z2")
                    MiniMetric("Carga", "Baja")
                }
                Button(onClick = onOpenPlan, modifier = Modifier.fillMaxWidth()) {
                    Text("Ver sesión")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KoreCard(modifier = Modifier.weight(1f), containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    Label("Esta semana")
                    Text("36 / 84 km", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { 36f / 84f },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                    )
                }
                KoreCard(modifier = Modifier.weight(1f), containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    Label("Estado")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("78", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null, tint = Color(0xFF2E9D63))
                    }
                    Text("+3 este mes", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { SectionTitle("Último entrenamiento", "Historial") }
        item {
            KoreCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF2E9D63))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("6 × 1 km", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Ayer · Sincronizado con Suunto", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("14,2 km", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "Series estables entre 3:42 y 3:47/km. Última repetición controlada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .13f)) {
                Label("Recomendación Kore")
                Text("La carga progresa bien", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Completa hoy el rodaje fácil. Si la fatiga aumenta, elimina los strides antes de tocar la sesión de umbral.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Ver por qué",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun MiniMetric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LabelOnDark(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = .65f),
    )
}
