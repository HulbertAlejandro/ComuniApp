package com.miempresa.comuniapp.data.repository.memory

import com.miempresa.comuniapp.domain.model.*
import com.miempresa.comuniapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor() : UserRepository {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    override val users: StateFlow<List<User>> = _users.asStateFlow()

    /**
     * Usuario autenticado actualmente.
     * Simula el comportamiento de FirebaseAuth.currentUser.
     */
    private val _currentUser = MutableStateFlow<User?>(null)

    // 🔐 Credenciales mock (email -> password)
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

    /**
     * Simula FirebaseAuth + Firestore.
     * Guarda el usuario y registra la contraseña en memoria.
     */
    override suspend fun save(user: User, password: String) {

        if (credentials.containsKey(user.email)) {
            throw Exception("Este correo electrónico ya está registrado.")
        }

        val newUser = user.copy(
            id = (_users.value.size + 1).toString()
        )

        _users.value += newUser
        credentials[newUser.email] = password

        // Simula login automático tras registro
        _currentUser.value = newUser
    }

    /**
     * Simula login.
     */
    override suspend fun login(email: String, password: String): User? {

        val storedPassword = credentials[email]

        return if (storedPassword == password) {

            val user = _users.value.find { it.email == email }

            _currentUser.value = user

            user

        } else {
            throw Exception("Correo o contraseña incorrectos.")
        }
    }

    /**
     * Simula FirebaseAuth.signOut()
     */
    override suspend fun logout() {
        _currentUser.value = null
    }

    /**
     * Simula flujo reactivo de sesión.
     */
    override fun observeCurrentUser(): Flow<User?> = _currentUser

    /**
     * Simula envío de correo de recuperación.
     */
    override suspend fun sendPasswordResetEmail(email: String) {

        val exists = _users.value.any { it.email == email }

        if (!exists) {
            throw Exception("No existe una cuenta con este correo electrónico.")
        }

        // Mock: no hace nada realmente
    }

    // =============================
    // Perfil
    // =============================

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

        // Mantener sesión sincronizada
        if (_currentUser.value?.id == user.id) {
            _currentUser.value = user
        }
    }

    override suspend fun delete(id: String) {

        val user = findById(id)

        user?.let {
            credentials.remove(it.email)
        }

        _users.value = _users.value.filterNot { it.id == id }

        // Si era el usuario logueado, cerrar sesión
        if (_currentUser.value?.id == id) {
            _currentUser.value = null
        }
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
                reputation = user.reputation.copy(
                    level = newLevel
                )
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
    // Intereses
    // =============================

    override suspend fun addInterestToUser(userId: String, eventId: String) {

        val user = findById(userId) ?: return

        if (eventId in user.interestedEventIds) return

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
        return findById(userId)?.interestedEventIds?.toSet() ?: emptySet()
    }

    // =============================
    // Categorías favoritas
    // =============================

    override suspend fun updateFavoriteCategories(
        userId: String,
        categories: List<Category>
    ) {

        val user = findById(userId) ?: return

        update(
            user.copy(
                favoriteCategories = categories
            )
        )
    }

    override suspend fun getFavoriteCategories(userId: String): List<Category> {
        return findById(userId)?.favoriteCategories ?: emptyList()
    }

    override suspend fun updateFcmToken(userId: String, fcmToken: String) {
        TODO("Not yet implemented")
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

    private fun seedUsers(): List<User> = listOf(

        User(
            id = "1",
            name = "Juan",
            email = "juan@email.com",
            direction = "Chapinero, Bogotá"
        ),

        User(
            id = "2",
            name = "Maria",
            email = "maria@email.com",
            direction = "Usaquén, Bogotá"
        ),

        User(
            id = "3",
            name = "Admin",
            email = "admin@email.com",
            role = UserRole.MODERATOR,
            direction = "La Candelaria, Bogotá"
        )
    )
}