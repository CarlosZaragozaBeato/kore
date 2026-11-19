package com.kore.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kore.android.session.SessionViewModel
import com.kore.android.ui.KoreApp
import com.kore.android.ui.KoreTheme

class MainActivity : ComponentActivity() {
    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by sessionViewModel.state.collectAsStateWithLifecycle()
            val importLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
            ) { uri -> uri?.let(sessionViewModel::importSession) }
            val exportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json"),
            ) { uri -> uri?.let(sessionViewModel::exportSession) }

            KoreTheme {
                KoreApp(
                    state = state,
                    onCreateSession = sessionViewModel::create,
                    onAddRoute = sessionViewModel::addRoute,
                    onAddShoe = sessionViewModel::addShoe,
                    onUpdateShoe = sessionViewModel::updateShoe,
                    onRetireShoe = sessionViewModel::retireShoe,
                    onImportSession = {
                        importLauncher.launch(arrayOf("application/json", "text/plain"))
                    },
                    onExportSession = { exportLauncher.launch("Kore.kore.json") },
                )
            }
        }
    }
}
