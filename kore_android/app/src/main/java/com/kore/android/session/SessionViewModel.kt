package com.kore.android.session

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kore.session.SessionDocument
import com.kore.session.RouteInput
import com.kore.session.ActivityInput
import com.kore.session.MuseumEntryInput
import com.kore.session.ShoeInput
import com.kore.session.addShoe
import com.kore.session.addRoute
import com.kore.session.addActivity
import com.kore.session.addMuseumEntry
import com.kore.session.retireShoe
import com.kore.session.updateShoe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data object Empty : SessionUiState
    data class Ready(val session: SessionDocument, val message: String? = null) : SessionUiState
    data class Error(val message: String, val session: SessionDocument? = null) : SessionUiState
}

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AndroidSessionRepository(application)
    private val mutableState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    private val mutationMutex = Mutex()

    val state: StateFlow<SessionUiState> = mutableState.asStateFlow()

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
