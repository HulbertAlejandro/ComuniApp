package com.miempresa.comuniapp.domain.repository

import com.miempresa.comuniapp.domain.model.Comment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CommentRepository {

    /**
     * StateFlow global de todos los comentarios.
     * Alimentado por un addSnapshotListener activo en el repositorio.
     * Usado por EventListViewModel para calcular commentCountsByEvent.
     */
    val comments: StateFlow<List<Comment>>

    suspend fun addComment(comment: Comment)

    /**
     * Retorna un Flow reactivo de comentarios para un evento específico.
     *
     * IMPORTANTE: NO es suspend. Un método que retorna Flow nunca debe ser suspend
     * porque el compilador trata el Flow retornado como ya materializado,
     * lo que cancela el listener interno inmediatamente después de la primera emisión.
     */
    fun getCommentsByEvent(eventId: String): Flow<List<Comment>>

    suspend fun deleteComment(commentId: String)

    fun getTotalCommentsCount(eventId: String): Flow<Int>
}