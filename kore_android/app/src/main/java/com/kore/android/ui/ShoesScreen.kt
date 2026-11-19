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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.kore.session.RunnerShoe
import com.kore.session.ShoeInput
import com.kore.session.ShoeRatings
import com.kore.session.ShoeUsagePhase
import java.util.Locale

private val shoeColors = listOf(Color(0xFF5A78C7), Color(0xFFE15B3D), Color(0xFF3F8B67), Color(0xFF9B6AB4))

@Composable
fun ShoesScreen(
    padding: PaddingValues,
    shoes: List<RunnerShoe>?,
    onAddShoe: (ShoeInput) -> Unit,
    onUpdateShoe: (String, ShoeInput) -> Unit,
    onRetireShoe: (String) -> Unit,
) {
    val demo = shoes == null
    val active = shoes.orEmpty().filter { it.status != "retired" }
    val retired = shoes.orEmpty().filter { it.status == "retired" }
    var showCreate by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<RunnerShoe?>(null) }
    var editing by remember { mutableStateOf<RunnerShoe?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (demo) DemoBadge()
                    Text("Zapatillas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Cada kilómetro y cada etapa de su vida.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { showCreate = true }, enabled = !demo) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Alta")
                }
            }
        }

        if (demo) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShoeSummary("3", "activas", Modifier.weight(1f))
                    ShoeSummary("1.221", "km en rotación", Modifier.weight(1f))
                    ShoeSummary("4", "retiradas", Modifier.weight(1f))
                }
            }
            item { SectionTitle("Rotación actual", "Por kilometraje") }
            items(MockData.shoes.size) { index -> DemoShoeCard(MockData.shoes[index], index) }
            item {
                KoreCard(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) {
                    Text("Crea tu archivo para gestionar zapatillas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("La demostración no modifica datos personales.")
                }
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShoeSummary(active.size.toString(), "activas", Modifier.weight(1f))
                    ShoeSummary(formatKm(active.sumOf { it.currentKm }), "km en rotación", Modifier.weight(1f))
                    ShoeSummary(retired.size.toString(), "retiradas", Modifier.weight(1f))
                }
            }
            if (active.isEmpty() && retired.isEmpty()) {
                item {
                    KoreCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Icon(Icons.AutoMirrored.Rounded.DirectionsRun, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Text("Añade tu primera zapatilla", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Guarda kilometraje, límite, precio, valoraciones y cómo cambia su uso con el tiempo.")
                        Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) { Text("Dar de alta") }
                    }
                }
            }
            if (active.isNotEmpty()) {
                item { SectionTitle("Rotación actual", "${active.size} activas") }
                items(active.size) { index -> RealShoeCard(active[index], index) { selected = active[index] } }
            }
            if (retired.isNotEmpty()) {
                item { SectionTitle("Histórico", "${retired.size} retiradas") }
                items(retired.size) { index -> RetiredShoeCard(retired[index]) { selected = retired[index] } }
            }
        }
    }

    if (showCreate) {
        ShoeForm(null, { showCreate = false }) {
            onAddShoe(it)
            showCreate = false
        }
    }
    editing?.let { shoe ->
        ShoeForm(shoe, { editing = null }) {
            onUpdateShoe(shoe.id, it)
            editing = null
        }
    }
    selected?.let { shoe ->
        ShoeDetailSheet(
            shoe = shoe,
            onDismiss = { selected = null },
            onEdit = { selected = null; editing = shoe },
            onRetire = { onRetireShoe(shoe.id); selected = null },
        )
    }
}

@Composable
private fun ShoeSummary(value: String, label: String, modifier: Modifier) {
    KoreCard(modifier = modifier, containerColor = MaterialTheme.colorScheme.surfaceVariant) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DemoShoeCard(shoe: Shoe, index: Int) {
    ShoeCard(shoe.name, shoe.type, shoe.currentKm.toDouble(), shoe.limitKm.toDouble(), shoe.acquired, shoe.price, shoe.use, shoe.rating, shoeColors[index % shoeColors.size], shoe.status) {}
}

@Composable
private fun RealShoeCard(shoe: RunnerShoe, index: Int, onClick: () -> Unit) {
    ShoeCard(
        shoe.name,
        shoe.type.replaceFirstChar { it.uppercase() },
        shoe.currentKm,
        shoe.limitKm,
        shoe.acquiredAt,
        formatPrice(shoe.purchasePrice, shoe.currency),
        currentPurpose(shoe),
        shoe.ratings.average,
        shoeColors[index % shoeColors.size],
        "ACTIVA",
        onClick,
    )
}

@Composable
private fun ShoeCard(
    name: String,
    type: String,
    currentKm: Double,
    limitKm: Double,
    acquiredAt: String,
    price: String,
    purpose: String,
    rating: Double,
    color: Color,
    status: String,
    onClick: () -> Unit,
) {
    KoreCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(14.dp).background(color, CircleShape))
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(status, style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E9D63))
                Text("★ ${formatRating(rating)}", fontWeight = FontWeight.Bold)
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatKm(currentKm), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(" / ${formatKm(limitKm)} km", modifier = Modifier.padding(bottom = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { (currentKm.toFloat() / limitKm.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            color = if (currentKm > limitKm * .9) Color(0xFFE15B3D) else color,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            SmallFact("Alta", acquiredAt)
            SmallFact("Precio", price)
        }
        Text(purpose, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RetiredShoeCard(shoe: RunnerShoe, onClick: () -> Unit) {
    KoreCard(modifier = Modifier.clickable(onClick = onClick), containerColor = MaterialTheme.colorScheme.surfaceVariant) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Archive, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(shoe.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Retirada con ${formatKm(shoe.currentKm)} km · alta ${shoe.acquiredAt}", style = MaterialTheme.typography.bodySmall)
            }
            Text(formatRating(shoe.ratings.average), fontWeight = FontWeight.Bold)
            Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoeDetailSheet(shoe: RunnerShoe, onDismiss: () -> Unit, onEdit: () -> Unit, onRetire: () -> Unit) {
    var confirmRetire by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Rounded.DirectionsRun, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(shoe.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text("${formatKm(shoe.currentKm)} km · ${formatPrice(shoe.purchasePrice, shoe.currency)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.Edit, contentDescription = "Editar", modifier = Modifier.clickable(onClick = onEdit))
                }
            }
            item {
                Label("Fases de uso")
                shoe.usagePhases.forEach { phase ->
                    val endKm = phase.toKm
                    val range = if (endKm == null) "${formatKm(phase.fromKm)} km →" else "${formatKm(phase.fromKm)}–${formatKm(endKm)} km"
                    UsagePhase(range, phase.purpose, shoe.currentKm >= phase.fromKm)
                }
            }
            item {
                Label("Valoración · ${formatRating(shoe.ratings.average)}")
                RatingRow("Comodidad", shoe.ratings.comfort.toFloat())
                RatingRow("Respuesta", shoe.ratings.response.toFloat())
                RatingRow("Estabilidad", shoe.ratings.stability.toFloat())
                RatingRow("Agarre", shoe.ratings.grip.toFloat())
                RatingRow("Durabilidad", shoe.ratings.durability.toFloat())
            }
            shoe.notes?.let { notes -> item { Label("Notas"); Text(notes) } }
            item {
                val cost = if (shoe.currentKm > 0) shoe.purchasePrice / shoe.currentKm * 10 else null
                Text(cost?.let { "Coste actual: ${formatPrice(it, shoe.currency)} por 10 km" } ?: "Coste por kilómetro disponible tras el primer uso", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (shoe.status != "retired") {
                item { OutlinedButton(onClick = { confirmRetire = true }, modifier = Modifier.fillMaxWidth()) { Text("Mover al histórico") } }
            }
        }
    }
    if (confirmRetire) {
        AlertDialog(
            onDismissRequest = { confirmRetire = false },
            title = { Text("¿Retirar ${shoe.name}?") },
            text = { Text("Conservarás su ficha completa en el histórico.") },
            confirmButton = { TextButton(onClick = onRetire) { Text("Retirar") } },
            dismissButton = { TextButton(onClick = { confirmRetire = false }) { Text("Cancelar") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoeForm(shoe: RunnerShoe?, onDismiss: () -> Unit, onSave: (ShoeInput) -> Unit) {
    var name by remember { mutableStateOf(shoe?.name.orEmpty()) }
    var type by remember { mutableStateOf(shoe?.type ?: "diaria") }
    var acquiredAt by remember { mutableStateOf(shoe?.acquiredAt.orEmpty()) }
    var currentKm by remember { mutableStateOf(shoe?.currentKm?.let(::plainNumber).orEmpty()) }
    var limitKm by remember { mutableStateOf(shoe?.limitKm?.let(::plainNumber) ?: "700") }
    var price by remember { mutableStateOf(shoe?.purchasePrice?.let(::plainNumber).orEmpty()) }
    var boundary by remember { mutableStateOf(shoe?.usagePhases?.firstOrNull()?.toKm?.let(::plainNumber) ?: "200") }
    var initialUse by remember { mutableStateOf(shoe?.usagePhases?.firstOrNull()?.purpose.orEmpty()) }
    var laterUse by remember { mutableStateOf(shoe?.usagePhases?.getOrNull(1)?.purpose.orEmpty()) }
    var notes by remember { mutableStateOf(shoe?.notes.orEmpty()) }
    var comfort by remember { mutableStateOf((shoe?.ratings?.comfort ?: 3.0).toFloat()) }
    var response by remember { mutableStateOf((shoe?.ratings?.response ?: 3.0).toFloat()) }
    var stability by remember { mutableStateOf((shoe?.ratings?.stability ?: 3.0).toFloat()) }
    var grip by remember { mutableStateOf((shoe?.ratings?.grip ?: 3.0).toFloat()) }
    var durability by remember { mutableStateOf((shoe?.ratings?.durability ?: 3.0).toFloat()) }
    val currentValue = currentKm.decimalOrNull()
    val limitValue = limitKm.decimalOrNull()
    val priceValue = price.decimalOrNull()
    val boundaryValue = boundary.decimalOrNull()
    val valid = name.isNotBlank() && acquiredAt.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) &&
        currentValue != null && currentValue >= 0 && limitValue != null && limitValue > 0 &&
        priceValue != null && priceValue >= 0 && boundaryValue != null && boundaryValue > 0 &&
        initialUse.isNotBlank() && laterUse.isNotBlank()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(if (shoe == null) "Nueva zapatilla" else "Editar zapatilla", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("Ficha local y portable", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { OutlinedTextField(name, { name = it }, label = { Text("Marca y modelo") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item {
                Text("Tipo", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("diaria", "mixta", "competición").forEach { option ->
                        FilterChip(selected = type == option, onClick = { type = option }, label = { Text(option.replaceFirstChar { it.uppercase() }) })
                    }
                }
            }
            item { OutlinedTextField(acquiredAt, { acquiredAt = it }, label = { Text("Fecha de alta · AAAA-MM-DD") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(currentKm, { currentKm = it }, label = { Text("Km actuales") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(limitKm, { limitKm = it }, label = { Text("Límite km") }, modifier = Modifier.weight(1f), singleLine = true)
                }
            }
            item { OutlinedTextField(price, { price = it }, label = { Text("Precio de compra €") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item {
                Label("Fases de uso")
                OutlinedTextField(initialUse, { initialUse = it }, label = { Text("Uso inicial") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(boundary, { boundary = it }, label = { Text("Cambiar uso al llegar al km") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(laterUse, { laterUse = it }, label = { Text("Uso posterior") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                Label("Valoración")
                RatingSlider("Comodidad", comfort) { comfort = it }
                RatingSlider("Respuesta", response) { response = it }
                RatingSlider("Estabilidad", stability) { stability = it }
                RatingSlider("Agarre", grip) { grip = it }
                RatingSlider("Durabilidad", durability) { durability = it }
            }
            item { OutlinedTextField(notes, { notes = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
            item {
                Button(
                    onClick = {
                        onSave(ShoeInput(
                            name = name,
                            type = type,
                            acquiredAt = acquiredAt,
                            currentKm = currentValue!!,
                            limitKm = limitValue!!,
                            purchasePrice = priceValue!!,
                            usagePhases = listOf(
                                ShoeUsagePhase(0.0, boundaryValue, initialUse),
                                ShoeUsagePhase(boundaryValue!!, null, laterUse),
                            ),
                            ratings = ShoeRatings(comfort.toDouble(), response.toDouble(), stability.toDouble(), grip.toDouble(), durability.toDouble()),
                            notes = notes.ifBlank { null },
                        ))
                    },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (shoe == null) "Guardar zapatilla" else "Guardar cambios") }
            }
        }
    }
}

@Composable
private fun RatingSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall)
        Slider(value = value, onValueChange = onChange, valueRange = 1f..5f, steps = 7, modifier = Modifier.weight(1f))
        Text(formatRating(value.toDouble()), modifier = Modifier.width(32.dp), fontWeight = FontWeight.Bold)
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
        Text(formatRating(value.toDouble()), modifier = Modifier.width(38.dp), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallFact(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

private fun currentPurpose(shoe: RunnerShoe): String = shoe.usagePhases
    .filter { phase ->
        val endKm = phase.toKm
        shoe.currentKm >= phase.fromKm && (endKm == null || shoe.currentKm < endKm)
    }
    .maxByOrNull { it.fromKm }
    ?.purpose
    ?: shoe.usagePhases.lastOrNull()?.purpose
    ?: "Sin uso definido"

private fun String.decimalOrNull(): Double? = replace(',', '.').toDoubleOrNull()
private fun plainNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
private fun formatKm(value: Double): String = String.format(Locale.getDefault(), if (value % 1.0 == 0.0) "%.0f" else "%.1f", value)
private fun formatRating(value: Double): String = String.format(Locale.getDefault(), "%.1f", value)
private fun formatPrice(value: Double, currency: String): String = String.format(Locale.getDefault(), "%.2f %s", value, if (currency == "EUR") "€" else currency)
