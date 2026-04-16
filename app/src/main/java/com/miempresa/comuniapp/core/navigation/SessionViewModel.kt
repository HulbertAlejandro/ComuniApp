package com.miempresa.comuniapp.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.data.model.UserSession
import com.miempresa.comuniapp.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SessionState {
    data object Loading : SessionState
    data object NotAuthenticated : SessionState
    data class Authenticated(val session: UserSession) : SessionState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionDataStore: SessionDataStore
) : ViewModel() {


    val sessionState: StateFlow<SessionState> = sessionDataStore.sessionFlow
        .map { session: UserSession? ->
            if (session != null) {
                SessionState.Authenticated(session)
            } else {
                SessionState.NotAuthenticated
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SessionState.Loading
        )

    fun login(userId: String, name: String, role: UserRole) {
        viewModelScope.launch {
            sessionDataStore.saveSession(userId, name, role)
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionDataStore.clearSession()
        }
    }
}
