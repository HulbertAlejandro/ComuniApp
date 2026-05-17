package com.miempresa.comuniapp.domain.model

/**
 * Comentario publicado por un usuario en un evento.
 *
 * @param id        Identificador único (asignado por Firestore).
 * @param eventId   ID del evento al que pertenece el comentario.
 * @param authorId  ID del usuario que escribió el comentario.
 * @param content   Texto del comentario.
 * @param timestamp Momento de publicación en milisegundos Unix.
 */
data class Comment(
    var id: String = "",
    val eventId: String = "",
    val authorId: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)