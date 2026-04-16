package com.miempresa.comuniapp.data.repository.memory

import com.miempresa.comuniapp.domain.model.*
import com.miempresa.comuniapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class UserRepositoryImpl @Inject constructor() : UserRepository {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    override val users: StateFlow<List<User>> = _users.asStateFlow()

    // 🔐 Credenciales (email -> password)
    private val credentials = mutableMapOf<String, String>()

    init {
        val initialUsers = seedUsers()
        _users.value = initialUsers

        // Passwords mock
        initialUsers.forEach {
            credentials[it.email] = "123456"
        }
    }

    // =============================
    // Auth
    // =============================

    override suspend fun saveWithPassword(user: User, password: String) {
        _users.value += user
        credentials[user.email] = password
    }

    override suspend fun login(email: String, password: String): User? {
        val storedPassword = credentials[email]
        return if (storedPassword == password) {
            _users.value.find { it.email == email }
        } else null
    }

    override suspend fun findById(id: String): User? =
        _users.value.find { it.id == id }

    override suspend fun findByEmail(email: String): User? =
        _users.value.find { it.email == email }

    override suspend fun getUsersByIds(ids: List<String>): List<User> =
        _users.value.filter { it.id in ids }

    override suspend fun update(user: User) {
        _users.value = _users.value.map {
            if (it.id == user.id) user else it
        }
    }

    override suspend fun delete(id: String) {
        val user = findById(id)
        user?.let {
            credentials.remove(it.email)
        }
        _users.value = _users.value.filterNot { it.id == id }
    }

    override suspend fun updatePassword(email: String, newPassword: String) {
        credentials[email] = newPassword
    }

    // =============================
    // Reputación
    // =============================

    override suspend fun addPoints(userId: String, points: Int) {
        val user = findById(userId) ?: return

        val newPoints = user.reputation.points + points
        val newLevel = calculateLevel(newPoints)

        update(
            user.copy(
                reputation = user.reputation.copy(
                    points = newPoints,
                    level = newLevel
                )
            )
        )
    }

    override suspend fun updateLevel(userId: String) {
        val user = findById(userId) ?: return

        val newLevel = calculateLevel(user.reputation.points)

        update(
            user.copy(
                reputation = user.reputation.copy(level = newLevel)
            )
        )
    }

    override suspend fun addBadge(userId: String, badge: Badge) {
        val user = findById(userId) ?: return

        update(
            user.copy(
                reputation = user.reputation.copy(
                    badges = user.reputation.badges + badge
                )
            )
        )
    }

    // =============================
    // Roles
    // =============================

    override suspend fun getModerators(): List<User> =
        _users.value.filter { it.role == UserRole.MODERATOR }

    // =============================
    // Ubicación
    // =============================

    override suspend fun getUsersNearby(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<User> {
        return _users.value.filter {
            distanceKm(
                latitude,
                longitude,
                it.location.latitude,
                it.location.longitude
            ) <= radiusKm
        }
    }

    // =============================
    // Helpers
    // =============================

    private fun calculateLevel(points: Int): UserLevel {
        return when {
            points < 100 -> UserLevel.ESPECTADOR
            points < 300 -> UserLevel.PARTICIPANTE
            points < 600 -> UserLevel.ORGANIZADOR
            else -> UserLevel.LIDER_COMUNITARIO
        }
    }

    private fun distanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun seedUsers(): List<User> {
        return listOf(
            User(
                id = "1",
                name = "Juan",
                email = "juan@email.com",
                location = Location(4.6097, -74.0817)
            ),
            User(
                id = "2",
                name = "Maria",
                email = "maria@email.com",
                location = Location(4.6100, -74.0820)
            ),
            User(
                id = "3",
                name = "Admin",
                email = "admin@email.com",
                role = UserRole.MODERATOR,
                location = Location(4.6110, -74.0830)
            )
        )
    }

    // =============================
    // Intereses por usuario
    // =============================

    override suspend fun addInterestToUser(userId: String, eventId: String) {
        val user = findById(userId) ?: return
        update(
            user.copy(
                interestedEventIds = user.interestedEventIds + eventId
            )
        )
    }

    override suspend fun removeInterestFromUser(userId: String, eventId: String) {
        val user = findById(userId) ?: return
        update(
            user.copy(
                interestedEventIds = user.interestedEventIds - eventId
            )
        )
    }

    override suspend fun getUserInterestedEventIds(userId: String): Set<String> {
        return findById(userId)?.interestedEventIds ?: emptySet()
    }

    // =============================
    // Categorías favoritas
    // =============================

    override suspend fun updateFavoriteCategories(userId: String, categories: List<Category>) {
        val user = findById(userId) ?: return
        update(user.copy(favoriteCategories = categories))
    }

    override suspend fun getFavoriteCategories(userId: String): List<Category> {
        return findById(userId)?.favoriteCategories ?: emptyList()
    }
}