package com.miempresa.comuniapp.features.password

import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ValidatedField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ForgetPasswordViewModel : ViewModel() {

    val email = ValidatedField("") { value ->
        when {
            value.isBlank() -> "El email es obligatorio"
            !Patterns.EMAIL_ADDRESS.matcher(value).matches() -> "Email inválido"
            else -> null
        }
    }

    val isFormValid: Boolean
        get() = email.isValid

    private val _result = MutableStateFlow<RequestResult?>(null)
    val result: StateFlow<RequestResult?> = _result.asStateFlow()

    fun sendRecoveryEmail() {
        if (isFormValid) {
            _result.value =
                RequestResult.Success("Se envió el enlace de recuperación")
        } else {
            _result.value =
                RequestResult.Failure("Formulario inválido")
        }
    }

    fun resetResult() {
        _result.value = null
    }
}