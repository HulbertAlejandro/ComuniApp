package com.miempresa.comuniapp.features.register

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.resources.ResourceProvider
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ValidatedField
import com.miempresa.comuniapp.domain.model.*
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: UserRepository,
    private val resources: ResourceProvider
) : ViewModel() {

    val name = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.error_name_empty) else null
    }

    val email = ValidatedField("") {
        when {
            it.isBlank() -> resources.getString(R.string.error_email_empty)
            !Patterns.EMAIL_ADDRESS.matcher(it).matches() -> resources.getString(R.string.error_email_invalid)
            else -> null
        }
    }

    val phone = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.error_phone_empty) else null
    }

    val password = ValidatedField("") {
        when {
            it.isBlank() -> resources.getString(R.string.error_password_required)
            it.length < 6 -> resources.getString(R.string.error_password_min_length)
            else -> null
        }
    }

    val confirmPassword = ValidatedField("") {
        when {
            it.isBlank() -> resources.getString(R.string.error_confirm_password_empty)
            it != password.value -> resources.getString(R.string.error_confirm_password_mismatch_message)
            else -> null
        }
    }

    // Categorías favoritas seleccionadas durante el registro
    private val _selectedCategories = MutableStateFlow<Set<Category>>(emptySet())
    val selectedCategories: StateFlow<Set<Category>> = _selectedCategories.asStateFlow()

    fun toggleCategory(category: Category) {
        _selectedCategories.update { current ->
            if (current.contains(category)) current - category else current + category
        }
    }

    val isFormValid: Boolean
        get() = name.isValid &&
                email.isValid &&
                phone.isValid &&
                password.isValid &&
                confirmPassword.isValid

    private val _registerResult = MutableStateFlow<RequestResult?>(null)
    val registerResult: StateFlow<RequestResult?> = _registerResult

    fun register() {
        if (!isFormValid) return

        viewModelScope.launch {
            _registerResult.value = RequestResult.Loading

            try {
                val exists = repository.findByEmail(email.value)
                if (exists != null) {
                    _registerResult.value = RequestResult.Failure(resources.getString(R.string.error_email_already_exists))
                    return@launch
                }

                val user = User(
                    id = UUID.randomUUID().toString(),
                    name = name.value.trim(),
                    email = email.value.trim(),
                    phoneNumber = phone.value.trim(),
                    profilePictureUrl = "https://i.pravatar.cc/300",
                    location = Location(latitude = 4.6097, longitude = -74.0817),
                    role = UserRole.USER,
                    reputation = Reputation(
                        points = 0,
                        level = UserLevel.ESPECTADOR,
                        badges = emptyList()
                    ),
                    // Categorías elegidas durante el registro
                    favoriteCategories = _selectedCategories.value.toList()
                )

                repository.saveWithPassword(user, password.value)
                _registerResult.value = RequestResult.Success(resources.getString(R.string.register_success))

            } catch (e: Exception) {
                _registerResult.value = RequestResult.Failure(e.message ?: resources.getString(R.string.error_generic))
            }
        }
    }

    fun resetRegisterResult() { _registerResult.value = null }

    fun resetForm() {
        name.reset()
        email.reset()
        phone.reset()
        password.reset()
        confirmPassword.reset()
        _selectedCategories.value = emptySet()
    }
}