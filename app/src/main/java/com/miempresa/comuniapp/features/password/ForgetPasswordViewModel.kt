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

    /** Verifica si el email existe en Firestore y emite el resultado. */
    fun sendRecoveryEmail() {
        if (!isFormValid) return

        viewModelScope.launch {
            _result.value = RequestResult.Loading
            try {
                // Firebase Auth envía el enlace; no necesitamos verificar
                // si el email existe primero (Firebase lo hace internamente)
                userRepository.sendPasswordResetEmail(email.value)

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