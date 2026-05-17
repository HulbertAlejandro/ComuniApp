package com.miempresa.comuniapp.features.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.resources.ResourceProvider
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ValidatedField
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de la pantalla de restablecimiento de contraseña.
 *
 * Recibe el email verificado desde la pantalla anterior a través de
 * la propiedad [email], que debe asignarse antes de llamar a [resetPassword].
 *
 * Estrategia de comunicación entre ViewModels:
 * En Fase 2 se recomienda usar un ViewModel compartido en el NavGraph
 * de contraseñas, o pasar el email como argumento de navegación.
 * Por ahora se asigna directamente tras obtenerlo de [ForgetPasswordViewModel].
 *
 * @param userRepository Repositorio que actualiza contraseñas en Firestore.
 * @param resources      Proveedor de strings localizados.
 */
@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val resources: ResourceProvider
) : ViewModel() {

    /**
     * Email del usuario verificado en la pantalla anterior.
     * Debe asignarse desde la Screen antes de invocar [resetPassword].
     *
     * Ejemplo desde la Screen:
     * ```
     * LaunchedEffect(Unit) {
     *     resetViewModel.email = forgetViewModel.email.value
     * }
     * ```
     */
    var email: String = ""

    /** Nueva contraseña: mínimo 6 caracteres. */
    val newPassword = ValidatedField("") {
        when {
            it.isBlank() -> resources.getString(R.string.error_password_required)
            it.length < 6 -> resources.getString(R.string.error_password_min_length)
            else -> null
        }
    }

    /** Confirmación: debe coincidir exactamente con [newPassword]. */
    val confirmPassword = ValidatedField("") {
        when {
            it.isBlank() -> resources.getString(R.string.error_confirm_password_empty)
            it != newPassword.value ->
                resources.getString(R.string.error_confirm_password_mismatch_message)
            else -> null
        }
    }

    /** Retorna true solo si ambos campos de contraseña son válidos. */
    val isFormValid: Boolean
        get() = newPassword.isValid && confirmPassword.isValid

    /** Estado del proceso de restablecimiento expuesto a la UI. */
    private val _result = MutableStateFlow<RequestResult?>(null)
    val result: StateFlow<RequestResult?> = _result.asStateFlow()

    /**
     * Actualiza la contraseña del usuario en Firestore.
     *
     * Flujo:
     * 1. Valida el formulario y que el email no esté vacío.
     * 2. Emite [RequestResult.Loading].
     * 3. Verifica que el usuario siga existiendo en Firestore.
     * 4. Llama a [UserRepository.updatePassword] con el nuevo valor.
     * 5. Emite [RequestResult.Success] para que la UI navegue al login.
     * 6. Cualquier excepción emite [RequestResult.Failure].
     */
    fun resetPassword() {
        if (!isFormValid || email.isBlank()) {
            _result.value = RequestResult.Failure(
                resources.getString(R.string.error_generic)
            )
            return
        }

        viewModelScope.launch {
            _result.value = RequestResult.Loading

            try {
                // Doble verificación: el usuario debe seguir existiendo
                val user = userRepository.findByEmail(email)
                if (user == null) {
                    _result.value = RequestResult.Failure(
                        resources.getString(R.string.error_user_not_found)
                    )
                    return@launch
                }

                // Actualiza la contraseña en Firestore vía el repositorio
                userRepository.updatePassword(email, newPassword.value)

                _result.value = RequestResult.Success(
                    resources.getString(R.string.password_reset_success)
                )

            } catch (e: Exception) {
                _result.value = RequestResult.Failure(
                    e.message ?: resources.getString(R.string.password_reset_failure)
                )
            }
        }
    }

    /** Reinicia el estado del resultado. */
    fun resetResult() {
        _result.value = null
    }
}