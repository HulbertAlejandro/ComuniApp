package com.miempresa.comuniapp.features.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ValidatedField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel : ViewModel() {

    val email = ValidatedField("") { value ->
        when {
            value.isEmpty() -> "El email es obligatorio"
            !Patterns.EMAIL_ADDRESS.matcher(value).matches() -> "Ingresa un email válido"
            else -> null
        }
    }

    val password = ValidatedField("") { value ->
        when {
            value.isEmpty() -> "La contraseña es obligatoria"
            value.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
            else -> null
        }
    }

    val isFormValid: Boolean
        get() = email.isValid && password.isValid

    private val _loginResult = MutableStateFlow<RequestResult?>(null)
    val loginResult: StateFlow<RequestResult?> = _loginResult.asStateFlow()

    fun login() {
        if (isFormValid) {

            _loginResult.value =
                if (email.value == "carlos@email.com" &&
                    password.value == "123456"
                ) {
                    RequestResult.Success("Login exitoso")
                } else {
                    RequestResult.Failure("Credenciales inválidas")
                }
        }
    }

    fun resetLoginResult() {
        _loginResult.value = null
    }

    fun resetForm() {
        email.reset()
        password.reset()
    }
}