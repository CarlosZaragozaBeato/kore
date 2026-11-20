package com.kore.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Museum
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.kore.session.ActivityInput
import com.kore.session.MuseumEntry
import com.kore.session.MuseumEntryInput
import com.kore.session.RunnerActivity
import com.kore.session.RunnerShoe
import java.util.Locale

@Composable
fun MuseumScreen(
    padding: PaddingValues,
    activities: List<RunnerActivity>?,
    entries: List<MuseumEntry>?,
    shoes: List<RunnerShoe>,
    onAddActivity: (ActivityInput) -> Unit,
    onAddMuseumEntry: (MuseumEntryInput) -> Unit,
) {
    if (activities == null || entries == null) {
        DemoMuseum(padding)
        return
    }
    val archivedActivityIds = entries.mapTo(mutableSetOf()) { it.activityId }
    val candidates = activities.filterNot { it.id in archivedActivityIds }.sortedByDescending { it.date }
    var showActivityForm by remember { mutableStateOf(false) }
    var selectedActivity by remember { mutableStateOf<RunnerActivity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Museo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Sesiones que merecen ser recordadas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { showActivityForm = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Actividad")
                }
            }
        }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${activities.size} actividades · ${entries.size} piezas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Solo tú decides qué actividad entra en el Museo.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (candidates.isNotEmpty()) {
            item { SectionTitle("Elige una actividad", "${candidates.size} pendientes") }
            items(candidates.size) { index ->
                ActivityCandidateCard(candidates[index]) { selectedActivity = candidates[index] }
            }
        } else if (activities.isEmpty()) {
            item {
                KoreCard(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) {
                    Icon(Icons.Rounded.Museum, contentDescription = null)
                    Text("Registra tu primera actividad", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Esta entrada manual utiliza el mismo idioma que usarán Suunto, GPX u otros proveedores.")
                    Button(onClick = { showActivityForm = true }, modifier = Modifier.fillMaxWidth()) { Text("Añadir actividad") }
                }
            }
        }
        if (entries.isNotEmpty()) {
            item { SectionTitle("Tu colección", "${entries.size} piezas") }
            items(entries.size) { index ->
                RealMuseumCard(entries.sortedByDescending { it.date }[index], shoes, index == 0)
            }
        } else if (activities.isNotEmpty()) {
            item {
                KoreCard(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) {
                    Text("El Museo todavía está vacío", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Selecciona una actividad y cuenta por qué merece quedarse aquí.")
                }
            }
        }
    }

    if (showActivityForm) {
        ActivityForm(
            onDismiss = { showActivityForm = false },
            onSave = { onAddActivity(it); showActivityForm = false },
        )
    }
    selectedActivity?.let { activity ->
        MuseumEntryForm(
            activity = activity,
            shoes = shoes,
            onDismiss = { selectedActivity = null },
            onSave = { onAddMuseumEntry(it); selectedActivity = null },
        )
    }
}

@Composable
private fun ActivityCandidateCard(activity: RunnerActivity, onClick: () -> Unit) {
    KoreCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(activity.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${activity.date} · ${activity.sport.replaceFirstChar { it.uppercase() }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatDuration(activity.durationSeconds), fontWeight = FontWeight.Bold)
        }
        Text("${formatDistance(activity.distanceMeters)} · ${formatPace(activity)}", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text("Guardar en el Museo") }
    }
}

@Composable
private fun RealMuseumCard(entry: MuseumEntry, shoes: List<RunnerShoe>, featured: Boolean) {
    val color = if (featured) Color(0xFFE15B3D) else MaterialTheme.colorScheme.surface
    val onColor = if (featured) Color.White else MaterialTheme.colorScheme.onSurface
    KoreCard(containerColor = color) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(if (featured) Color.White else MaterialTheme.colorScheme.primary, CircleShape))
            Spacer(Modifier.width(9.dp))
            Text(entry.motive.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = onColor)
            Spacer(Modifier.weight(1f))
            Text(entry.date, style = MaterialTheme.typography.labelSmall, color = onColor.copy(alpha = .72f))
        }
        Text(entry.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = onColor)
        Text("${formatDistance(entry.distanceMeters)} · ${formatDuration(entry.durationSeconds)}", style = MaterialTheme.typography.titleMedium, color = onColor)
        Text(entry.story, color = onColor.copy(alpha = .9f))
        if (entry.tags.isNotEmpty()) Text(entry.tags.joinToString("  ") { "#$it" }, style = MaterialTheme.typography.bodySmall, color = onColor.copy(alpha = .72f))
        val shoe = shoes.find { it.id == entry.shoeId }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Link, contentDescription = null, tint = onColor, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(shoe?.let { "Actividad vinculada · ${it.name}" } ?: "Actividad vinculada", style = MaterialTheme.typography.bodySmall, color = onColor.copy(alpha = .72f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityForm(onDismiss: () -> Unit, onSave: (ActivityInput) -> Unit) {
    var title by remember { mutableStateOf("") }
    var sport by remember { mutableStateOf("running") }
    var date by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("") }
    var distanceKm by remember { mutableStateOf("") }
    var elevation by remember { mutableStateOf("") }
    var heartRate by remember { mutableStateOf("") }
    val duration = durationMinutes.decimalOrNull()
    val distance = distanceKm.decimalOrNull()
    val elevationValue = elevation.toIntOrNull()
    val heartRateValue = heartRate.toIntOrNull()
    val valid = title.isNotBlank() && date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && duration != null && duration > 0 &&
        distance != null && distance >= 0 && (elevation.isBlank() || elevationValue != null && elevationValue >= 0) &&
        (heartRate.isBlank() || heartRateValue != null && heartRateValue in 30..250)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Text("Registrar actividad", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("Entrada manual en el modelo canónico de Kore", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { OutlinedTextField(title, { title = it }, label = { Text("Nombre de la sesión") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item {
                Text("Deporte", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("running", "trail", "walking").forEach { option ->
                        FilterChip(selected = sport == option, onClick = { sport = option }, label = { Text(option.replaceFirstChar { it.uppercase() }) })
                    }
                }
            }
            item { OutlinedTextField(date, { date = it }, label = { Text("Fecha · AAAA-MM-DD") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(durationMinutes, { durationMinutes = it }, label = { Text("Duración min") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(distanceKm, { distanceKm = it }, label = { Text("Distancia km") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(elevation, { elevation = it }, label = { Text("Desnivel m") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(heartRate, { heartRate = it }, label = { Text("FC media") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
            item {
                Button(
                    onClick = { onSave(ActivityInput(title, sport, date, (duration!! * 60).toInt(), distance!! * 1_000, elevationValue, heartRateValue)) },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Guardar actividad") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuseumEntryForm(
    activity: RunnerActivity,
    shoes: List<RunnerShoe>,
    onDismiss: () -> Unit,
    onSave: (MuseumEntryInput) -> Unit,
) {
    var title by remember { mutableStateOf(activity.title) }
    var motive by remember { mutableStateOf("Memorable") }
    var story by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var shoeId by remember { mutableStateOf<String?>(null) }
    val valid = title.isNotBlank() && motive.isNotBlank() && story.isNotBlank()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Text("Crear pieza", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("${activity.date} · ${formatDistance(activity.distanceMeters)} · ${formatDuration(activity.durationSeconds)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { OutlinedTextField(title, { title = it }, label = { Text("Título del recuerdo") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Text("Motivo", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Memorable", "PB", "Carrera").forEach { option ->
                        FilterChip(selected = motive == option, onClick = { motive = option }, label = { Text(option) })
                    }
                }
            }
            item { OutlinedTextField(story, { story = it }, label = { Text("¿Por qué quieres recordarla?") }, modifier = Modifier.fillMaxWidth(), minLines = 4) }
            item { OutlinedTextField(tags, { tags = it }, label = { Text("Etiquetas separadas por comas") }, modifier = Modifier.fillMaxWidth()) }
            if (shoes.isNotEmpty()) {
                item {
                    Text("Zapatilla vinculada", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        shoes.forEach { shoe ->
                            FilterChip(selected = shoeId == shoe.id, onClick = { shoeId = if (shoeId == shoe.id) null else shoe.id }, label = { Text(shoe.name) })
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = { onSave(MuseumEntryInput(activity.id, title, motive, story, tags.split(','), shoeId)) },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Incorporar al Museo") }
            }
        }
    }
}

@Composable
private fun DemoMuseum(padding: PaddingValues) {
    var filter by remember { mutableStateOf("Todas") }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DemoBadge()
                Text("Museo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Sesiones que merecen ser recordadas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todas", "PB", "Carreras", "Memorables").forEach { option ->
                    FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option) })
                }
            }
        }
        items(MockData.museum.size) { index -> DemoMuseumCard(MockData.museum[index], index == 0) }
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
private fun DemoMuseumCard(piece: MuseumPiece, featured: Boolean) {
    KoreCard(containerColor = if (featured) piece.color else MaterialTheme.colorScheme.surface) {
        Text(piece.badge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (featured) Color.White else piece.color)
        Text(piece.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = if (featured) Color.White else MaterialTheme.colorScheme.onSurface)
        Text("${piece.distance} · ${piece.time}", style = MaterialTheme.typography.titleMedium, color = if (featured) Color.White else MaterialTheme.colorScheme.onSurface)
        Text(piece.story, color = if (featured) Color.White.copy(alpha = .9f) else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun String.decimalOrNull(): Double? = replace(',', '.').toDoubleOrNull()
private fun formatDistance(meters: Double): String = String.format(Locale.getDefault(), "%.2f km", meters / 1_000)
private fun formatDuration(seconds: Int): String = "%d:%02d:%02d".format(seconds / 3_600, (seconds % 3_600) / 60, seconds % 60)
private fun formatPace(activity: RunnerActivity): String {
    if (activity.distanceMeters <= 0) return "Sin ritmo"
    val secondsPerKm = (activity.durationSeconds / (activity.distanceMeters / 1_000)).toInt()
    return "%d:%02d/km".format(secondsPerKm / 60, secondsPerKm % 60)
}
