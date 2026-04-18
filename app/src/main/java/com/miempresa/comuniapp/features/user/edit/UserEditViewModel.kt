package com.miempresa.comuniapp.features.user.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface UserEditUiEvent {
    data class ShowMessage(val message: String) : UserEditUiEvent
    data object NavigateBack : UserEditUiEvent
}

@HiltViewModel
class UserEditViewModel @Inject constructor(
    private val repository: UserRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    val user: StateFlow<User?> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                repository.users.map { list -> list.find { it.id == session.userId } }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _selectedCategories = MutableStateFlow<Set<Category>>(emptySet())
    val selectedCategories: StateFlow<Set<Category>> = _selectedCategories.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UserEditUiEvent>(replay = 0)
    val uiEvents: SharedFlow<UserEditUiEvent> = _uiEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            user.filterNotNull().first().let { u ->
                _selectedCategories.value = u.favoriteCategories.toSet()
            }
        }
    }

    fun toggleCategory(category: Category) {
        _selectedCategories.update { current ->
            if (current.contains(category)) current - category else current + category
        }
    }

    fun saveUser(name: String, phone: String, photo: String) {
        val current = user.value ?: return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.update(
                    current.copy(
                        name = name.trim(),
                        phoneNumber = phone.trim(),
                        profilePictureUrl = photo.trim().ifBlank { current.profilePictureUrl },
                        favoriteCategories = _selectedCategories.value.toList()
                    )
                )
                _uiEvents.emit(UserEditUiEvent.ShowMessage("Perfil actualizado con éxito"))
                _uiEvents.emit(UserEditUiEvent.NavigateBack)
            } catch (e: Exception) {
                _uiEvents.emit(UserEditUiEvent.ShowMessage("Error al guardar: ${e.message}"))
            } finally {
                _isSaving.value = false
            }
        }
    }

    // ✅ Nueva funcionalidad para eliminar cuenta
    fun deleteAccount() {
        val current = user.value ?: return
        viewModelScope.launch {
            try {
                repository.delete(current.id)
                sessionDataStore.clearSession() // Al limpiar sesión, el NavHost redirige al Login
                _uiEvents.emit(UserEditUiEvent.ShowMessage("Cuenta eliminada correctamente"))
            } catch (e: Exception) {
                _uiEvents.emit(UserEditUiEvent.ShowMessage("Error: ${e.message}"))
            }
        }
    }
}