package com.miempresa.comuniapp.domain.model

/**
 * Insignia otorgada a un usuario por logros específicos.
 *
 * @param id          Identificador único de la insignia.
 * @param name        Nombre descriptivo.
 * @param description Detalle del logro que la otorgó.
 * @param achievedAt  Timestamp Unix (milisegundos) del momento en que se obtuvo.
 */
data class Badge(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val achievedAt: Long = 0L
)