package com.miempresa.comuniapp.features.register

import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.resources.ResourceProvider
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ValidatedField
import com.miempresa.comuniapp.domain.model.*
import com.miempresa.comuniapp.domain.repository.StorageRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: UserRepository,
    private val storageRepository: StorageRepository,  // ← nueva dependencia
    private val resources: ResourceProvider
) : ViewModel() {

    val name = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.error_name_empty) else null
    }
    val email = ValidatedField("") {
        when {
            it.isBlank() -> resources.getString(R.string.error_email_empty)
            !Patterns.EMAIL_ADDRESS.matcher(it).matches() ->
                resources.getString(R.string.error_email_invalid)
            else -> null
        }
    }
    val phone = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.error_phone_empty) else null
    }
    val password = ValidatedField("") {
        when {
            it.isBlank() -> resources.getString(R.string.error_password_required)
            it.length < 6 -> resources.getString(R.string.error_password_min_length)
            else -> null
        }
    }
    val confirmPassword = ValidatedField("") {
        when {
            it.isBlank() -> resources.getString(R.string.error_confirm_password_empty)
            it != password.value ->
                resources.getString(R.string.error_confirm_password_mismatch_message)
            else -> null
        }
    }
    val direccion = ValidatedField("") { null }

    private val _selectedCategories = MutableStateFlow<Set<Category>>(emptySet())
    val selectedCategories: StateFlow<Set<Category>> = _selectedCategories.asStateFlow()

    fun toggleCategory(category: Category) {
        _selectedCategories.update { current ->
            if (current.contains(category)) current - category else current + category
        }
    }

    val isFormValid: Boolean
        get() = name.isValid && email.isValid && phone.isValid &&
                password.isValid && confirmPassword.isValid

    private val _registerResult = MutableStateFlow<RequestResult?>(null)
    val registerResult: StateFlow<RequestResult?> = _registerResult.asStateFlow()

    /**
     * Registra al usuario subiendo primero la foto si es una URI local,
     * luego persiste el perfil completo en Firestore con la URL de Storage.
     *
     * @param photo String con la URI local seleccionada por el usuario,
     *              o vacío si no seleccionó ninguna.
     */
    fun register(photo: String) {
        if (!isFormValid) return

        viewModelScope.launch {
            _registerResult.value = RequestResult.Loading

            try {
                // ── PASO 1: subir foto si el usuario seleccionó una ──────
                // Usamos el email como nombre de archivo para que sea único
                // y predecible. Tras el registro, el UID sería mejor clave;
                // aquí lo hacemos antes de tener el UID, así que usamos email.
                val profilePictureUrl = resolveProfilePictureUrl(
                    localUriString = photo,
                    email          = email.value.trim()
                )

                // ── PASO 2: construir el modelo y guardar en Firestore ────
                val user = User(
                    id                 = "",
                    name               = name.value.trim(),
                    email              = email.value.trim(),
                    phoneNumber        = phone.value.trim(),
                    profilePictureUrl  = profilePictureUrl,
                    direction          = direccion.value.trim(),
                    role               = UserRole.USER,
                    reputation         = Reputation(
                        points = 0,
                        level  = UserLevel.ESPECTADOR,
                        badges = emptyList()
                    ),
                    favoriteCategories = _selectedCategories.value.toList()
                )

                repository.save(user, password.value)

                _registerResult.value = RequestResult.Success(
                    resources.getString(R.string.register_success)
                )

            } catch (e: Exception) {
                _registerResult.value = RequestResult.Failure(
                    e.message ?: resources.getString(R.string.error_generic)
                )
            }
        }
    }

    /**
     * Determina la URL final de la foto de perfil.
     *
     * - Si [localUriString] está en blanco → usa avatar genérico.
     * - Si es una URI local (content:// o file://) → sube a Storage y
     *   retorna la downloadUrl.
     * - Si ya es una URL HTTPS → es una selección previa, la reutiliza.
     */
    private suspend fun resolveProfilePictureUrl(
        localUriString: String,
        email: String
    ): String {
        if (localUriString.isBlank()) return "https://i.pravatar.cc/300"

        // Las URLs de Storage y avatares empiezan con "https"
        if (localUriString.startsWith("https")) return localUriString

        val localUri  = Uri.parse(localUriString)
        // Sanitizamos el email para usarlo como nombre de archivo seguro
        val safeEmail = email.replace(Regex("[^a-zA-Z0-9]"), "_")
        val path      = "profile_pictures/$safeEmail.jpg"

        return storageRepository.uploadImage(localUri, path)
    }

    fun resetRegisterResult() { _registerResult.value = null }

    fun resetForm() {
        name.reset()
        email.reset()
        phone.reset()
        password.reset()
        confirmPassword.reset()
        direccion.reset()
        _selectedCategories.value = emptySet()
    }
}