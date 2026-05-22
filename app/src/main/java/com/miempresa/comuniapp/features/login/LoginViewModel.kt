package com.miempresa.comuniapp.features.login

import android.util.Patterns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.resources.ResourceProvider
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ValidatedField
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * ViewModel de la pantalla de inicio de sesión.
 *
 * Responsabilidades:
 * - Validar email y contraseña.
 * - Autenticar al usuario.
 * - Guardar sesión local en DataStore.
 * - Obtener y persistir el token FCM en Firestore.
 *
 * El token FCM se guarda después del login porque:
 * - onNewToken() NO siempre se ejecuta al iniciar sesión.
 * - Firebase puede reutilizar un token ya existente.
 *
 * Estructura esperada en Firestore:
 *
 * users/{uid}
 *    ├── name
 *    ├── email
 *    ├── role
 *    └── fcmToken
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: UserRepository,
    private val sessionDataStore: SessionDataStore,
    private val resources: ResourceProvider,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // ─────────────────────────────────────────────────────────────
    // Estado UI
    // ─────────────────────────────────────────────────────────────

    private val _loginResult =
        MutableStateFlow<RequestResult?>(null)

    val loginResult: StateFlow<RequestResult?> =
        _loginResult.asStateFlow()

    // ─────────────────────────────────────────────────────────────
    // Campos del formulario
    // ─────────────────────────────────────────────────────────────

    /**
     * Campo email con validación reactiva.
     */
    val email = ValidatedField("") { value ->

        when {

            value.isEmpty() -> {
                resources.getString(
                    R.string.error_email_empty
                )
            }

            !Patterns.EMAIL_ADDRESS
                .matcher(value)
                .matches() -> {

                resources.getString(
                    R.string.error_email_invalid
                )
            }

            else -> null
        }
    }

    /**
     * Campo contraseña con validación reactiva.
     */
    val password = ValidatedField("") { value ->

        when {

            value.isEmpty() -> {
                resources.getString(
                    R.string.error_password_empty
                )
            }

            value.length < 6 -> {
                resources.getString(
                    R.string.validation_error_password_length
                )
            }

            else -> null
        }
    }

    /**
     * El formulario es válido solo si
     * ambos campos pasan la validación.
     */
    val isFormValid: Boolean
        get() = email.isValid && password.isValid

    // ─────────────────────────────────────────────────────────────
    // Login
    // ─────────────────────────────────────────────────────────────

    /**
     * Inicia sesión con las credenciales proporcionadas.
     */
    fun login() {
        if (!isFormValid) return
        viewModelScope.launch {
            _loginResult.value = RequestResult.Loading
            try {
                val user = repository.login(email.value, password.value)
                if (user != null) {
                    sessionDataStore.saveSession(user.id, user.name, user.role)

                    actualizarTokenFcm(user.id)

                    _loginResult.value = RequestResult.Success(
                        resources.getString(R.string.login_success)
                    )
                } else {
                    _loginResult.value = RequestResult.Failure(
                        resources.getString(R.string.login_failure)
                    )
                }
            } catch (e: Exception) {
                _loginResult.value = RequestResult.Failure(
                    e.message ?: resources.getString(R.string.error_generic)
                )
            }
        }
    }

    /**
     * Obtiene el token FCM actual del dispositivo y lo guarda en Firestore.
     * Si falla, lo registra silenciosamente: no debe bloquear el login.
     *
     * @param userId ID del usuario recién autenticado.
     */
    private fun actualizarTokenFcm(userId: String) {
        viewModelScope.launch {
            runCatching {
                val token = com.google.firebase.messaging.FirebaseMessaging
                    .getInstance()
                    .token
                    .await()
                repository.updateFcmToken(userId, token)
            }.onFailure { e ->
                android.util.Log.e("FCM", "Error al registrar token FCM: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Limpia formulario y estado.
     */
    fun resetForm() {

        email.reset()
        password.reset()

        _loginResult.value = null
    }

    /**
     * Limpia únicamente el resultado del login.
     */
    fun resetLoginResult() {

        _loginResult.value = null
    }
}