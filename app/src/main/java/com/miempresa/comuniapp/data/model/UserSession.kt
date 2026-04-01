package com.miempresa.comuniapp.data.model

import com.miempresa.comuniapp.domain.model.UserRole

data class UserSession(
    val userId: String,
    val role: UserRole
)