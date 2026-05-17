package com.miempresa.comuniapp.data.repository.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.miempresa.comuniapp.core.utils.toUserMessage
import com.miempresa.comuniapp.domain.model.Badge
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.model.UserLevel
import com.miempresa.comuniapp.domain.model.UserRole
import com.miempresa.comuniapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [UserRepository] usando Firebase Firestore.
 *
 * Características implementadas:
 * Soporte offline mediante PersistentCacheSettings.
 * Manejo específico de FirebaseFirestoreException.
 * Uso obligatorio de await() en operaciones suspend.
 * Mapeo correcto del ID del documento Firestore → modelo.
 * Uso correcto de enums mediante .name.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    /**
     * Referencia a la colección users.
     */
    private val collection = firestore.collection("users")

    private val _users = MutableStateFlow<List<User>>(emptyList())

    override val users: StateFlow<List<User>> = _users.asStateFlow()

    init {
        /**
         * Listener reactivo en tiempo real.
         *
         * Gracias al caché persistente:
         * - primero emite datos locales
         * - luego sincroniza con servidor
         */
        collection.addSnapshotListener { snapshot, error ->

            if (error != null) return@addSnapshotListener

            snapshot?.let {

                _users.value = it.documents.mapNotNull { doc ->

                    doc.toObject(User::class.java)?.apply {
                        id = doc.id
                    }
                }
            }
        }
    }

    // =========================================================
    // AUTH
    // =========================================================

    override suspend fun saveWithPassword(user: User, password: String) {

        try {

            val docRef = collection.document()

            val userWithId = user.copy(id = docRef.id)

            val batch = firestore.batch()

            batch.set(docRef, userWithId)

            batch.set(
                docRef.collection("credentials").document("password"),
                mapOf("value" to password)
            )

            batch.commit().await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al registrar el usuario: ${e.message}")
        }
    }

    override suspend fun login(
        email: String,
        password: String
    ): User? {

        try {

            val snapshot = collection
                .whereEqualTo("email", email)
                .get()
                .await()

            val doc = snapshot.documents.firstOrNull()
                ?: return null

            val user = doc.toObject(User::class.java)?.apply {
                id = doc.id
            } ?: return null

            val credentialsDoc = collection
                .document(user.id)
                .collection("credentials")
                .document("password")
                .get()
                .await()

            val storedPassword = credentialsDoc.getString("value")

            return if (storedPassword == password) {
                user
            } else {
                null
            }

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al iniciar sesión: ${e.message}")
        }
    }

    override suspend fun findById(id: String): User? {

        try {

            val doc = collection
                .document(id)
                .get()
                .await()

            return doc.toObject(User::class.java)?.apply {
                this.id = doc.id
            }

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al buscar el usuario: ${e.message}")
        }
    }

    override suspend fun findByEmail(email: String): User? {

        try {

            val snapshot = collection
                .whereEqualTo("email", email)
                .get()
                .await()

            val doc = snapshot.documents.firstOrNull()
                ?: return null

            return doc.toObject(User::class.java)?.apply {
                id = doc.id
            }

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al buscar el usuario por email: ${e.message}")
        }
    }

    /**
     * Consulta por IDs aprovechando caché local.
     */
    override suspend fun getUsersByIds(ids: List<String>): List<User> {

        return ids.mapNotNull { id ->
            findById(id)
        }
    }

    override suspend fun update(user: User) {

        try {

            collection
                .document(user.id)
                .set(user)
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al actualizar el usuario: ${e.message}")
        }
    }

    override suspend fun updatePassword(
        email: String,
        newPassword: String
    ) {

        try {

            val user = findByEmail(email)
                ?: return

            collection
                .document(user.id)
                .collection("credentials")
                .document("password")
                .set(mapOf("value" to newPassword))
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al actualizar la contraseña: ${e.message}")
        }
    }

    override suspend fun delete(id: String) {

        try {

            collection
                .document(id)
                .delete()
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al eliminar el usuario: ${e.message}")
        }
    }

    // =========================================================
    // REPUTACIÓN
    // =========================================================

    override suspend fun addPoints(
        userId: String,
        points: Int
    ) {

        try {

            collection
                .document(userId)
                .update(
                    "reputation.points",
                    FieldValue.increment(points.toLong())
                )
                .await()

            updateLevel(userId)

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al agregar puntos: ${e.message}")
        }
    }

    override suspend fun updateLevel(userId: String) {

        try {

            val user = findById(userId)
                ?: return

            val newLevel = calculateLevel(user.reputation.points)

            collection
                .document(userId)
                .update(
                    "reputation.level",
                    newLevel.name
                )
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al actualizar el nivel: ${e.message}")
        }
    }

    override suspend fun addBadge(
        userId: String,
        badge: Badge
    ) {

        try {

            collection
                .document(userId)
                .update(
                    "reputation.badges",
                    FieldValue.arrayUnion(badge)
                )
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al agregar la insignia: ${e.message}")
        }
    }

    // =========================================================
    // ROLES
    // =========================================================

    /**
     * Consulta avanzada usando enum.name.
     */
    override suspend fun getModerators(): List<User> {

        try {

            val snapshot = collection
                .whereEqualTo(
                    "role",
                    UserRole.MODERATOR.name
                )
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->

                doc.toObject(User::class.java)?.apply {
                    id = doc.id
                }
            }

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al consultar moderadores: ${e.message}")
        }
    }

    // =========================================================
    // INTERESES
    // =========================================================

    override suspend fun addInterestToUser(
        userId: String,
        eventId: String
    ) {

        try {

            collection
                .document(userId)
                .update(
                    "interestedEventIds",
                    FieldValue.arrayUnion(eventId)
                )
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al agregar interés: ${e.message}")
        }
    }

    override suspend fun removeInterestFromUser(
        userId: String,
        eventId: String
    ) {

        try {

            collection
                .document(userId)
                .update(
                    "interestedEventIds",
                    FieldValue.arrayRemove(eventId)
                )
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al eliminar interés: ${e.message}")
        }
    }

    override suspend fun getUserInterestedEventIds(userId: String): Set<String> {

        return try {

            findById(userId)?.interestedEventIds?.toSet()
                ?: emptySet()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al consultar intereses: ${e.message}")
        }
    }

    // =========================================================
    // CATEGORÍAS FAVORITAS
    // =========================================================

    override suspend fun updateFavoriteCategories(
        userId: String,
        categories: List<Category>
    ) {

        try {

            /**
             * Conversión correcta de enums → String.
             */
            collection
                .document(userId)
                .update(
                    "favoriteCategories",
                    categories.map { it.name }
                )
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al actualizar categorías favoritas: ${e.message}")
        }
    }

    override suspend fun getFavoriteCategories(userId: String): List<Category> {

        return try {

            findById(userId)?.favoriteCategories
                ?: emptyList()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al consultar categorías favoritas: ${e.message}")
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private fun calculateLevel(points: Int): UserLevel {

        return when {

            points < 100 -> UserLevel.ESPECTADOR

            points < 300 -> UserLevel.PARTICIPANTE

            points < 600 -> UserLevel.ORGANIZADOR

            else -> UserLevel.LIDER_COMUNITARIO
        }
    }
}