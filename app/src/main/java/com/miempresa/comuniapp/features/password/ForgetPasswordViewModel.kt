package com.miempresa.comuniapp.features.password

import android.util.Patterns
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
class ForgetPasswordViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val email = ValidatedField("") {
        when {
            it.isBlank() -> "El email es obligatorio"
            !Patterns.EMAIL_ADDRESS.matcher(it).matches() -> "Email inválido"
            else -> null
        }
    }

    val isFormValid: Boolean
        get() = email.isValid

    private val _result = MutableStateFlow<RequestResult?>(null)
    val result: StateFlow<RequestResult?> = _result.asStateFlow()

    fun sendRecoveryEmail() {
        if (!isFormValid) return

        viewModelScope.launch {
            _result.value = RequestResult.Loading

            try {
                val user = userRepository.findByEmail(email.value)

                if (user == null) {
                    _result.value =
                        RequestResult.Failure("No existe un usuario con ese email")
                    return@launch
                }

                // Simulación de envío de correo (Fase 2)
                delay(1500)

                _result.value =
                    RequestResult.Success("Se envió el enlace de recuperación")

            } catch (e: Exception) {
                _result.value =
                    RequestResult.Failure(
                        e.message ?: "Error al enviar el correo"
                    )
            }
        }
    }

    fun resetResult() {
        _result.value = null
    }
}