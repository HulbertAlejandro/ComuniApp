package com.miempresa.comuniapp.domain.repository

import com.miempresa.comuniapp.domain.model.Comment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CommentRepository {

    val comments: StateFlow<List<Comment>>

    suspend fun addComment(comment: Comment)

    suspend fun getCommentsByEvent(eventId: String): Flow<List<Comment>>

    suspend fun deleteComment(commentId: String)

    // ✅ NUEVO: Obtener el conteo de comentarios por evento reactivamente
    fun getTotalCommentsCount(eventId: String): Flow<Int>
}