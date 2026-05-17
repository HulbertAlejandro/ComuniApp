package com.miempresa.comuniapp.features.password

import android.util.Patterns
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
 * ViewModel de la pantalla "¿Olvidaste tu contraseña?".
 *
 * Verifica que el email ingresado exista en Firestore.
 * Si el usuario es encontrado, emite [RequestResult.Success] para que
 * la UI navegue hacia la pantalla de restablecimiento.
 *
 * Nota: el envío real de correo de recuperación se implementará en Fase 2
 * con Firebase Authentication. Por ahora, la verificación se hace
 * directamente contra la colección "users" en Firestore.
 *
 * @param userRepository Repositorio que consulta usuarios en Firestore.
 * @param resources      Proveedor de strings localizados.
 */
@HiltViewModel
class ForgetPasswordViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val resources: ResourceProvider
) : ViewModel() {

    /** Campo de email con validación de formato. */
    val email = ValidatedField("") {
        when {
            it.isBlank() -> resources.getString(R.string.error_email_empty)
            !Patterns.EMAIL_ADDRESS.matcher(it).matches() ->
                resources.getString(R.string.error_email_invalid)
            else -> null
        }
    }

    /** Retorna true solo si el campo email tiene un valor válido. */
    val isFormValid: Boolean
        get() = email.isValid

    /** Estado del proceso de verificación expuesto a la UI. */
    private val _result = MutableStateFlow<RequestResult?>(null)
    val result: StateFlow<RequestResult?> = _result.asStateFlow()

    /**
     * Busca en Firestore si el email ingresado corresponde a un usuario registrado.
     *
     * Flujo:
     * 1. Emite [RequestResult.Loading] para activar el spinner en el botón.
     * 2. Consulta el repositorio con el email.
     * 3. Si no existe el usuario, emite [RequestResult.Failure].
     * 4. Si existe, emite [RequestResult.Success] para que la UI navegue
     *    hacia la pantalla de restablecimiento de contraseña.
     * 5. Cualquier excepción de red o Firestore emite [RequestResult.Failure].
     */
    fun sendRecoveryEmail() {
        if (!isFormValid) return

        viewModelScope.launch {
            _result.value = RequestResult.Loading

            try {
                val user = userRepository.findByEmail(email.value)

                if (user == null) {
                    _result.value = RequestResult.Failure(
                        resources.getString(R.string.error_user_not_found)
                    )
                    return@launch
                }

                // Usuario encontrado: habilita el flujo de restablecimiento
                _result.value = RequestResult.Success(
                    resources.getString(R.string.password_forget_success)
                )

            } catch (e: Exception) {
                _result.value = RequestResult.Failure(
                    e.message ?: resources.getString(R.string.password_forget_failure)
                )
            }
        }
    }

    /** Reinicia el estado del resultado. */
    fun resetResult() {
        _result.value = null
    }
}