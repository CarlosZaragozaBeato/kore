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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
fun SettingsScreen(
    padding: PaddingValues,
    hasRealSession: Boolean,
    onImportSession: () -> Unit,
    onExportSession: () -> Unit,
) {
    var autoSync by remember { mutableStateOf(true) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoBadge()
                Text("Ajustes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Control local de tu sesión y fuentes de datos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { SectionTitle("Suunto") }
        item {
            KoreCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = Color(0xFF2E9D63))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Cuenta conectada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Última sincronización · Hoy, 08:42", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("ACTIVA", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E9D63))
                }
                SettingToggle(
                    title = "Sincronización automática",
                    subtitle = "Al abrir Kore y una vez al día",
                    checked = autoSync,
                    onCheckedChange = { autoSync = it },
                )
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text("Sincronizar ahora")
                }
                Text(
                    "Mockup: no existe conexión ni se usan credenciales reales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { SectionTitle("Sesión Kore") }
        item {
            KoreCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (hasRealSession) "Sesión local activa" else "Sesión de demostración", style = MaterialTheme.typography.titleMedium)
                        Text("Kore Session v1 · almacenamiento privado", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onImportSession, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Importar")
                    }
                    Button(
                        onClick = onExportSession,
                        modifier = Modifier.weight(1f),
                        enabled = hasRealSession,
                    ) {
                        Icon(Icons.Rounded.Upload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Exportar")
                    }
                }
            }
        }
        item { SectionTitle("Datos y privacidad") }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                SettingsInfoRow(Icons.Rounded.Lock, "Solo en tu dispositivo", "No hay backend ni cuenta Kore")
                SettingsInfoRow(Icons.Rounded.Storage, "Recursos de planificación", "Catálogo incluido en la aplicación")
                SettingsInfoRow(Icons.Rounded.CloudDone, "Suunto", "Fuente remota y detalle recuperable")
            }
        }
        item { SectionTitle("Planificación") }
        item {
            KoreCard {
                SettingsLink("Objetivo actual", "10K · 38:00")
                SettingsLink("Volumen habitual", "80–90 km/semana")
                SettingsLink("Días de carrera", "6 días")
                SettingsLink("Ritmos y zonas", "VO₂max 56 · revisar")
            }
        }
        item {
            Text(
                "Kore 0.1 · Prototipo visual",
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsLink(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
