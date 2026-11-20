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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kore.android.session.SuuntoUiState
import com.kore.android.suunto.SuuntoCredentials

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    padding: PaddingValues,
    hasRealSession: Boolean,
    onImportSession: () -> Unit,
    onExportSession: () -> Unit,
    onCreateArchive: () -> Unit,
    demo: Boolean,
    suuntoState: SuuntoUiState,
    onSaveSuuntoCredentials: (SuuntoCredentials) -> Unit,
    onSynchronizeSuunto: (Int) -> Unit,
    onDisconnectSuunto: () -> Unit,
) {
    var showSuuntoForm by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (demo) DemoBadge()
                Text("Ajustes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Tu archivo, tus fuentes, tus datos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { SectionTitle("Fuentes de actividad") }
        item {
            KoreCard {
                SourceRow(
                    "Suunto",
                    when {
                        demo -> "Conectada · Demostración"
                        suuntoState.syncing -> "Sincronizando entrenamientos…"
                        suuntoState.configured -> "Credenciales locales configuradas"
                        else -> "No conectada"
                    },
                    demo || suuntoState.configured,
                )
                if (!demo) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showSuuntoForm = true }, modifier = Modifier.weight(1f)) {
                            Text(if (suuntoState.configured) "Reconfigurar" else "Configurar")
                        }
                        Button(
                            onClick = { onSynchronizeSuunto(30) },
                            modifier = Modifier.weight(1f),
                            enabled = hasRealSession && suuntoState.configured && !suuntoState.syncing,
                        ) { Text(if (suuntoState.syncing) "Sincronizando" else "Últimos 30 días") }
                    }
                    suuntoState.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (suuntoState.configured) {
                        OutlinedButton(
                            onClick = { onSynchronizeSuunto(3650) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = hasRealSession && !suuntoState.syncing,
                        ) { Text("Recuperar histórico · hasta 10 años") }
                        OutlinedButton(onClick = onDisconnectSuunto, modifier = Modifier.fillMaxWidth(), enabled = !suuntoState.syncing) {
                            Text("Eliminar credenciales de Suunto")
                        }
                    }
                }
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
                if (!hasRealSession) {
                    Button(onClick = onCreateArchive, modifier = Modifier.fillMaxWidth()) {
                        Text("Crear mi archivo real")
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
                "Kore 0.7 · Archivo runner · Suunto local",
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showSuuntoForm) {
        SuuntoCredentialsForm(
            onDismiss = { showSuuntoForm = false },
            onSave = {
                onSaveSuuntoCredentials(it)
                showSuuntoForm = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuuntoCredentialsForm(
    onDismiss: () -> Unit,
    onSave: (SuuntoCredentials) -> Unit,
) {
    var clientId by remember { mutableStateOf("") }
    var clientSecret by remember { mutableStateOf("") }
    var subscriptionKey by remember { mutableStateOf("") }
    var refreshToken by remember { mutableStateOf("") }
    val valid = clientId.isNotBlank() && clientSecret.isNotBlank() && subscriptionKey.isNotBlank() && refreshToken.isNotBlank()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Conectar Suunto", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("Configuración personal avanzada", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                KoreCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    Icon(Icons.Rounded.Lock, contentDescription = null)
                    Text("Los cuatro valores se cifran con Android Keystore.", fontWeight = FontWeight.Bold)
                    Text("No se guardan en la sesión Kore, no se exportan y no están incluidos en el APK.", style = MaterialTheme.typography.bodySmall)
                }
            }
            item { OutlinedTextField(clientId, { clientId = it }, label = { Text("Client ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item { SecretField(clientSecret, { clientSecret = it }, "Client Secret") }
            item { SecretField(subscriptionKey, { subscriptionKey = it }, "Subscription Key") }
            item { SecretField(refreshToken, { refreshToken = it }, "Refresh Token") }
            item {
                Button(
                    onClick = { onSave(SuuntoCredentials(clientId, clientSecret, subscriptionKey, refreshToken)) },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Guardar cifrado") }
            }
        }
    }
}

@Composable
private fun SecretField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
    )
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
