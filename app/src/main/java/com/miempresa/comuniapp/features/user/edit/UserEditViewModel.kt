package com.miempresa.comuniapp.features.user.edit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.resources.ResourceProvider
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.repository.StorageRepository  // ← nueva importación
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface UserEditUiEvent {
    data class ShowMessage(val message: String) : UserEditUiEvent
    data object NavigateBack : UserEditUiEvent
}

/**
 * ViewModel de la pantalla de edición de perfil de usuario.
 *
 * Con Storage integrado, [saveUser] ahora:
 * 1. Detecta si [photo] es una URI local nueva o una URL ya persistida.
 * 2. Sube la foto a "profile_pictures/{userId}.jpg" solo si es local.
 * 3. Guarda la URL definitiva en el documento de Firestore.
 *
 * Usar el [userId] como nombre de archivo garantiza que cada usuario
 * sobreescriba siempre el mismo archivo, sin acumular fotos huérfanas.
 */
@HiltViewModel
class UserEditViewModel @Inject constructor(
    private val repository: UserRepository,
    private val storageRepository: StorageRepository,   // ← nueva dependencia
    private val sessionDataStore: SessionDataStore,
    private val resources: ResourceProvider
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

    /**
     * Guarda los cambios del perfil del usuario.
     *
     * Flujo con Storage:
     * 1. Si [photo] está en blanco → conserva la URL actual de Firestore.
     * 2. Si [photo] es "https://..." → ya es una URL válida, no se sube de nuevo.
     * 3. Si [photo] es "content://" o "file://" → URI local nueva, se sube
     *    a "profile_pictures/{userId}.jpg" antes de actualizar Firestore.
     *
     * @param name      Nombre actualizado.
     * @param phone     Teléfono actualizado.
     * @param photo     String de URI local o URL existente de la foto de perfil.
     * @param direccion Dirección o barrio actualizado.
     */
    fun saveUser(name: String, phone: String, photo: String, direccion: String) {
        val current = user.value ?: return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                // ── PASO 1: resolver la URL final de la foto ─────────────
                val profilePictureUrl = resolveProfilePictureUrl(
                    localUriString   = photo,
                    currentUrl       = current.profilePictureUrl,
                    userId           = current.id
                )

                // ── PASO 2: actualizar el documento en Firestore ─────────
                repository.update(
                    current.copy(
                        name              = name.trim(),
                        phoneNumber       = phone.trim(),
                        profilePictureUrl = profilePictureUrl,
                        direction         = direccion.trim().ifBlank { current.direction },
                        favoriteCategories = _selectedCategories.value.toList()
                    )
                )

                _uiEvents.emit(
                    UserEditUiEvent.ShowMessage(
                        resources.getString(R.string.user_edit_profile_saved)
                    )
                )
                _uiEvents.emit(UserEditUiEvent.NavigateBack)

            } catch (e: Exception) {
                _uiEvents.emit(
                    UserEditUiEvent.ShowMessage(
                        "${resources.getString(R.string.user_edit_save_error)}: ${e.message}"
                    )
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Determina la URL final de la foto de perfil:
     *
     * - Blank       → conserva [currentUrl] (el usuario no cambió la foto).
     * - "https://…" → ya es una URL de Storage o avatar, se reutiliza.
     * - Otro        → es una URI local; se sube y se retorna la downloadUrl.
     *
     * Usar el [userId] como nombre de archivo tiene dos ventajas:
     * 1. Sobrescribe la foto anterior automáticamente (sin huérfanos).
     * 2. La ruta es determinista: "profile_pictures/{userId}.jpg".
     */
    private suspend fun resolveProfilePictureUrl(
        localUriString: String,
        currentUrl: String,
        userId: String
    ): String {
        if (localUriString.isBlank())             return currentUrl
        if (localUriString.startsWith("https"))   return localUriString

        val localUri = Uri.parse(localUriString)
        val path     = "profile_pictures/$userId.jpg"
        return storageRepository.uploadImage(localUri, path)
    }

    fun deleteAccount() {
        val current = user.value ?: return
        viewModelScope.launch {
            try {
                repository.delete(current.id)
                sessionDataStore.clearSession()
                _uiEvents.emit(
                    UserEditUiEvent.ShowMessage(
                        resources.getString(R.string.user_edit_account_deleted)
                    )
                )
            } catch (e: Exception) {
                _uiEvents.emit(
                    UserEditUiEvent.ShowMessage(
                        "${resources.getString(R.string.user_edit_delete_error)}: ${e.message}"
                    )
                )
            }
        }
    }
}