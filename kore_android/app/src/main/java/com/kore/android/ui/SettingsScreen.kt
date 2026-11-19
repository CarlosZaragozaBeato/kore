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
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    hasRealSession: Boolean,
    onImportSession: () -> Unit,
    onExportSession: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoBadge()
                Text("Ajustes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Tu archivo, tus fuentes, tus datos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { SectionTitle("Fuentes de actividad") }
        item {
            KoreCard {
                SourceRow("Suunto", "Conectada · Hoy, 08:42", true)
                SourceRow("Garmin", "Disponible en el futuro", false)
                SourceRow("COROS", "Disponible en el futuro", false)
                SourceRow("FIT / GPX", "Importación de archivos", false)
                Text(
                    "Kore convierte todas las fuentes a actividades propias. Ninguna pantalla depende de Suunto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { SectionTitle("Sesión Kore") }
        item {
            KoreCard {
                SettingsInfoRow(
                    Icons.Rounded.Storage,
                    if (hasRealSession) "Archivo local activo" else "Archivo de demostración",
                    "Kore Session v2 · sin base de datos",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onImportSession, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Importar")
                    }
                    Button(onClick = onExportSession, modifier = Modifier.weight(1f), enabled = hasRealSession) {
                        Icon(Icons.Rounded.Upload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Exportar")
                    }
                }
            }
        }
        item { SectionTitle("Privacidad") }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                SettingsInfoRow(Icons.Rounded.Lock, "Solo en tu dispositivo", "Sin cuenta Kore, backend o nube propia")
                SettingsInfoRow(Icons.Rounded.Extension, "Credenciales separadas", "Nunca viajan dentro de la sesión")
                SettingsInfoRow(Icons.Rounded.Storage, "Adjuntos opcionales", "FIT y GPS fuera del JSON principal")
            }
        }
        item {
            Text(
                "Kore 0.2 · Archivo runner · Prototipo visual",
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SourceRow(name: String, state: String, connected: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (connected) Icons.Rounded.CloudDone else Icons.Rounded.Extension,
            contentDescription = null,
            tint = if (connected) Color(0xFF2E9D63) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(state, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(if (connected) "ACTIVA" else "", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E9D63))
    }
}

@Composable
private fun SettingsInfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
