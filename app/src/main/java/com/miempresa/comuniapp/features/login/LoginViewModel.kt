package com.miempresa.comuniapp.features.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import javax.inject.Inject

/**
 * ViewModel de la pantalla de inicio de sesión.
 *
 * Gestiona la validación de campos, la llamada al repositorio de usuarios
 * y el almacenamiento de la sesión activa mediante [SessionDataStore].
 *
 * @param repository       Repositorio que consulta credenciales en Firestore.
 * @param sessionDataStore Almacén local de la sesión del usuario autenticado.
 * @param resources        Proveedor de strings para mensajes localizados.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: UserRepository,
    private val sessionDataStore: SessionDataStore,
    private val resources: ResourceProvider
) : ViewModel() {

    /** Estado del resultado del intento de inicio de sesión. */
    private val _loginResult = MutableStateFlow<RequestResult?>(null)
    val loginResult: StateFlow<RequestResult?> = _loginResult.asStateFlow()

    /** Campo de email con validación reactiva. */
    val email = ValidatedField("") { value ->
        when {
            value.isEmpty() -> resources.getString(R.string.error_email_empty)
            !Patterns.EMAIL_ADDRESS.matcher(value).matches() ->
                resources.getString(R.string.error_email_invalid)
            else -> null
        }
    }

    /** Campo de contraseña con validación reactiva. */
    val password = ValidatedField("") { value ->
        when {
            value.isEmpty() -> resources.getString(R.string.error_password_empty)
            value.length < 6 -> resources.getString(R.string.validation_error_password_length)
            else -> null
        }
    }

    /** Indica si todos los campos del formulario son válidos. */
    val isFormValid: Boolean
        get() = email.isValid && password.isValid

    /**
     * Intenta autenticar al usuario con las credenciales ingresadas.
     *
     * Flujo:
     * 1. Emite [RequestResult.Loading] para activar el indicador en la UI.
     * 2. Consulta el repositorio con email y contraseña.
     * 3. Si el usuario existe, guarda la sesión y emite [RequestResult.Success].
     * 4. Si las credenciales no coinciden, emite [RequestResult.Failure].
     * 5. Cualquier excepción inesperada también emite [RequestResult.Failure].
     */
    fun login() {
        if (!isFormValid) return

        viewModelScope.launch {
            _loginResult.value = RequestResult.Loading

            try {
                val user = repository.login(email.value, password.value)
                if (user != null) {
                    // Persiste el ID, nombre y rol del usuario en DataStore
                    sessionDataStore.saveSession(user.id, user.name, user.role)
                    _loginResult.value = RequestResult.Success(
                        resources.getString(R.string.login_success)
                    )
                } else {
                    _loginResult.value = RequestResult.Failure(
                        resources.getString(R.string.login_failure)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _loginResult.value = RequestResult.Failure(
                    e.message ?: resources.getString(R.string.error_generic)
                )
            }
        }
    }

    /** Limpia el formulario y reinicia el estado de resultado. */
    fun resetForm() {
        email.reset()
        password.reset()
        _loginResult.value = null
    }

    /** Reinicia el estado de resultado sin limpiar el formulario. */
    fun resetLoginResult() {
        _loginResult.value = null
    }
}