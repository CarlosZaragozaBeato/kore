package com.kore.android.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Museum
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Route
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
import com.kore.session.RouteInput
import com.kore.session.ActivityInput
import com.kore.session.MuseumEntryInput
import com.kore.session.ShoeInput
import com.kore.session.routes
import com.kore.session.activities
import com.kore.session.museumEntries
import com.kore.session.shoes

private enum class Destination(val label: String, val icon: ImageVector) {
    Home("Inicio", Icons.Rounded.Home),
    Museum("Museo", Icons.Rounded.Museum),
    Routes("Rutas", Icons.Rounded.Route),
    Records("Marcas", Icons.Rounded.EmojiEvents),
    Shoes("Zapatillas", Icons.AutoMirrored.Rounded.DirectionsRun),
    Settings("Ajustes", Icons.Rounded.Settings),
}

private val bottomDestinations = Destination.entries.filter { it != Destination.Settings }

@Composable
fun KoreApp(
    state: SessionUiState,
    onCreateSession: (String) -> Unit,
    onAddRoute: (RouteInput) -> Unit,
    onAddShoe: (ShoeInput) -> Unit,
    onUpdateShoe: (String, ShoeInput) -> Unit,
    onRetireShoe: (String) -> Unit,
    onAddActivity: (ActivityInput) -> Unit,
    onAddMuseumEntry: (MuseumEntryInput) -> Unit,
    onImportSession: () -> Unit,
    onExportSession: () -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(Destination.Home) }
    var demoMode by rememberSaveable { mutableStateOf(false) }

    if (state == SessionUiState.Loading) {
        LoadingScreen()
        return
    }
    if (state == SessionUiState.Empty && !demoMode) {
        WelcomeScreen(
            onCreate = onCreateSession,
            onImport = onImportSession,
            onExploreDemo = { demoMode = true },
        )
        return
    }
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
                bottomDestinations.forEach { item ->
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
                demo = realSession == null,
                routesCount = realSession?.inventory()?.routes ?: 0,
                shoesCount = realSession?.inventory()?.shoes ?: 0,
                activitiesCount = realSession?.inventory()?.activities ?: 0,
                museumCount = realSession?.inventory()?.museumEntries ?: 0,
                onOpenSettings = { destination = Destination.Settings },
            )
            Destination.Museum -> MuseumScreen(
                padding = padding,
                activities = realSession?.activities(),
                entries = realSession?.museumEntries(),
                shoes = realSession?.shoes().orEmpty(),
                onAddActivity = onAddActivity,
                onAddMuseumEntry = onAddMuseumEntry,
            )
            Destination.Routes -> RoutesScreen(
                padding = padding,
                routes = realSession?.routes(),
                onAddRoute = onAddRoute,
            )
            Destination.Records -> RecordsScreen(padding, realSession == null)
            Destination.Shoes -> ShoesScreen(
                padding = padding,
                shoes = realSession?.shoes(),
                onAddShoe = onAddShoe,
                onUpdateShoe = onUpdateShoe,
                onRetireShoe = onRetireShoe,
            )
            Destination.Settings -> SettingsScreen(
                padding = padding,
                hasRealSession = realSession != null,
                onImportSession = onImportSession,
                onExportSession = onExportSession,
                demo = realSession == null,
                onCreateArchive = {
                    demoMode = false
                    destination = Destination.Home
                },
            )
        }
    }
}
