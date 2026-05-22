package com.miempresa.comuniapp.domain.model

/**
 * Representa a un usuario registrado en la aplicación.
 *
 * Con Firebase Authentication integrado:
 * - El [id] corresponde al UID generado por Firebase Auth.
 * - La contraseña NO se almacena aquí ni en Firestore; Firebase Auth la gestiona.
 * - Todos los campos tienen valores por defecto para el constructor vacío de Firestore.
 *
 * @param id                 UID de Firebase Auth (asignado al registrar).
 * @param name               Nombre visible del usuario.
 * @param email              Correo electrónico (sincronizado con Firebase Auth).
 * @param phoneNumber        Número de teléfono opcional.
 * @param profilePictureUrl  URL de la foto de perfil.
 * @param direction          Dirección o barrio del usuario.
 * @param role               Rol dentro de la aplicación ([UserRole]).
 * @param reputation         Sistema de puntos, nivel e insignias.
 * @param interestedEventIds IDs de eventos marcados como "me interesa".
 * @param favoriteCategories Categorías favoritas seleccionadas por el usuario.
 *
 * El campo [fcmToken] se actualiza automáticamente al iniciar sesión
 * y cuando Firebase renueva el token del dispositivo. Es el identificador
 * que el servidor usa para enviar notificaciones push a este dispositivo.
 *
 * @param fcmToken Token de Firebase Cloud Messaging del dispositivo actual.
 *                 Vacío si el usuario nunca inició sesión en este dispositivo
 *                 o si revocó los permisos de notificación.
 */
data class User(
    var id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profilePictureUrl: String = "",
    val direction: String = "",
    val role: UserRole = UserRole.USER,
    val reputation: Reputation = Reputation(),
    val interestedEventIds: List<String> = emptyList(),
    val favoriteCategories: List<Category> = emptyList(),
    val fcmToken: String = ""
)