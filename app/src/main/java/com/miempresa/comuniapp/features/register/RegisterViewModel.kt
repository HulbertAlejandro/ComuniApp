package com.miempresa.comuniapp.features.register

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.resources.ResourceProvider
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ValidatedField
import com.miempresa.comuniapp.domain.model.*
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de la pantalla de registro de nuevos usuarios.
 *
 * Responsabilidades:
 * - Validar cada campo del formulario mediante [ValidatedField].
 * - Verificar si el email ya está registrado antes de crear el usuario.
 * - Construir el objeto [User] y delegarlo al repositorio.
 * - Emitir estados [RequestResult] para que la UI reaccione.
 *
 * @param repository Repositorio de usuarios que persiste en Firestore.
 * @param resources  Proveedor de strings para mensajes localizados.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: UserRepository,
    private val resources: ResourceProvider
) : ViewModel() {

    /** Campo nombre: no puede estar vacío. */
    val name = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.error_name_empty) else null
    }

    /** Campo email: formato válido obligatorio. */
    val email = ValidatedField("") {
        when {
            it.isBlank() -> resources.getString(R.string.error_email_empty)
            !Patterns.EMAIL_ADDRESS.matcher(it).matches() ->
                resources.getString(R.string.error_email_invalid)
            else -> null
        }
    }

    /** Campo teléfono: no puede estar vacío. */
    val phone = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.error_phone_empty) else null
    }

    /** Campo contraseña: mínimo 6 caracteres. */
    val password = ValidatedField("") {
        when {
            it.isBlank() -> resources.getString(R.string.error_password_required)
            it.length < 6 -> resources.getString(R.string.error_password_min_length)
            else -> null
        }
    }

    /** Campo confirmar contraseña: debe coincidir con [password]. */
    val confirmPassword = ValidatedField("") {
        when {
            it.isBlank() -> resources.getString(R.string.error_confirm_password_empty)
            it != password.value ->
                resources.getString(R.string.error_confirm_password_mismatch_message)
            else -> null
        }
    }

    /** Campo dirección: opcional, sin validación obligatoria. */
    val direccion = ValidatedField("") { null }

    /** Categorías de interés seleccionadas por el usuario. */
    private val _selectedCategories = MutableStateFlow<Set<Category>>(emptySet())
    val selectedCategories: StateFlow<Set<Category>> = _selectedCategories.asStateFlow()

    /**
     * Agrega o quita una categoría del conjunto de seleccionadas.
     * Si ya está presente la elimina; si no, la agrega.
     */
    fun toggleCategory(category: Category) {
        _selectedCategories.update { current ->
            if (current.contains(category)) current - category else current + category
        }
    }

    /** Retorna true solo si todos los campos obligatorios son válidos. */
    val isFormValid: Boolean
        get() = name.isValid &&
                email.isValid &&
                phone.isValid &&
                password.isValid &&
                confirmPassword.isValid

    /** Estado del proceso de registro expuesto a la UI. */
    private val _registerResult = MutableStateFlow<RequestResult?>(null)
    val registerResult: StateFlow<RequestResult?> = _registerResult.asStateFlow()

    /**
     * Ejecuta el flujo de registro del nuevo usuario.
     *
     * Pasos:
     * 1. Valida el formulario; si no es válido, no continúa.
     * 2. Emite [RequestResult.Loading] para activar el indicador visual.
     * 3. Verifica que el email no esté ya registrado en Firestore.
     * 4. Construye el [User] sin ID (Firestore lo asigna en el repositorio).
     * 5. Llama a [UserRepository.saveWithPassword] y emite [RequestResult.Success].
     * 6. Cualquier excepción emite [RequestResult.Failure].
     *
     * @param photo URI o URL de la foto de perfil seleccionada; usa un avatar
     *              genérico si se deja en blanco.
     */
    fun register(photo: String) {
        if (!isFormValid) return

        viewModelScope.launch {
            _registerResult.value = RequestResult.Loading

            try {
                // Verifica duplicados antes de crear el documento
                val existe = repository.findByEmail(email.value)
                if (existe != null) {
                    _registerResult.value = RequestResult.Failure(
                        resources.getString(R.string.error_email_already_exists)
                    )
                    return@launch
                }

                // El id se deja vacío; el repositorio lo asigna al hacer .document()
                val user = User(
                    id = "",
                    name = name.value.trim(),
                    email = email.value.trim(),
                    phoneNumber = phone.value.trim(),
                    profilePictureUrl = photo.ifBlank { "https://i.pravatar.cc/300" },
                    direction = direccion.value.trim(),
                    role = UserRole.USER,
                    reputation = Reputation(
                        points = 0,
                        level = UserLevel.ESPECTADOR,
                        badges = emptyList()
                    ),
                    favoriteCategories = _selectedCategories.value.toList()
                )

                repository.saveWithPassword(user, password.value)

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

    /** Reinicia el estado del resultado sin limpiar el formulario. */
    fun resetRegisterResult() { _registerResult.value = null }

    /** Limpia todos los campos del formulario y las categorías seleccionadas. */
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