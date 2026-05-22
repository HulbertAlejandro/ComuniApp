package com.miempresa.comuniapp.data.repository.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.miempresa.comuniapp.core.utils.toUserMessage
import com.miempresa.comuniapp.domain.model.Comment
import com.miempresa.comuniapp.domain.repository.CommentRepository
import com.miempresa.comuniapp.domain.repository.EventRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [CommentRepository] con Firestore.
 *
 * ── Arquitectura de listeners ────────────────────────────────────────────────
 * Se mantienen dos tipos de listeners:
 *
 * 1. [_comments] (StateFlow global): listener sin filtro para toda la colección.
 *    Usado por [EventListViewModel.commentCountsByEvent] para calcular el conteo
 *    de comentarios por evento en el feed. Sin este listener, commentCountsByEvent
 *    siempre emite emptyMap() porque el StateFlow nunca recibe datos.
 *
 * 2. [getCommentsByEvent] (callbackFlow por eventId): listener filtrado por evento.
 *    Usado por [EventDetailViewModel.comments] para mostrar la lista reactiva
 *    en el detalle. El listener se crea y destruye junto al colector.
 *
 * ── Por qué getCommentsByEvent NO es suspend ─────────────────────────────────
 * flatMapLatest espera (T) -> Flow<R>, no suspend (T) -> Flow<R>.
 * Si el método es suspend, el compilador lo trata como una función que retorna
 * un Flow ya materializado. El callbackFlow interno se crea correctamente pero
 * su scope se cancela inmediatamente cuando la coroutine suspendida retorna,
 * dejando al listener de Firestore sin scope activo. Solo llega el primer
 * snapshot; los siguientes no tienen listener que los reciba.
 */
@Singleton
class CommentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val eventRepository: EventRepository
) : CommentRepository {

    private val collection = firestore.collection("comments")

    /**
     * StateFlow global de todos los comentarios.
     * El addSnapshotListener se activa al crear el repositorio (Singleton)
     * y permanece activo durante toda la vida del proceso.
     */
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    override val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    init {
        /**
         * Listener global sin filtro para alimentar el StateFlow.
         * Este listener NO se filtra por eventId porque EventListViewModel
         * necesita comentarios de todos los eventos para calcular el conteo.
         *
         * En apps con alto volumen de comentarios, este listener puede reemplazarse
         * por un campo `commentsCount` en el documento del evento, actualizado
         * por Cloud Function con FieldValue.increment().
         */
        collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("CommentRepository", "Listener global error: ${error.message}")
                return@addSnapshotListener
            }
            _comments.value = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Comment::class.java)?.apply { id = doc.id }
            } ?: emptyList()
        }
    }

    override suspend fun addComment(comment: Comment) {

        try {

            val docRef = collection.document()

            val finalComment = comment.copy(
                id = docRef.id
            )

            docRef.set(finalComment).await()

            // 🔥 ACTUALIZAR CONTADOR DEL EVENTO
            eventRepository.incrementCommentsCount(
                comment.eventId
            )

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception(
                "Error al agregar comentario: ${e.message}"
            )
        }
    }

    /**
     * Flow reactivo de comentarios filtrado por [eventId].
     *
     * ── Por qué NO es suspend ────────────────────────────────────────────────
     * Ver documentación de clase. NO agregar suspend a este método.
     *
     * ── Ordenamiento ────────────────────────────────────────────────────────
     * Se ordena en memoria en lugar de usar .orderBy() en Firestore para evitar
     * la creación de un índice compuesto obligatorio (eventId + timestamp).
     *
     * ── Ciclo de vida ────────────────────────────────────────────────────────
     * El listener de Firestore se registra cuando flatMapLatest suscribe el Flow
     * y se elimina en awaitClose() cuando flatMapLatest lo cancela (al cambiar
     * de eventId o al destruirse el ViewModel).
     */
    override fun getCommentsByEvent(eventId: String): Flow<List<Comment>> = callbackFlow {
        Log.d("CommentRepository", "Iniciando listener para eventId: $eventId")

        val listener = collection
            .whereEqualTo("eventId", eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CommentRepository", "Error en listener de comentarios: ${error.message}")
                    // Emitir lista vacía en lugar de cerrar el Flow para mantener
                    // el colector activo y poder recuperarse cuando la conexión vuelva
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Comment::class.java)?.apply { id = doc.id }
                } ?: emptyList()

                trySend(list.sortedBy { it.timestamp })
            }

        awaitClose {
            Log.d("CommentRepository", "Listener eliminado para eventId: $eventId")
            listener.remove()
        }
    }

    override suspend fun deleteComment(commentId: String) {

        try {

            val snapshot =
                collection.document(commentId)
                    .get()
                    .await()

            val comment =
                snapshot.toObject(Comment::class.java)

            collection.document(commentId)
                .delete()
                .await()

            comment?.eventId?.let { eventId ->

                eventRepository.decrementCommentsCount(
                    eventId
                )
            }

        } catch (e: FirebaseFirestoreException) {

            throw Exception(e.toUserMessage())

        } catch (e: Exception) {

            throw Exception(
                "Error al eliminar comentario: ${e.message}"
            )
        }
    }

    override fun getTotalCommentsCount(eventId: String): Flow<Int> = callbackFlow {
        val listener = collection
            .whereEqualTo("eventId", eventId)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }
}