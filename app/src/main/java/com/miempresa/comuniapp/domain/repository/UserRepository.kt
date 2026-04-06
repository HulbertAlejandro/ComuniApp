package com.miempresa.comuniapp.domain.repository

import com.miempresa.comuniapp.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {

    val users: StateFlow<List<User>>

    suspend fun save(user: User)

    suspend fun findById(id: String): User?

    suspend fun findByEmail(email: String): User?

    suspend fun login(email: String, password: String): User?

    suspend fun update(user: User)

    suspend fun getAll(): List<User>
}
