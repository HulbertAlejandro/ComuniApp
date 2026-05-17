package com.miempresa.comuniapp.domain.model

/**
 * Roles disponibles para los usuarios de la aplicación.
 * - USER: usuario estándar.
 * - MODERATOR: puede aprobar o rechazar eventos.
 */
enum class UserRole {
    USER,
    MODERATOR
}