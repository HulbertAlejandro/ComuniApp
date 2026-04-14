package com.miempresa.comuniapp.features.user.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UserRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    // Observa el repositorio reactivamente: cualquier update() en UserEditViewModel
    // se refleja aquí automáticamente sin necesidad de recargar.
    val user: StateFlow<User?> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                repository.users.map { list -> list.find { it.id == session.userId } }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    fun logout() {
        viewModelScope.launch {
            sessionDataStore.clearSession()
        }
    }
}