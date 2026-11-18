package com.kore.android.session

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kore.session.SessionDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data object Empty : SessionUiState
    data class Ready(val session: SessionDocument, val message: String? = null) : SessionUiState
    data class Error(val message: String, val session: SessionDocument? = null) : SessionUiState
}

class SessionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AndroidSessionRepository(application)
    private val mutableState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)

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

    private fun currentSession(): SessionDocument? = when (val state = mutableState.value) {
        is SessionUiState.Ready -> state.session
        is SessionUiState.Error -> state.session
        else -> null
    }
}
