package com.miempresa.comuniapp.data.repository

import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.model.UserRole
import com.miempresa.comuniapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(): UserRepository {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    override val users: StateFlow<List<User>> = _users.asStateFlow()

    init {
        _users.value = fetchUsers()
    }

    override suspend fun save(user: User) {
        _users.value += user
    }

    override suspend fun findById(id: String): User? {
        return _users.value.firstOrNull { it.id == id }
    }

    override suspend fun findByEmail(email: String): User? {
        return _users.value.firstOrNull { it.email == email }
    }

    override suspend fun login(email: String, password: String): User? {
        return _users.value.firstOrNull { it.email == email && it.password == password }
    }

    override suspend fun update(user: User) {
        val index = _users.value.indexOfFirst { it.id == user.id }

        if (index != -1) {
            _users.value = _users.value.toMutableList().apply {
                set(index, user)
            }
        }
    }

    override suspend fun getAll(): List<User> {
        return _users.value
    }

    private fun fetchUsers(): List<User> {
        return listOf(
            User(
                id = "1",
                name = "Juan",
                city = "Ciudad 1",
                address = "Calle 123",
                email = "juan@email.com",
                password = "111111",
                profilePictureUrl = "https://m.media-amazon.com/images/I/41g6jROgo0L.png"
            ),
            User(
                id = "2",
                name = "Maria",
                city = "Pereira",
                address = "Calle 456",
                email = "maria@email.com",
                password = "222222",
                profilePictureUrl = "https://picsum.photos/200?random=2"
            ),
            User(
                id = "3",
                name = "Carlos",
                city = "Armenia",
                address = "Calle 789",
                email = "carlos@email.com",
                password = "333333",
                profilePictureUrl = "https://picsum.photos/200?random=3",
                role = UserRole.ADMIN
            )
        )
    }
}