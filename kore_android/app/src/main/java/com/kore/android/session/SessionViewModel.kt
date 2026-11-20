package com.kore.android.session

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kore.session.SessionDocument
import com.kore.session.RouteInput
import com.kore.session.ActivityInput
import com.kore.session.MuseumEntryInput
import com.kore.session.PersonalBestInput
import com.kore.session.ShoeInput
import com.kore.session.addShoe
import com.kore.session.addRoute
import com.kore.session.addActivity
import com.kore.session.addMuseumEntry
import com.kore.session.addPersonalBest
import com.kore.session.retireShoe
import com.kore.session.updateShoe
import com.kore.session.SuuntoWorkoutAdapter
import com.kore.session.importActivities
import com.kore.android.suunto.SecureSuuntoStore
import com.kore.android.suunto.SuuntoApiClient
import com.kore.android.suunto.SuuntoCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data object Empty : SessionUiState
    data class Ready(val session: SessionDocument, val message: String? = null) : SessionUiState
    data class Error(val message: String, val session: SessionDocument? = null) : SessionUiState
}

data class SuuntoUiState(
    val configured: Boolean,
    val syncing: Boolean = false,
    val message: String? = null,
    val lastWorkoutCount: Int? = null,
)

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AndroidSessionRepository(application)
    private val mutableState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    private val mutationMutex = Mutex()
    private val suuntoStore = SecureSuuntoStore(application)
    private val suuntoClient = SuuntoApiClient(suuntoStore)
    private val mutableSuuntoState = MutableStateFlow(SuuntoUiState(suuntoStore.isConfigured()))

    val state: StateFlow<SessionUiState> = mutableState.asStateFlow()
    val suuntoState: StateFlow<SuuntoUiState> = mutableSuuntoState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableState.value = SessionUiState.Loading
            mutableState.value = runCatching { repository.loadActive() }
                .fold(
                    onSuccess = { session ->
                        session?.let { SessionUiState.Ready(it) } ?: SessionUiState.Empty
                    },
                    onFailure = { error ->
                        SessionUiState.Error(error.message ?: "No se pudo abrir la sesión")
                    },
                )
        }
    }

    fun create(label: String) {
        viewModelScope.launch {
            mutableState.value = SessionUiState.Loading
            mutableState.value = runCatching { repository.create(label) }
                .fold(
                    onSuccess = { SessionUiState.Ready(it, "Sesión creada") },
                    onFailure = { error ->
                        SessionUiState.Error(error.message ?: "No se pudo crear la sesión")
                    },
                )
        }
    }

    fun importSession(uri: Uri) {
        viewModelScope.launch {
            val retained = currentSession()
            mutableState.value = SessionUiState.Loading
            mutableState.value = runCatching { repository.importFrom(uri) }
                .fold(
                    onSuccess = { SessionUiState.Ready(it, "Sesión importada") },
                    onFailure = { error ->
                        SessionUiState.Error(
                            error.message ?: "No se pudo importar la sesión",
                            retained,
                        )
                    },
                )
        }
    }

    fun exportSession(uri: Uri) {
        val session = currentSession() ?: return
        viewModelScope.launch {
            mutableState.value = runCatching {
                repository.exportTo(uri, session)
                session
            }.fold(
                onSuccess = { SessionUiState.Ready(it, "Sesión exportada") },
                onFailure = { error ->
                    SessionUiState.Error(error.message ?: "No se pudo exportar la sesión", session)
                },
            )
        }
    }

    fun addRoute(input: RouteInput) {
        viewModelScope.launch {
            mutationMutex.withLock {
                val session = currentSession() ?: return@withLock
                try {
                    val updated = session.addRoute(input)
                    repository.save(updated)
                    mutableState.value = SessionUiState.Ready(updated, "Ruta guardada")
                } catch (error: Exception) {
                    mutableState.value = SessionUiState.Error(
                        error.message ?: "No se pudo guardar la ruta",
                        session,
                    )
                }
            }
        }
    }

    fun addShoe(input: ShoeInput) = mutateSession("Zapatilla guardada") { addShoe(input) }

    fun updateShoe(id: String, input: ShoeInput) =
        mutateSession("Zapatilla actualizada") { updateShoe(id, input) }

    fun retireShoe(id: String) = mutateSession("Zapatilla movida al histórico") { retireShoe(id) }

    fun addActivity(input: ActivityInput) =
        mutateSession("Actividad guardada") { addActivity(input) }

    fun addMuseumEntry(input: MuseumEntryInput) =
        mutateSession("Sesión incorporada al Museo") { addMuseumEntry(input) }

    fun addPersonalBest(input: PersonalBestInput) =
        mutateSession("Marca personal guardada") { addPersonalBest(input) }

    fun saveSuuntoCredentials(credentials: SuuntoCredentials) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { suuntoStore.save(credentials) } }
                .onSuccess { mutableSuuntoState.value = SuuntoUiState(true, message = "Credenciales cifradas en el dispositivo") }
                .onFailure { error ->
                    mutableSuuntoState.value = SuuntoUiState(
                        configured = suuntoStore.isConfigured(),
                        message = error.message ?: "No se pudo configurar Suunto",
                    )
                }
        }
    }

    fun disconnectSuunto() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { suuntoStore.clear() }
            mutableSuuntoState.value = SuuntoUiState(false, message = "Credenciales de Suunto eliminadas")
        }
    }

    fun synchronizeSuunto(days: Int = 30) {
        val session = currentSession() ?: return
        if (mutableSuuntoState.value.syncing) return
        viewModelScope.launch {
            mutableSuuntoState.value = mutableSuuntoState.value.copy(syncing = true, message = "Sincronizando…")
            runCatching {
                val result = withContext(Dispatchers.IO) {
                    suuntoClient.synchronize(suuntoStore.load(), days)
                }
                val imports = result.pages.flatMap { page -> SuuntoWorkoutAdapter.parsePage(page) }
                val updated = mutationMutex.withLock {
                    val latest = currentSession() ?: session
                    latest.importActivities(imports).also { repository.save(it) }
                }
                updated to result.workoutCount
            }.onSuccess { (updated, count) ->
                mutableState.value = SessionUiState.Ready(updated, "Suunto sincronizado")
                mutableSuuntoState.value = SuuntoUiState(
                    configured = true,
                    message = "$count entrenamientos procesados",
                    lastWorkoutCount = count,
                )
            }.onFailure { error ->
                mutableSuuntoState.value = SuuntoUiState(
                    configured = suuntoStore.isConfigured(),
                    message = error.message ?: "No se pudo sincronizar Suunto",
                )
            }
        }
    }

    private fun mutateSession(message: String, mutation: SessionDocument.() -> SessionDocument) {
        viewModelScope.launch {
            mutationMutex.withLock {
                val session = currentSession() ?: return@withLock
                try {
                    val updated = session.mutation()
                    repository.save(updated)
                    mutableState.value = SessionUiState.Ready(updated, message)
                } catch (error: Exception) {
                    mutableState.value = SessionUiState.Error(
                        error.message ?: "No se pudo actualizar la sesión",
                        session,
                    )
                }
            }
        }
    }

    private fun currentSession(): SessionDocument? = when (val state = mutableState.value) {
        is SessionUiState.Ready -> state.session
        is SessionUiState.Error -> state.session
        else -> null
    }
}
