package com.miempresa.comuniapp.domain.model

/**
 * Representa a un usuario registrado en la aplicación.
 * Todos los campos tienen valores por defecto para satisfacer el constructor
 * vacío que exige Firestore al deserializar documentos.
 *
 * @param id               Identificador único (asignado por Firestore).
 * @param name             Nombre visible del usuario.
 * @param email            Correo electrónico (usado para autenticación).
 * @param phoneNumber      Número de teléfono opcional.
 * @param profilePictureUrl URL de la foto de perfil.
 * @param direction        Dirección o barrio del usuario.
 * @param role             Rol dentro de la aplicación ([UserRole]).
 * @param reputation       Sistema de puntos, nivel y medallas.
 * @param interestedEventIds IDs de eventos marcados como "me interesa".
 * @param favoriteCategories Categorías favoritas seleccionadas por el usuario.
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
    val favoriteCategories: List<Category> = emptyList()
)