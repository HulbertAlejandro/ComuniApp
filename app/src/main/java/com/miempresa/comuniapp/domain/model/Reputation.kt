package com.miempresa.comuniapp.domain.model

data class Reputation(
    val points: Int = 0,
    val level: UserLevel = UserLevel.ESPECTADOR,
    val badges: List<Badge> = emptyList()
)