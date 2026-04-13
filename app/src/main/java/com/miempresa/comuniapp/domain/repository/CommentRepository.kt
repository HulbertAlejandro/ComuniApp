package com.miempresa.comuniapp.domain.repository

import com.miempresa.comuniapp.domain.model.Comment
import kotlinx.coroutines.flow.StateFlow

interface CommentRepository {

    val comments: StateFlow<List<Comment>>

    suspend fun addComment(comment: Comment)

    suspend fun getCommentsByEvent(eventId: String): List<Comment>

    suspend fun deleteComment(commentId: String)
}