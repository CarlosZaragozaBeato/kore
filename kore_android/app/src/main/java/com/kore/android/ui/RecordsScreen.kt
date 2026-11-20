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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.kore.session.MuseumEntry
import com.kore.session.PersonalBestInput
import com.kore.session.RunnerActivity
import com.kore.session.RunnerPersonalBest
import java.util.Locale

@Composable
fun RecordsScreen(
    padding: PaddingValues,
    records: List<RunnerPersonalBest>?,
    activities: List<RunnerActivity>,
    museumEntries: List<MuseumEntry>,
    onAddPersonalBest: (PersonalBestInput) -> Unit,
) {
    if (records == null) {
        DemoRecords(padding)
        return
    }
    val current = records.groupBy { it.distanceMeters }.values
        .mapNotNull { group -> group.minByOrNull { it.durationMillis } }
        .sortedBy { it.distanceMeters }
    var showForm by remember { mutableStateOf(false) }
    var selectedDistance by remember { mutableStateOf<Double?>(null) }
    val historyDistance = selectedDistance ?: current.firstOrNull()?.distanceMeters
    val history = records.filter { it.distanceMeters == historyDistance }.sortedByDescending { it.achievedAt }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Marcas personales", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("La mejor de hoy y todas las que te trajeron hasta aquí.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { showForm = true }) {
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
                        Text("${current.size} distancias", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.inverseOnSurface, fontWeight = FontWeight.Bold)
                        Text("${records.size} marcas conservadas", color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = .67f))
                    }
                }
            }
        }
        if (current.isEmpty()) {
            item {
                KoreCard(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) {
                    Text("Registra tu primera referencia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Puede ser una carrera oficial, un entrenamiento, una estimación o una entrada manual.")
                    Button(onClick = { showForm = true }, modifier = Modifier.fillMaxWidth()) { Text("Añadir marca") }
                }
            }
        } else {
            item { SectionTitle("Marcas actuales", "Mejor tiempo por distancia") }
            items(current.size) { index ->
                val record = current[index]
                val previous = records.filter { it.distanceMeters == record.distanceMeters && it.id != record.id }
                    .minByOrNull { it.durationMillis }
                CurrentRecordCard(record, previous)
            }
            item {
                SectionTitle("Cronología")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    current.take(4).forEach { record ->
                        FilterChip(
                            selected = record.distanceMeters == historyDistance,
                            onClick = { selectedDistance = record.distanceMeters },
                            label = { Text(distanceLabel(record.distanceMeters)) },
                        )
                    }
                }
            }
            item {
                KoreCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    history.forEach { record ->
                        TimelineEntry(
                            time = durationLabel(record.durationMillis),
                            date = record.achievedAt,
                            source = evidenceLabel(record.evidenceType),
                            current = record.id == current.find { it.distanceMeters == historyDistance }?.id,
                        )
                    }
                }
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

    if (showForm) {
        RecordForm(
            activities = activities,
            museumEntries = museumEntries,
            onDismiss = { showForm = false },
            onSave = { onAddPersonalBest(it); showForm = false },
        )
    }
}

@Composable
private fun CurrentRecordCard(record: RunnerPersonalBest, previous: RunnerPersonalBest?) {
    val improvement = previous?.let { it.durationMillis - record.durationMillis }?.takeIf { it > 0 }
    KoreCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(.55f)) {
                Text(distanceLabel(record.distanceMeters), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(paceLabel(record), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                Text(durationLabel(record.durationMillis), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(record.achievedAt, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                improvement?.let { Text("−${durationLabel(it)}", color = Color(0xFF2E9D63), fontWeight = FontWeight.Bold) }
                Text(evidenceLabel(record.evidenceType), style = MaterialTheme.typography.labelSmall)
                if (record.museumEntryId != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Museum, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Museo", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        record.notes?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordForm(
    activities: List<RunnerActivity>,
    museumEntries: List<MuseumEntry>,
    onDismiss: () -> Unit,
    onSave: (PersonalBestInput) -> Unit,
) {
    var distanceOption by remember { mutableStateOf("5K") }
    var customDistance by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf("manual") }
    var activityId by remember { mutableStateOf<String?>(null) }
    var museumEntryId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    val distance = when (distanceOption) {
        "5K" -> 5_000.0
        "10K" -> 10_000.0
        "21,1K" -> 21_097.5
        "42,2K" -> 42_195.0
        else -> customDistance.replace(',', '.').toDoubleOrNull()?.times(1_000)
    }
    val duration = parseDurationMillis(time)
    val valid = distance != null && distance > 0 && duration != null && duration > 0 && date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Text("Nueva marca", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("Todas las referencias permanecen en la cronología", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Text("Distancia", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("5K", "10K", "21,1K", "42,2K").forEach { option ->
                        FilterChip(selected = distanceOption == option, onClick = { distanceOption = option }, label = { Text(option) })
                    }
                }
                FilterChip(selected = distanceOption == "Otra", onClick = { distanceOption = "Otra" }, label = { Text("Otra distancia") })
                if (distanceOption == "Otra") {
                    OutlinedTextField(customDistance, { customDistance = it }, label = { Text("Distancia km") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(time, { time = it }, label = { Text("Tiempo · HH:MM:SS") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(date, { date = it }, label = { Text("AAAA-MM-DD") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
            item {
                Text("Evidencia", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "official_race" to "Carrera oficial",
                        "training" to "Entrenamiento",
                        "estimate" to "Estimación",
                        "manual" to "Manual",
                    ).forEach { (value, label) ->
                        FilterChip(selected = evidence == value, onClick = { evidence = value }, label = { Text(label) })
                    }
                }
            }
            if (museumEntries.isNotEmpty()) {
                item {
                    Text("Pieza del Museo (opcional)", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        museumEntries.forEach { entry ->
                            FilterChip(
                                selected = museumEntryId == entry.id,
                                onClick = {
                                    val selecting = museumEntryId != entry.id
                                    museumEntryId = if (selecting) entry.id else null
                                    activityId = if (selecting) entry.activityId else null
                                },
                                label = { Text(entry.title) },
                            )
                        }
                    }
                }
            }
            if (activities.isNotEmpty()) {
                item {
                    Text("Actividad (opcional)", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        activities.take(8).forEach { activity ->
                            FilterChip(
                                selected = activityId == activity.id,
                                onClick = {
                                    activityId = if (activityId == activity.id) null else activity.id
                                    museumEntryId = null
                                },
                                label = { Text("${activity.date} · ${activity.title}") },
                            )
                        }
                    }
                }
            }
            item { OutlinedTextField(notes, { notes = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
            item {
                Button(
                    onClick = { onSave(PersonalBestInput(distance!!, duration!!, date, evidence, activityId, museumEntryId, notes.ifBlank { null })) },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Guardar marca") }
            }
        }
    }
}

@Composable
private fun TimelineEntry(time: String, date: String, source: String, current: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(if (current) 13.dp else 9.dp).background(if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(date, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DemoRecords(padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { DemoBadge(); Text("Marcas personales", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }
        item { SectionTitle("Marcas actuales", "Demostración") }
        items(MockData.currentBests.size) { index ->
            val pb = MockData.currentBests[index]
            KoreCard {
                Text(pb.distance, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(pb.time, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("${pb.pace} · ${pb.date}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun parseDurationMillis(text: String): Long? {
    val parts = text.trim().split(':').map { it.toLongOrNull() ?: return null }
    if (parts.size !in 2..3 || parts.any { it < 0 }) return null
    val hours = if (parts.size == 3) parts[0] else 0
    val minutes = if (parts.size == 3) parts[1] else parts[0]
    val seconds = parts.last()
    if (minutes >= 60 || seconds >= 60) return null
    return (hours * 3_600 + minutes * 60 + seconds) * 1_000
}

private fun distanceLabel(meters: Double): String = when {
    meters == 5_000.0 -> "5K"
    meters == 10_000.0 -> "10K"
    meters == 21_097.5 -> "21,1K"
    meters == 42_195.0 -> "42,2K"
    else -> String.format(Locale.getDefault(), "%.2fK", meters / 1_000)
}

private fun durationLabel(millis: Long): String {
    val totalSeconds = millis / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

private fun paceLabel(record: RunnerPersonalBest): String {
    val secondsPerKm = (record.durationMillis / 1_000.0 / (record.distanceMeters / 1_000.0)).toLong()
    return "%d:%02d/km".format(secondsPerKm / 60, secondsPerKm % 60)
}

private fun evidenceLabel(type: String): String = when (type) {
    "official_race" -> "Carrera oficial"
    "training" -> "Entrenamiento"
    "estimate" -> "Estimación"
    else -> "Manual"
}
