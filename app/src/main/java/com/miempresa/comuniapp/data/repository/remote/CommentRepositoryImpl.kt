package com.miempresa.comuniapp.data.repository.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.miempresa.comuniapp.core.utils.toUserMessage
import com.miempresa.comuniapp.domain.model.Comment
import com.miempresa.comuniapp.domain.repository.CommentRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CommentRepository {

    private val collection = firestore.collection("comments")

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())

    override val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    init {

        collection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) return@addSnapshotListener

                snapshot?.let {

                    _comments.value = it.documents.mapNotNull { doc ->

                        doc.toObject(Comment::class.java)?.apply {
                            id = doc.id
                        }
                    }
                }
            }
    }

    override suspend fun addComment(comment: Comment) {

        try {

            val docRef = collection.document()

            val finalComment = comment.copy(
                id = docRef.id
            )

            docRef.set(finalComment).await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al agregar comentario: ${e.message}")
        }
    }

    override suspend fun getCommentsByEvent(
        eventId: String
    ): Flow<List<Comment>> = callbackFlow {

        val listener = collection
            .whereEqualTo("eventId", eventId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) return@addSnapshotListener

                val comments = snapshot?.documents?.mapNotNull { doc ->

                    doc.toObject(Comment::class.java)?.apply {
                        id = doc.id
                    }

                } ?: emptyList()

                trySend(comments)
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun deleteComment(commentId: String) {

        try {

            collection
                .document(commentId)
                .delete()
                .await()

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception("Error al eliminar comentario: ${e.message}")
        }
    }

    override fun getTotalCommentsCount(
        eventId: String
    ): Flow<Int> {

        return _comments.map { list ->

            list.count {
                it.eventId == eventId
            }
        }
    }
}