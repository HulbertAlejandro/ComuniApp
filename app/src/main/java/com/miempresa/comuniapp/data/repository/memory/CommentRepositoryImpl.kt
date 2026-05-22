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

    override fun getCommentsByEvent(eventId: String): Flow<List<Comment>> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteComment(commentId: String) {
        TODO("Not yet implemented")
    }

    override fun getTotalCommentsCount(eventId: String): Flow<Int> {
        TODO("Not yet implemented")
    }

}