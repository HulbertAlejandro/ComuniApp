package com.miempresa.comuniapp.domain.model

/**
 * Niveles de reputación que un usuario puede alcanzar según sus puntos.
 * - ESPECTADOR:         0 – 99 puntos.
 * - PARTICIPANTE:     100 – 299 puntos.
 * - ORGANIZADOR:      300 – 599 puntos.
 * - LIDER_COMUNITARIO: 600+ puntos.
 */
enum class UserLevel {
    ESPECTADOR,
    PARTICIPANTE,
    ORGANIZADOR,
    LIDER_COMUNITARIO
}