package com.miempresa.comuniapp.features.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ResourceProvider
import com.miempresa.comuniapp.core.utils.ValidatedField
import com.miempresa.comuniapp.domain.repository.UserRepository
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: UserRepository,
    private val sessionDataStore: SessionDataStore,
    private val resources: ResourceProvider
) : ViewModel() {

    private val _loginResult = MutableStateFlow<RequestResult?>(null)
    val loginResult: StateFlow<RequestResult?> = _loginResult.asStateFlow()


    val email = ValidatedField("") { value ->
        when {
            value.isEmpty() -> resources.getString(R.string.email_required)
            !Patterns.EMAIL_ADDRESS.matcher(value).matches() -> resources.getString(R.string.email_invalid)
            else -> null
        }
    }

    val password = ValidatedField("") { value ->
        when {
            value.isEmpty() -> resources.getString(R.string.password_required)
            value.length < 6 -> resources.getString(R.string.password_min_length)
            else -> null
        }
    }

    val isFormValid: Boolean
        get() = email.isValid && password.isValid

    fun resetForm() {
        email.reset()
        password.reset()
        _loginResult.value = null
    }

    fun login() {
        if (isFormValid) {
            viewModelScope.launch {
                _loginResult.value = RequestResult.Loading
                try {
                    val user = repository.login(email.value, password.value)
                    if (user != null) {
                        // Guardar sesión
                        sessionDataStore.saveSession(user.id, user.role)
                        _loginResult.value = RequestResult.Success(resources.getString(R.string.login_success))
                    } else {
                        _loginResult.value = RequestResult.Failure(resources.getString(R.string.invalid_credentials))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _loginResult.value = RequestResult.Failure(e.message ?: resources.getString(R.string.login_error))
                }
            }
        }
    }

    fun resetLoginResult() {
        _loginResult.value = null
    }
}