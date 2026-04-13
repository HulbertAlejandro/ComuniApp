package com.miempresa.comuniapp.features.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ValidatedField
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    // ⚠️ Esto debe venir del flujo anterior (Fase 2 simplificado)
    var email: String = ""

    val newPassword = ValidatedField("") {
        when {
            it.isBlank() -> "La contraseña es obligatoria"
            it.length < 6 -> "Mínimo 6 caracteres"
            else -> null
        }
    }

    val confirmPassword = ValidatedField("") {
        when {
            it.isBlank() -> "Confirma la contraseña"
            it != newPassword.value -> "No coinciden"
            else -> null
        }
    }

    val isFormValid: Boolean
        get() = newPassword.isValid && confirmPassword.isValid

    private val _result = MutableStateFlow<RequestResult?>(null)
    val result: StateFlow<RequestResult?> = _result.asStateFlow()

    fun resetPassword() {
        if (!isFormValid || email.isBlank()) {
            _result.value = RequestResult.Failure("Error en la solicitud")
            return
        }

        viewModelScope.launch {
            _result.value = RequestResult.Loading

            try {
                val user = userRepository.findByEmail(email)

                if (user == null) {
                    _result.value =
                        RequestResult.Failure("Usuario no encontrado")
                    return@launch
                }

                // Simulación de delay
                delay(1500)

                userRepository.updatePassword(email, newPassword.value)

                _result.value =
                    RequestResult.Success("Contraseña actualizada")

            } catch (e: Exception) {
                _result.value =
                    RequestResult.Failure(
                        e.message ?: "Error al actualizar"
                    )
            }
        }
    }

    fun resetResult() {
        _result.value = null
    }
}