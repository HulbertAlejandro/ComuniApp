package com.miempresa.comuniapp.data.repository.memory

import com.miempresa.comuniapp.domain.model.Comment
import com.miempresa.comuniapp.domain.repository.CommentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepositoryImpl @Inject constructor() : CommentRepository {

    private val _allComments = MutableStateFlow<List<Comment>>(emptyList())
    override val comments: StateFlow<List<Comment>> = _allComments.asStateFlow()

    override suspend fun addComment(comment: Comment) {
        _allComments.update { it + comment }
    }

    override suspend fun getCommentsByEvent(eventId: String): Flow<List<Comment>> =
        _allComments.map { it.filter { comment -> comment.eventId == eventId } }

    override suspend fun deleteComment(commentId: String) {
        _allComments.update { it.filterNot { it.id == commentId } }
    }

    // ✅ NUEVO: Devuelve el conteo de comentarios para un evento específico de forma reactiva
    override fun getTotalCommentsCount(eventId: String): Flow<Int> =
        _allComments.map { comments ->
            comments.count { it.eventId == eventId }
        }
}