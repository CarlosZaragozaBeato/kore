package com.kore.android.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Museum
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    profileLabel: String,
    padding: PaddingValues,
    demo: Boolean,
    routesCount: Int,
    onOpenSettings: () -> Unit,
) {
    if (!demo) {
        RealHomeScreen(profileLabel, routesCount, padding, onOpenSettings)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoBadge()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("El archivo de $profileLabel", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Abrir ajustes")
                    }
                }
                Text("Tu historia como runner, conservada por ti.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.inverseSurface) {
                LabelDark("TODA UNA VIDA CORRIENDO")
                Text("18.642 km", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.inverseOnSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                    DarkMetric("1.384", "actividades")
                    DarkMetric("12", "marcas")
                    DarkMetric("7", "zapatillas")
                }
                Text("Desde octubre de 2019", color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = .62f))
            }
        }
        item { SectionTitle("Pieza destacada", "Entrar al museo") }
        item {
            val piece = MockData.museum.first()
            KoreCard(containerColor = piece.color) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Museum, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text(piece.badge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = .78f))
                }
                Text(piece.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
                Text("${piece.date}  ·  ${piece.distance}  ·  ${piece.time}", color = Color.White.copy(alpha = .82f))
                Text(piece.story, color = Color.White)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Ver recuerdo", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Color.White)
                }
            }
        }
        item { SectionTitle("Última marca personal", "Ver todas") }
        item {
            val pb = MockData.currentBests.first()
            KoreCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Label(pb.distance)
                        Text(pb.time, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(pb.improvement, color = Color(0xFF2E9D63), fontWeight = FontWeight.Bold)
                        Text(pb.date, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("Vinculada a «Test 5K» · Nike Vaporfly 3", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { SectionTitle("Rotación actual", "3 activas") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MockData.shoes.take(2).forEach { shoe ->
                    KoreCard(modifier = Modifier.weight(1f), containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Spacer(Modifier.size(12.dp).background(shoe.color, CircleShape))
                        Text(shoe.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${shoe.currentKm} km", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text(shoe.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .13f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Rounded.DirectionsRun, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Adizero SL2 · 611 km", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Cerca de tu límite orientativo de 650 km.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Abrir zapatillas")
                }
            }
        }
    }
}

@Composable
private fun RealHomeScreen(
    profileLabel: String,
    routesCount: Int,
    padding: PaddingValues,
    onOpenSettings: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("El archivo de $profileLabel", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Tu archivo local está preparado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Abrir ajustes")
                }
            }
        }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.inverseSurface) {
                LabelDark("TU ARCHIVO KORE")
                Text("$routesCount rutas", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.inverseOnSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                    DarkMetric("0", "actividades")
                    DarkMetric("0", "marcas")
                    DarkMetric("0", "zapatillas")
                }
            }
        }
        item { SectionTitle("Empieza tu historia") }
        item {
            KoreCard {
                Text("1 · Guarda una ruta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("La sección Rutas ya guarda recorridos reales dentro de tu sesión.")
                Text("2 · Añade tus zapatillas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Será el siguiente flujo funcional.")
                Text("3 · Conecta actividades", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Después podrás llevar sesiones especiales al Museo y vincular PB.")
            }
        }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .13f)) {
                Text("Sin datos ficticios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("En tu archivo real solo se muestran elementos que hayas guardado o importado.")
            }
        }
    }
}

@Composable
private fun DarkMetric(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.inverseOnSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = .62f))
    }
}

@Composable
private fun LabelDark(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}
