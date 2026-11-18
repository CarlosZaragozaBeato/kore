package com.kore.android.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.kore.android.session.SessionUiState

private enum class Destination(val label: String, val icon: ImageVector) {
    Home("Inicio", Icons.Rounded.Home),
    Plan("Plan", Icons.Rounded.CalendarMonth),
    Progress("Progreso", Icons.Rounded.Insights),
    History("Historial", Icons.Rounded.History),
    Settings("Ajustes", Icons.Rounded.Settings),
}

@Composable
fun KoreApp(
    state: SessionUiState,
    onImportSession: () -> Unit,
    onExportSession: () -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(Destination.Home) }
    val realSession = when (state) {
        is SessionUiState.Ready -> state.session
        is SessionUiState.Error -> state.session
        else -> null
    }
    val profileLabel = realSession?.profileLabel ?: "Carlos"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (destination) {
            Destination.Home -> HomeScreen(
                profileLabel = profileLabel,
                padding = padding,
                onOpenPlan = { destination = Destination.Plan },
            )
            Destination.Plan -> PlanScreen(padding)
            Destination.Progress -> ProgressScreen(padding)
            Destination.History -> HistoryScreen(padding)
            Destination.Settings -> SettingsScreen(
                padding = padding,
                hasRealSession = realSession != null,
                onImportSession = onImportSession,
                onExportSession = onExportSession,
            )
        }
    }
}
