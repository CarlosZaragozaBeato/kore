package com.kore.android.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Route
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kore.session.RouteInput
import com.kore.session.RunnerRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(
    padding: PaddingValues,
    routes: List<RunnerRoute>?,
    onAddRoute: (RouteInput) -> Unit,
) {
    val demo = routes == null
    var showForm by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (demo) DemoBadge()
                    Text("Rutas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Recorridos que forman parte de tu historia.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { showForm = true }, enabled = !demo) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Guardar")
                }
            }
        }
        if (demo) {
            items(MockData.routes.size) { index -> DemoRouteCard(MockData.routes[index], index) }
            item {
                KoreCard(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) {
                    Text("Crea tu archivo para guardar rutas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("El modo demostración no modifica datos reales.")
                }
            }
        } else if (routes.isNullOrEmpty()) {
            item {
                KoreCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    Icon(Icons.Rounded.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                    Text("Todavía no tienes rutas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Guarda manualmente tu primer recorrido. Más adelante podrás importar GPX.")
                    Button(onClick = { showForm = true }, modifier = Modifier.fillMaxWidth()) { Text("Crear primera ruta") }
                }
            }
        } else {
            items(routes.size) { index -> RealRouteCard(routes[index], index) }
        }
    }

    if (showForm) {
        RouteForm(
            onDismiss = { showForm = false },
            onSave = {
                onAddRoute(it)
                showForm = false
            },
        )
    }
}

@Composable
private fun DemoRouteCard(route: DemoRoute, index: Int) {
    RouteCard(
        name = route.name,
        location = route.location,
        distance = "${route.distanceKm} km",
        elevation = "+${route.elevationMeters} m",
        surface = route.surface,
        note = route.note,
        index = index,
    )
}

@Composable
private fun RealRouteCard(route: RunnerRoute, index: Int) {
    RouteCard(
        name = route.name,
        location = route.location,
        distance = String.format(java.util.Locale.getDefault(), "%.1f km", route.distanceKm),
        elevation = route.elevationGainMeters?.let { "+$it m" } ?: "Sin desnivel",
        surface = route.surface.replaceFirstChar { it.uppercase() },
        note = route.notes ?: "Ruta guardada manualmente",
        index = index,
    )
}

@Composable
private fun RouteCard(
    name: String,
    location: String,
    distance: String,
    elevation: String,
    surface: String,
    note: String,
    index: Int,
) {
    KoreCard {
        RouteSketch(index)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(location, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(distance, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(elevation, style = MaterialTheme.typography.labelLarge)
            Text(surface, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RouteSketch(index: Int) {
    val color = listOf(Color(0xFFE15B3D), Color(0xFF356D9A), Color(0xFF3F8B67))[index % 3]
    Canvas(Modifier.fillMaxWidth().height(86.dp)) {
        val path = Path().apply {
            moveTo(size.width * .05f, size.height * .72f)
            cubicTo(size.width * .2f, size.height * .1f, size.width * .36f, size.height * .95f, size.width * .5f, size.height * .42f)
            cubicTo(size.width * .68f, size.height * -.05f, size.width * .73f, size.height * .88f, size.width * .95f, size.height * .25f)
        }
        drawPath(path, color, style = Stroke(width = 8f, cap = StrokeCap.Round))
        drawCircle(color, 9f, Offset(size.width * .05f, size.height * .72f))
        drawCircle(color, 9f, Offset(size.width * .95f, size.height * .25f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteForm(onDismiss: () -> Unit, onSave: (RouteInput) -> Unit) {
    var name by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var surface by remember { mutableStateOf("asfalto") }
    var elevation by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val distanceValue = distance.replace(',', '.').toDoubleOrNull()
    val elevationValue = elevation.toIntOrNull()
    val valid = name.isNotBlank() && location.isNotBlank() && distanceValue != null && distanceValue > 0 && (elevation.isBlank() || elevationValue != null)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Guardar ruta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("Entrada manual · podrás añadir GPX más adelante", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item { OutlinedTextField(location, { location = it }, label = { Text("Ubicación") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(distance, { distance = it }, label = { Text("Distancia km") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(elevation, { elevation = it }, label = { Text("Desnivel m") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
            item {
                Text("Superficie", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("asfalto", "mixta", "trail").forEach { option ->
                        FilterChip(selected = surface == option, onClick = { surface = option }, label = { Text(option.replaceFirstChar { it.uppercase() }) })
                    }
                }
            }
            item { OutlinedTextField(notes, { notes = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
            item {
                Button(
                    onClick = {
                        onSave(RouteInput(name, distanceValue!!, location, surface, elevationValue, notes.ifBlank { null }))
                    },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Guardar en mi sesión") }
            }
        }
    }
}
