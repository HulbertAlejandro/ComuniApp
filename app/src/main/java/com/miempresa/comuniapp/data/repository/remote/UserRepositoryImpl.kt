package com.miempresa.comuniapp.data.repository.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.miempresa.comuniapp.core.utils.toUserMessage
import com.miempresa.comuniapp.domain.model.*
import com.miempresa.comuniapp.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [UserRepository] que combina Firebase Authentication
 * para la gestión de identidad y Firebase Firestore para el perfil del usuario.
 *
 * Responsabilidades separadas:
 * - [FirebaseAuth]:      registro, login, logout, verificación de email,
 *                        recuperación de contraseña.
 * - [FirebaseFirestore]: perfil público (nombre, reputación, intereses, etc.).
 *
 * Todas las operaciones de escritura capturan [FirebaseAuthException] y
 * [FirebaseFirestoreException] antes del catch genérico para dar mensajes
 * de error legibles al usuario.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : UserRepository {

    /** Referencia a la colección "users" en Firestore. */
    private val collection = firestore.collection("users")

    private val _users = MutableStateFlow<List<User>>(emptyList())
    override val users: StateFlow<List<User>> = _users.asStateFlow()

    init {
        // Escucha en tiempo real: emite primero desde caché local (offline)
        // y luego desde el servidor cuando hay conexión disponible.
        collection.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            snapshot?.let {
                _users.value = it.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.apply { id = doc.id }
                }
            }
        }
    }

    /**
     * Flujo que emite el usuario autenticado actualmente.
     * Emite null cuando no hay sesión activa o si ocurre un error.
     *
     * Implementación:
     * - Usa [FirebaseAuth.AuthStateListener] para detectar cambios de sesión.
     * - Si hay un usuario autenticado, escucha su documento en Firestore
     *   para emitir su perfil actualizado en tiempo real.
     * - Si no hay usuario, emite null.
     *
     * El [awaitClose] asegura que se limpien los listeners al cancelar la
     * colección del Flow, evitando fugas de memoria.
     */
    override fun observeCurrentUser(): Flow<User?> = callbackFlow {

        var registration: ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->

            registration?.remove()

            val firebaseUser = firebaseAuth.currentUser

            if (firebaseUser == null) {
                trySend(null)
                return@AuthStateListener
            }

            registration = collection
                .document(firebaseUser.uid)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        trySend(null)
                        return@addSnapshotListener
                    }

                    val user = snapshot
                        ?.toObject(User::class.java)
                        ?.apply { id = snapshot.id }

                    trySend(user)
                }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            registration?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }

    // =============================
    // Autenticación — Firebase Auth
    // =============================

    /**
     * Registra un nuevo usuario en Firebase Auth y guarda su perfil en Firestore.
     *
     * Flujo detallado:
     * 1. [FirebaseAuth.createUserWithEmailAndPassword]: crea la cuenta con email/password.
     * 2. Obtiene el UID generado por Firebase Auth.
     * 3. Crea una copia del usuario con ese UID como ID (sin contraseña).
     * 4. Guarda el perfil en Firestore bajo "users/{uid}".
     * 5. Envía el correo de verificación al email registrado.
     *
     * El correo de verificación se configura visualmente en la consola de Firebase
     * (Autenticación → Plantillas de correo electrónico), no en el código Android.
     *
     * @param user     Perfil del nuevo usuario (id y contraseña se ignoran aquí).
     * @param password Contraseña para Firebase Auth; NO se guarda en Firestore.
     * @throws Exception si el email ya está en uso, la contraseña es débil, o falla la red.
     */
    override suspend fun save(user: User, password: String) {
        try {
            // 1. Crear cuenta en Firebase Authentication
            val result = auth
                .createUserWithEmailAndPassword(user.email, password)
                .await()

            val uid = result.user?.uid
                ?: throw Exception("Error al obtener el UID del usuario creado.")

            // 2. Construir el perfil sin contraseña, usando el UID de Auth como ID
            val userConUid = user.copy(
                id       = uid,
                // La contraseña NO se almacena: Firebase Auth la gestiona de forma segura
            )

            // 3. Guardar el perfil en Firestore bajo "users/{uid}"
            collection
                .document(uid)
                .set(userConUid)
                .await()

            // 4. Enviar correo de verificación al email recién registrado
            // El contenido y diseño del correo se configura en la consola de Firebase,
            // no aquí. Ver sección de explicación al final de este archivo.
            result.user
                ?.sendEmailVerification()
                ?.await()

        } catch (e: FirebaseAuthException) {
            // Error nativo de Firebase Auth (email en uso, contraseña débil, etc.)
            throw Exception(e.toUserMessage())
        } catch (e: FirebaseFirestoreException) {
            // Error al guardar el perfil en Firestore
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            // Error genérico de red u otro inesperado
            throw Exception("Error al registrar el usuario: ${e.message}")
        }
    }

    /**
     * Autentica al usuario con email y contraseña usando Firebase Auth.
     *
     * Flujo detallado:
     * 1. [FirebaseAuth.signInWithEmailAndPassword]: verifica las credenciales.
     * 2. Verifica que el usuario haya confirmado su correo electrónico.
     * 3. Si NO está verificado: cierra sesión y lanza excepción específica.
     * 4. Si está verificado: recupera el perfil desde Firestore y lo retorna.
     *
     * La verificación de email es obligatoria por seguridad: evita que alguien
     * registre un email ajeno y lo use sin el consentimiento del dueño real.
     *
     * @return El [User] con su perfil de Firestore.
     * @throws Exception si las credenciales son incorrectas, el correo no está
     *                   verificado, o falla la red.
     */
    override suspend fun login(email: String, password: String): User? {
        try {
            // 1. Autenticar con Firebase Auth
            val result = auth
                .signInWithEmailAndPassword(email, password)
                .await()

            val uid = result.user?.uid
                ?: throw Exception("Error al obtener el UID del usuario autenticado.")

            // 2. Verificar que el correo esté confirmado
            val emailVerificado = auth.currentUser?.isEmailVerified == true
            if (!emailVerificado) {
                // Forzar cierre de sesión inmediatamente por seguridad
                auth.signOut()
                throw Exception(
                    "Debes verificar tu correo electrónico antes de iniciar sesión. " +
                            "Revisa tu bandeja de entrada."
                )
            }

            // 3. Recuperar el perfil del usuario desde Firestore
            return findById(uid)

        } catch (e: FirebaseAuthException) {
            throw Exception(e.toUserMessage())
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            // Re-lanza la excepción de verificación de email sin modificar el mensaje
            throw e
        }
    }

    /**
     * Cierra la sesión del usuario actual en Firebase Auth.
     *
     * [FirebaseAuth.signOut] es siempre exitoso localmente; no requiere
     * conexión a internet. El [observeCurrentUser] emitirá null automáticamente
     * tras esta llamada, lo que el NavGraph usa para redirigir al login.
     */
    override suspend fun logout() {
        auth.signOut()
    }

    /**
     * Envía un correo de restablecimiento de contraseña al email indicado.
     *
     * Firebase envía un enlace seguro (gestionado por Google) que abre
     * una página web para ingresar la nueva contraseña. No hay ninguna
     * pantalla dentro de la app para completar este proceso.
     *
     * El contenido y diseño del correo se configura en:
     * Consola Firebase → Authentication → Templates → Password reset
     *
     * @param email Correo del usuario que olvidó su contraseña.
     * @throws Exception si el email no está registrado o falla la red.
     */
    override suspend fun sendPasswordResetEmail(email: String) {
        try {
            auth
                .sendPasswordResetEmail(email)
                .await()
        } catch (e: FirebaseAuthException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al enviar el correo de recuperación: ${e.message}")
        }
    }

    // =============================
    // Perfil en Firestore
    // =============================

    /**
     * Busca un usuario por su UID en Firestore.
     * Aprovecha la caché local si no hay conexión.
     *
     * @return El [User] encontrado, o null si no existe el documento.
     */
    override suspend fun findById(id: String): User? {
        return try {
            val doc = collection.document(id).get().await()
            doc.toObject(User::class.java)?.apply { this.id = doc.id }
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al buscar el usuario: ${e.message}")
        }
    }

    /**
     * Busca un usuario por su email en Firestore.
     *
     * @return El [User] encontrado, o null si no existe.
     */
    override suspend fun findByEmail(email: String): User? {
        return try {
            val snapshot = collection
                .whereEqualTo("email", email)
                .get()
                .await()
            val doc = snapshot.documents.firstOrNull() ?: return null
            doc.toObject(User::class.java)?.apply { id = doc.id }
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al buscar el usuario por email: ${e.message}")
        }
    }

    /**
     * Recupera múltiples usuarios por sus IDs.
     * Consulta individual para aprovechar la caché local de Firestore.
     */
    override suspend fun getUsersByIds(ids: List<String>): List<User> {
        return ids.mapNotNull { findById(it) }
    }

    /**
     * Reemplaza todos los campos del documento del usuario en Firestore.
     *
     * @throws Exception si falla la escritura.
     */
    override suspend fun update(user: User) {
        try {
            collection.document(user.id).set(user).await()
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al actualizar el usuario: ${e.message}")
        }
    }

    /**
     * Elimina el documento del usuario en Firestore.
     *
     * Nota: en Fase 3 se debe eliminar también la cuenta en Firebase Auth
     * usando [auth.currentUser?.delete()], lo que requiere re-autenticación
     * reciente del usuario por políticas de seguridad de Firebase.
     *
     * @throws Exception si falla la eliminación.
     */
    override suspend fun delete(id: String) {
        try {
            collection.document(id).delete().await()
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al eliminar el usuario: ${e.message}")
        }
    }

    // =============================
    // Reputación
    // =============================

    /**
     * Suma [points] al contador de reputación de forma atómica.
     * [FieldValue.increment] evita condiciones de carrera sin leer el documento.
     */
    override suspend fun addPoints(userId: String, points: Int) {
        try {
            collection.document(userId)
                .update("reputation.points", FieldValue.increment(points.toLong()))
                .await()
            // Recalcula el nivel tras el incremento
            updateLevel(userId)
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al agregar puntos: ${e.message}")
        }
    }

    /**
     * Lee los puntos actuales y actualiza el nivel del usuario.
     * Actualiza solo el campo "reputation.level" para minimizar escrituras.
     */
    override suspend fun updateLevel(userId: String) {
        try {
            val user = findById(userId) ?: return
            val nuevoNivel = calcularNivel(user.reputation.points)
            collection.document(userId)
                .update("reputation.level", nuevoNivel.name)
                .await()
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al actualizar el nivel: ${e.message}")
        }
    }

    /**
     * Agrega una insignia de forma atómica usando [FieldValue.arrayUnion].
     * No agrega duplicados sin necesidad de leer el documento primero.
     */
    override suspend fun addBadge(userId: String, badge: Badge) {
        try {
            collection.document(userId)
                .update("reputation.badges", FieldValue.arrayUnion(badge))
                .await()
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al agregar la insignia: ${e.message}")
        }
    }

    // =============================
    // Roles
    // =============================

    /** Retorna todos los usuarios con rol [UserRole.MODERATOR]. */
    override suspend fun getModerators(): List<User> {
        return try {
            val snapshot = collection
                .whereEqualTo("role", UserRole.MODERATOR.name)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.apply { id = doc.id }
            }
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al consultar moderadores: ${e.message}")
        }
    }

    // =============================
    // Intereses
    // =============================

    /**
     * Agrega [eventId] a los intereses del usuario de forma atómica.
     * [FieldValue.arrayUnion] garantiza que no haya duplicados.
     */
    override suspend fun addInterestToUser(userId: String, eventId: String) {
        try {
            collection.document(userId)
                .update("interestedEventIds", FieldValue.arrayUnion(eventId))
                .await()
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al agregar el interés: ${e.message}")
        }
    }

    /**
     * Elimina [eventId] de los intereses del usuario de forma atómica.
     * [FieldValue.arrayRemove] no requiere leer el documento primero.
     */
    override suspend fun removeInterestFromUser(userId: String, eventId: String) {
        try {
            collection.document(userId)
                .update("interestedEventIds", FieldValue.arrayRemove(eventId))
                .await()
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al eliminar el interés: ${e.message}")
        }
    }

    /**
     * Retorna el conjunto de IDs de eventos de interés del usuario.
     * @return Conjunto vacío si el usuario no existe.
     */
    override suspend fun getUserInterestedEventIds(userId: String): Set<String> {
        return findById(userId)?.interestedEventIds?.toSet() ?: emptySet()
    }

    // =============================
    // Categorías favoritas
    // =============================

    /**
     * Reemplaza la lista de categorías favoritas.
     * Almacena los nombres de los enums como Strings para Firestore.
     */
    override suspend fun updateFavoriteCategories(userId: String, categories: List<Category>) {
        try {
            collection.document(userId)
                .update("favoriteCategories", categories.map { it.name })
                .await()
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al actualizar las categorías favoritas: ${e.message}")
        }
    }

    /**
     * Retorna las categorías favoritas del usuario.
     * @return Lista vacía si el usuario no existe.
     */
    override suspend fun getFavoriteCategories(userId: String): List<Category> {
        return findById(userId)?.favoriteCategories ?: emptyList()
    }

    // =============================
    // Helpers privados
    // =============================

    /**
     * Calcula el [UserLevel] según los puntos acumulados.
     * Rangos: 0-99 → ESPECTADOR, 100-299 → PARTICIPANTE,
     *         300-599 → ORGANIZADOR, 600+ → LIDER_COMUNITARIO.
     */
    private fun calcularNivel(points: Int): UserLevel = when {
        points < 100  -> UserLevel.ESPECTADOR
        points < 300  -> UserLevel.PARTICIPANTE
        points < 600  -> UserLevel.ORGANIZADOR
        else          -> UserLevel.LIDER_COMUNITARIO
    }

    // Agregar a UserRepositoryImpl, junto a los demás métodos

    /**
     * Actualiza únicamente el campo "fcmToken" del documento del usuario.
     * Usa [update] en lugar de [set] para no reescribir todo el documento.
     *
     * @param userId   UID del usuario en Firestore.
     * @param fcmToken Token FCM del dispositivo actual.
     */
    override suspend fun updateFcmToken(userId: String, fcmToken: String) {
        try {
            collection.document(userId)
                .update("fcmToken", fcmToken)
                .await()
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al actualizar el token FCM: ${e.message}")
        }
    }
}