package com.miempresa.comuniapp.features.register

import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ValidatedField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RegisterViewModel : ViewModel() {

    val name = ValidatedField("") { value ->
        if (value.isBlank()) "El nombre es obligatorio" else null
    }

    val city = ValidatedField("") { value ->
        if (value.isBlank()) "La ciudad es obligatoria" else null
    }

    val address = ValidatedField("") { value ->
        if (value.isBlank()) "La dirección es obligatoria" else null
    }

    val email = ValidatedField("") { value ->
        when {
            value.isBlank() -> "El email es obligatorio"
            !Patterns.EMAIL_ADDRESS.matcher(value).matches() -> "Ingresa un email válido"
            else -> null
        }
    }

    val password = ValidatedField("") { value ->
        when {
            value.isBlank() -> "La contraseña es obligatoria"
            value.length < 6 -> "Mínimo 6 caracteres"
            else -> null
        }
    }

    val confirmPassword = ValidatedField("") { value ->
        when {
            value.isBlank() -> "Confirma la contraseña"
            value != password.value -> "Las contraseñas no coinciden"
            else -> null
        }
    }

    val isFormValid: Boolean
        get() =
            name.isValid &&
                    city.isValid &&
                    address.isValid &&
                    email.isValid &&
                    password.isValid &&
                    confirmPassword.isValid

    private val _registerResult = MutableStateFlow<RequestResult?>(null)
    val registerResult: StateFlow<RequestResult?> =
        _registerResult.asStateFlow()

    fun register() {
        if (isFormValid) {
            _registerResult.value =
                RequestResult.Success("Registro exitoso")
        } else {
            _registerResult.value =
                RequestResult.Failure("Formulario inválido")
        }
    }

    fun resetRegisterResult() {
        _registerResult.value = null
    }

    fun resetForm() {
        name.reset()
        city.reset()
        address.reset()
        email.reset()
        password.reset()
        confirmPassword.reset()
    }
}