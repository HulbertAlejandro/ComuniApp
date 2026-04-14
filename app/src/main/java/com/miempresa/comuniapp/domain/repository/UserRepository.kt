package com.miempresa.comuniapp.domain.repository

import com.miempresa.comuniapp.domain.model.Badge
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {

    val users: StateFlow<List<User>>

    suspend fun saveWithPassword(user: User, password: String)
    suspend fun findById(id: String): User?
    suspend fun findByEmail(email: String): User?
    suspend fun login(email: String, password: String): User?
    suspend fun update(user: User)
    suspend fun updatePassword(email: String, newPassword: String)
    suspend fun delete(id: String)

    // Reputación
    suspend fun addPoints(userId: String, points: Int)
    suspend fun updateLevel(userId: String)
    suspend fun addBadge(userId: String, badge: Badge)

    // Roles
    suspend fun getModerators(): List<User>

    // Ubicación
    suspend fun getUsersNearby(latitude: Double, longitude: Double, radiusKm: Double): List<User>

    // Intereses por evento
    suspend fun addInterestToUser(userId: String, eventId: String)
    suspend fun removeInterestFromUser(userId: String, eventId: String)
    suspend fun getUserInterestedEventIds(userId: String): Set<String>

    // Categorías favoritas
    suspend fun updateFavoriteCategories(userId: String, categories: List<Category>)
    suspend fun getFavoriteCategories(userId: String): List<Category>
}