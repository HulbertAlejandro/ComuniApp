package com.miempresa.comuniapp.domain.repository

import com.miempresa.comuniapp.domain.model.Badge
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de acceso a datos de usuarios.
 *
 * Con Firebase Authentication integrado, esta interfaz separa
 * claramente las responsabilidades:
 *
 * - [FirebaseAuth] gestiona identidad: credenciales, sesión, verificación.
 * - [FirebaseFirestore] gestiona perfil: nombre, dirección, reputación, etc.
 *
 * Todas las operaciones de escritura lanzan [Exception] con mensaje
 * legible en español si fallan, listo para mostrar en la UI.
 */
interface UserRepository {

    /**
     * Lista reactiva de todos los usuarios desde Firestore.
     * Se actualiza automáticamente con cambios remotos y sirve datos
     * offline gracias a [PersistentCacheSettings].
     */
    val users: StateFlow<List<User>>

    /**
     * Flujo que emite el usuario autenticado actualmente.
     * Emite null cuando no hay sesión activa.
     * NO es suspend porque es un Flow de observación continua.
     */
    fun observeCurrentUser(): Flow<User?>

    // ── Autenticación (Firebase Auth) ────────────────────────────────────

    /**
     * Registra un nuevo usuario en Firebase Auth y guarda su perfil en Firestore.
     *
     * Flujo interno:
     * 1. Crea la cuenta en Firebase Auth con email y contraseña.
     * 2. Usa el UID generado como ID del documento en Firestore.
     * 3. Guarda el perfil sin contraseña en Firestore.
     * 4. Envía el correo de verificación automáticamente.
     *
     * @param user     Datos del perfil del nuevo usuario (sin ID ni contraseña).
     * @param password Contraseña para Firebase Auth (NO se guarda en Firestore).
     * @throws Exception si el email ya está en uso, la contraseña es débil, o falla la red.
     */
    suspend fun save(user: User, password: String)

    /**
     * Autentica al usuario con email y contraseña usando Firebase Auth.
     *
     * Regla crítica: si el usuario no ha verificado su correo, se fuerza
     * el cierre de sesión y se lanza una excepción específica.
     *
     * @return El [User] con su perfil de Firestore, o null si no existe el documento.
     * @throws Exception si las credenciales son incorrectas, el correo no está
     *                   verificado, o falla la red.
     */
    suspend fun login(email: String, password: String): User?

    /**
     * Cierra la sesión del usuario actual en Firebase Auth.
     * No lanza excepciones; [FirebaseAuth.signOut] es siempre exitoso localmente.
     */
    suspend fun logout()

    /**
     * Envía un correo de restablecimiento de contraseña al email indicado.
     *
     * Firebase envía un enlace seguro que abre un flujo externo (web)
     * para cambiar la contraseña. No requiere ninguna pantalla adicional
     * dentro de la app después de esta llamada.
     *
     * @param email Correo del usuario que olvidó su contraseña.
     * @throws Exception si el email no está registrado o falla la red.
     */
    suspend fun sendPasswordResetEmail(email: String)

    // ── Perfil en Firestore ───────────────────────────────────────────────

    /**
     * Busca un usuario por ID de documento en Firestore.
     * @return El [User] encontrado, o null si no existe.
     */
    suspend fun findById(id: String): User?

    /**
     * Busca un usuario por su dirección de email en Firestore.
     * @return El [User] encontrado, o null si no existe.
     */
    suspend fun findByEmail(email: String): User?

    /**
     * Recupera múltiples usuarios por sus IDs.
     * Los IDs no encontrados se omiten silenciosamente.
     */
    suspend fun getUsersByIds(ids: List<String>): List<User>

    /**
     * Reemplaza todos los campos del documento del usuario en Firestore.
     * @throws Exception si falla la escritura.
     */
    suspend fun update(user: User)

    /**
     * Elimina el documento del usuario en Firestore.
     * Nota: la cuenta en Firebase Auth debe eliminarse por separado
     * (requiere re-autenticación reciente, se implementa en Fase 3).
     * @throws Exception si falla la eliminación.
     */
    suspend fun delete(id: String)

    // ── Reputación ───────────────────────────────────────────────────────

    /**
     * Suma [points] al contador de reputación de forma atómica (FieldValue.increment).
     * Recalcula el nivel automáticamente.
     * @throws Exception si falla la escritura.
     */
    suspend fun addPoints(userId: String, points: Int)

    /**
     * Recalcula y persiste el nivel del usuario según sus puntos actuales.
     * @throws Exception si falla la escritura.
     */
    suspend fun updateLevel(userId: String)

    /**
     * Agrega una insignia a la lista del usuario de forma atómica (arrayUnion).
     * No agrega duplicados.
     * @throws Exception si falla la escritura.
     */
    suspend fun addBadge(userId: String, badge: Badge)

    // ── Roles ────────────────────────────────────────────────────────────

    /** Retorna todos los usuarios con rol MODERATOR. */
    suspend fun getModerators(): List<User>

    // ── Intereses ────────────────────────────────────────────────────────

    /**
     * Agrega [eventId] a los intereses del usuario (arrayUnion, atómico).
     * @throws Exception si falla la escritura.
     */
    suspend fun addInterestToUser(userId: String, eventId: String)

    /**
     * Elimina [eventId] de los intereses del usuario (arrayRemove, atómico).
     * @throws Exception si falla la escritura.
     */
    suspend fun removeInterestFromUser(userId: String, eventId: String)

    /**
     * Retorna el conjunto de IDs de eventos de interés del usuario.
     * @return Conjunto vacío si el usuario no existe.
     */
    suspend fun getUserInterestedEventIds(userId: String): Set<String>

    // ── Categorías favoritas ─────────────────────────────────────────────

    /**
     * Reemplaza la lista completa de categorías favoritas del usuario.
     * @throws Exception si falla la escritura.
     */
    suspend fun updateFavoriteCategories(userId: String, categories: List<Category>)

    /**
     * Retorna las categorías favoritas del usuario.
     * @return Lista vacía si el usuario no existe.
     */
    suspend fun getFavoriteCategories(userId: String): List<Category>

    /**
     * Actualiza el token FCM del dispositivo actual en el documento del usuario.
     * Llamado al iniciar sesión y desde [MyFirebaseMessagingService.onNewToken].
     *
     * @param userId   ID del usuario propietario del token.
     * @param fcmToken Nuevo token generado por Firebase Cloud Messaging.
     * @throws Exception si falla la escritura en Firestore.
     */
    suspend fun updateFcmToken(userId: String, fcmToken: String)
}