package com.miempresa.comuniapp.domain.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profilePictureUrl: String = "",
    val direction: String = "",
    val role: UserRole = UserRole.USER,
    val reputation: Reputation = Reputation(),
    val interestedEventIds: Set<String> = emptySet(),
    val favoriteCategories: List<Category> = emptyList()
)