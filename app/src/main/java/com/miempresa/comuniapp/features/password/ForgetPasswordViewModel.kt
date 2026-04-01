package com.miempresa.comuniapp.features.password

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ValidatedField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import javax.inject.Inject

@HiltViewModel
class ForgetPasswordViewModel @Inject constructor() : ViewModel() {

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
        if (isFormValid) {
            viewModelScope.launch {

                _result.value = RequestResult.Loading

                try {
                    // Simulación (luego conectas backend)
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
    }

    fun resetResult() {
        _result.value = null
    }
}