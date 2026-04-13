package com.miempresa.comuniapp.data.repository.memory

import com.miempresa.comuniapp.domain.model.Comment
import com.miempresa.comuniapp.domain.repository.CommentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepositoryImpl @Inject constructor() : CommentRepository {

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    override val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    override suspend fun addComment(comment: Comment) {
        _comments.value += comment
    }

    override suspend fun getCommentsByEvent(eventId: String): List<Comment> =
        _comments.value.filter { it.eventId == eventId }

    override suspend fun deleteComment(commentId: String) {
        _comments.value = _comments.value.filterNot { it.id == commentId }
    }
}