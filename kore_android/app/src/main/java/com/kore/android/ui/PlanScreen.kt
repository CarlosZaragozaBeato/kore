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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(padding: PaddingValues) {
    var selectedDay by remember { mutableIntStateOf(2) }
    var showBuilder by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DemoBadge()
                    Text("Plan 10K · Sub 38", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Semana 3 de 8 · 16–22 nov", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { showBuilder = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Nuevo")
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MockData.week.size) { index ->
                    val day = MockData.week[index]
                    DayPicker(
                        day = day,
                        selected = selectedDay == index,
                        onClick = { selectedDay = index },
                    )
                }
            }
        }
        item {
            val day = MockData.week[selectedDay]
            KoreCard(
                containerColor = if (day.status == MockDayStatus.Rest) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else MaterialTheme.colorScheme.surface,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Label(day.type)
                        Text(day.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(day.distance, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
                if (day.status != MockDayStatus.Rest) {
                    Text(
                        if (selectedDay == 2) "12 km suaves + 6 × 20 s progresivos · recuperación completa caminando o trotando."
                        else "Calentamiento, bloque principal y vuelta a la calma adaptados al objetivo del día.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text("5:05–5:30/km") })
                        AssistChip(onClick = {}, label = { Text("Z1–Z2") })
                    }
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                        Text(if (day.status == MockDayStatus.Completed) "Ver resultado Suunto" else "Ver detalle de sesión")
                    }
                } else {
                    Text("Día libre de carrera. Movilidad opcional y recuperación.")
                }
            }
        }
        item { SectionTitle("Semana", "84 km previstos") }
        items(MockData.week.size) { index ->
            val day = MockData.week[index]
            WeekRow(day, selectedDay == index) { selectedDay = index }
        }
        item {
            KoreCard(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) {
                Label("Flexibilidad del bloque")
                Text("Adaptar próxima semana", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Kore podrá ajustar dosis, sesión específica o día de descanso según ejecución, fatiga y objetivo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Revisar propuesta")
                }
            }
        }
    }

    if (showBuilder) {
        PlanBuilderSheet(onDismiss = { showBuilder = false })
    }
}

@Composable
private fun DayPicker(day: MockPlanDay, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(52.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            day.day,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            day.date,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(
            Modifier.size(6.dp).background(
                when (day.status) {
                    MockDayStatus.Completed -> Color(0xFF2E9D63)
                    MockDayStatus.Today -> MaterialTheme.colorScheme.primary
                    MockDayStatus.Planned -> MaterialTheme.colorScheme.outlineVariant
                    MockDayStatus.Rest -> Color.Transparent
                },
                CircleShape,
            ),
        )
    }
}

@Composable
private fun WeekRow(day: MockPlanDay, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                else MaterialTheme.colorScheme.surface,
                MaterialTheme.shapes.medium,
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(day.day, style = MaterialTheme.typography.labelSmall)
            Text(day.date, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text(day.title, style = MaterialTheme.typography.titleMedium)
            Text(day.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(day.distance, style = MaterialTheme.typography.labelLarge)
        if (day.status == MockDayStatus.Completed) {
            Icon(Icons.Rounded.Check, contentDescription = "Completado", tint = Color(0xFF2E9D63))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanBuilderSheet(onDismiss: () -> Unit) {
    var distance by remember { mutableStateOf("10K") }
    var weeks by remember { mutableFloatStateOf(8f) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Crear nuevo plan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Configura el bloque a tu medida", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Label("Distancia principal")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("5K", "10K", "21K", "42K").forEach { option ->
                        FilterChip(
                            selected = distance == option,
                            onClick = { distance = option },
                            label = { Text(option) },
                        )
                    }
                }
            }
            item {
                Label("Duración")
                Text("${weeks.toInt()} semanas", style = MaterialTheme.typography.titleMedium)
                Slider(value = weeks, onValueChange = { weeks = it }, valueRange = 4f..24f, steps = 19)
            }
            item {
                Label("Objetivo")
                KoreCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    Text("10 km en 38:00", style = MaterialTheme.typography.titleMedium)
                    Text("80–86 km/semana · 6 días", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Label("Prioridad del bloque")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = true, onClick = {}, label = { Text("Equilibrado") })
                    FilterChip(selected = false, onClick = {}, label = { Text("Velocidad") })
                    FilterChip(selected = false, onClick = {}, label = { Text("Aeróbico") })
                }
            }
            item {
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Previsualizar plan")
                }
                Text(
                    "Prototipo visual: todavía no se generará ni guardará ningún bloque.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
