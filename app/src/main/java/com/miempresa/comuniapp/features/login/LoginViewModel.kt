package com.miempresa.comuniapp.features.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ValidatedField
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

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
            val user = repository.login(email.value, password.value)
            _loginResult.value = if (user != null) {
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
