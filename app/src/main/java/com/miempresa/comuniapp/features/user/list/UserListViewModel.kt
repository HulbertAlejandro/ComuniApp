package com.miempresa.comuniapp.features.user.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.model.UserRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserListViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    init {
        fetchUsers()
    }

    private fun fetchUsers() {
        viewModelScope.launch {
            // Simulación de carga de datos
            delay(1000)
            _users.value = listOf(
                User(
                    id = "1",
                    name = "Carlos Pérez",
                    city = "Madrid",
                    address = "Calle Gran Vía 123",
                    email = "carlos@email.com",
                    password = "123456",
                    phoneNumber = "600123456",
                    profilePictureUrl = "",
                    role = UserRole.USER
                ),
                User(
                    id = "2",
                    name = "María García",
                    city = "Barcelona",
                    address = "Avenida Diagonal 456",
                    email = "maria@email.com",
                    password = "123456",
                    phoneNumber = "600789012",
                    profilePictureUrl = "",
                    role = UserRole.USER
                ),
                User(
                    id = "3",
                    name = "Administrador",
                    city = "Valencia",
                    address = "Plaza del Ayuntamiento 1",
                    email = "admin@comuniapp.com",
                    password = "admin123",
                    phoneNumber = "900123456",
                    profilePictureUrl = "",
                    role = UserRole.ADMIN
                )
            )
        }
    }

    fun findById(id: String): User? {
        return _users.value.find { it.id == id }
    }
}
