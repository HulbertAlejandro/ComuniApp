package com.miempresa.comuniapp.features.user.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UserRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val session = sessionDataStore.sessionFlow.firstOrNull()

            session?.let {
                val foundUser = repository.findById(it.userId)
                _user.value = foundUser
            }
        }
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun updateUser(updated: User) {
        viewModelScope.launch {
            repository.update(updated)
            _user.value = updated
            _isEditMode.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionDataStore.clearSession()
        }
    }
}