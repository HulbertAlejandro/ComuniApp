package com.miempresa.comuniapp.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phoneNumber: String = "",
    val profilePictureUrl: String = "",

    // Ubicación requerida por el sistema
    val location: Location,

    // Rol del sistema
    val role: UserRole = UserRole.USER,

    // Sistema de reputación
    val reputation: Reputation = Reputation(),

    // Intereses del usuario
    val interestedEventIds: Set<String> = emptySet(),

    // Categorías favoritas del usuario
    val favoriteCategories: List<Category> = emptyList()

)