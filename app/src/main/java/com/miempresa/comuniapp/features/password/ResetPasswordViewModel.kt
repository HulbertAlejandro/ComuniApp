package com.miempresa.comuniapp.features.password

import androidx.lifecycle.ViewModel
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ValidatedField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import javax.inject.Inject

@HiltViewModel
class ResetPasswordViewModel @Inject constructor() : ViewModel() {

    val newPassword = ValidatedField("") { value ->
        when {
            value.isBlank() -> "La contraseña es obligatoria"
            value.length < 6 -> "Mínimo 6 caracteres"
            else -> null
        }
    }

    val confirmPassword = ValidatedField("") { value ->
        when {
            value.isBlank() -> "Confirma la contraseña"
            value != newPassword.value -> "No coinciden"
            else -> null
        }
    }

    val isFormValid: Boolean
        get() = newPassword.isValid && confirmPassword.isValid

    private val _result = MutableStateFlow<RequestResult?>(null)
    val result: StateFlow<RequestResult?> = _result.asStateFlow()

    fun resetPassword() {
        if (isFormValid) {
            _result.value =
                RequestResult.Success("Contraseña actualizada correctamente")
        } else {
            _result.value =
                RequestResult.Failure("Formulario inválido")
        }
    }

    fun resetResult() {
        _result.value = null
    }
}