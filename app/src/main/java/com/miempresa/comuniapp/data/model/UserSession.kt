package com.miempresa.comuniapp.data.model

import com.miempresa.comuniapp.domain.model.UserRole

data class UserSession(
    val userId: String,
    val name: String,
    val role: UserRole
)