package com.miempresa.comuniapp.domain.repository

import com.miempresa.comuniapp.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val users: StateFlow<List<User>>
    fun save(user: User)
    fun findById(id: String): User?
    fun findByEmail(email: String): User?
    fun login(email: String, password: String): User?
}
