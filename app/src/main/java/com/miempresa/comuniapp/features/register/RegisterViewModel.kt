package com.miempresa.comuniapp.features.register

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ValidatedField
import com.miempresa.comuniapp.domain.model.*
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    val name = ValidatedField("") {
        if (it.isBlank()) "El nombre es obligatorio" else null
    }

    val city = ValidatedField("") {
        if (it.isBlank()) "La ciudad es obligatoria" else null
    }

    val address = ValidatedField("") {
        if (it.isBlank()) "La dirección es obligatoria" else null
    }

    val email = ValidatedField("") {
        when {
            it.isBlank() -> "El email es obligatorio"
            !Patterns.EMAIL_ADDRESS.matcher(it).matches() -> "Email inválido"
            else -> null
        }
    }

    val password = ValidatedField("") {
        when {
            it.isBlank() -> "La contraseña es obligatoria"
            it.length < 6 -> "Mínimo 6 caracteres"
            else -> null
        }
    }

    val confirmPassword = ValidatedField("") {
        when {
            it.isBlank() -> "Confirma la contraseña"
            it != password.value -> "Las contraseñas no coinciden"
            else -> null
        }
    }

    val isFormValid: Boolean
        get() = name.isValid &&
                city.isValid &&
                address.isValid &&
                email.isValid &&
                password.isValid &&
                confirmPassword.isValid

    private val _registerResult = MutableStateFlow<RequestResult?>(null)
    val registerResult: StateFlow<RequestResult?> = _registerResult.asStateFlow()

    fun register() {
        if (isFormValid) {
            viewModelScope.launch {

                _registerResult.value = RequestResult.Loading

                try {
                    val existingUser = repository.findByEmail(email.value)

                    if (existingUser != null) {
                        _registerResult.value =
                            RequestResult.Failure("El email ya está registrado")
                        return@launch
                    }

                    // Ubicación simulada (Fase 2)
                    val location = Location(
                        latitude = 4.6097,
                        longitude = -74.0817
                    )

                    val newUser = User(
                        id = UUID.randomUUID().toString(),
                        name = name.value,
                        email = email.value,
                        phoneNumber = "",
                        profilePictureUrl = "",
                        location = location,
                        role = UserRole.USER,
                        reputation = Reputation()
                    )

                    // Password NO va en el modelo de dominio
                    repository.saveWithPassword(newUser, password.value)

                    _registerResult.value =
                        RequestResult.Success("Registro exitoso")

                } catch (e: Exception) {
                    _registerResult.value =
                        RequestResult.Failure(
                            e.message ?: "Error al registrar"
                        )
                }
            }
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