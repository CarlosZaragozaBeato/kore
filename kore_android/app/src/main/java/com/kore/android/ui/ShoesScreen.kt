package com.kore.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Euro
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoesScreen(padding: PaddingValues) {
    var selected by remember { mutableStateOf<Shoe?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DemoBadge()
                    Text("Zapatillas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Cada kilómetro y cada etapa de su vida.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = {}) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Alta")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShoeSummary("3", "activas", Modifier.weight(1f))
                ShoeSummary("1.221", "km en rotación", Modifier.weight(1f))
                ShoeSummary("4", "retiradas", Modifier.weight(1f))
            }
        }
        item { SectionTitle("Rotación actual", "Por kilometraje") }
        items(MockData.shoes.size) { index ->
            ShoeCard(MockData.shoes[index]) { selected = MockData.shoes[index] }
        }
        item { SectionTitle("Histórico", "4 retiradas") }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Archive, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Nike Pegasus 39", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Retirada con 824 km · 2023–2024", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("4,3", fontWeight = FontWeight.Bold)
                    Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    selected?.let { shoe -> ShoeDetailSheet(shoe) { selected = null } }
}

@Composable
private fun ShoeSummary(value: String, label: String, modifier: Modifier) {
    KoreCard(modifier = modifier, containerColor = MaterialTheme.colorScheme.surfaceVariant) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ShoeCard(shoe: Shoe, onClick: () -> Unit) {
    KoreCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(14.dp).background(shoe.color, CircleShape))
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(shoe.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(shoe.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(shoe.status, style = MaterialTheme.typography.labelSmall, color = if (shoe.status == "ACTIVA") Color(0xFF2E9D63) else Color(0xFFE15B3D))
                Text("★ ${shoe.rating}", fontWeight = FontWeight.Bold)
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${shoe.currentKm}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(" / ${shoe.limitKm} km", modifier = Modifier.padding(bottom = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { (shoe.currentKm.toFloat() / shoe.limitKm).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            color = if (shoe.currentKm > shoe.limitKm * .9f) Color(0xFFE15B3D) else shoe.color,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            SmallFact("Alta", shoe.acquired)
            SmallFact("Precio", shoe.price)
        }
        Text(shoe.use, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoeDetailSheet(shoe: Shoe, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Rounded.DirectionsRun, contentDescription = null, tint = shoe.color, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(shoe.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text("${shoe.currentKm} km · ${shoe.price}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Label("Fases de uso")
                UsagePhase("0–200 km", "Series · Competición", true)
                UsagePhase("200–350 km", "Rodajes rápidos", shoe.currentKm >= 200)
                UsagePhase("350 km →", "Retirar o uso ocasional", shoe.currentKm >= 350)
            }
            item {
                Label("Valoración")
                RatingRow("Comodidad", 4.5f)
                RatingRow("Respuesta", 4.9f)
                RatingRow("Estabilidad", 4.0f)
                RatingRow("Agarre", 4.4f)
                RatingRow("Durabilidad", 4.2f)
            }
            item {
                Label("Notas")
                Text("Muy buena respuesta en ritmos de 10K. Reservar para calidad mientras mantenga la sensación de rebote.")
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Euro, contentDescription = null)
                    Text("Coste actual: 2,03 € por 10 km", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun UsagePhase(range: String, use: String, reached: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.size(9.dp).background(if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(range, modifier = Modifier.weight(.8f), fontWeight = FontWeight.Bold)
        Text(use, modifier = Modifier.weight(1.2f), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RatingRow(label: String, value: Float) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        LinearProgressIndicator(progress = { value / 5f }, modifier = Modifier.weight(1f).height(6.dp))
        Text(String.format(Locale.getDefault(), "%.1f", value), modifier = Modifier.width(38.dp), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallFact(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
