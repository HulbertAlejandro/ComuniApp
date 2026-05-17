package com.miempresa.comuniapp.domain.model

/**
 * Información de reputación de un usuario.
 * Firestore requiere constructor vacío, de ahí los valores por defecto.
 *
 * @param points Puntos acumulados.
 * @param level  Nivel calculado a partir de los puntos ([UserLevel]).
 * @param badges Lista de insignias obtenidas.
 */
data class Reputation(
    val points: Int = 0,
    val level: UserLevel = UserLevel.ESPECTADOR,
    val badges: List<Badge> = emptyList()
)