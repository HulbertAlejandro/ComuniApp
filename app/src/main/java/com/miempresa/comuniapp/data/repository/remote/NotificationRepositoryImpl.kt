package com.miempresa.comuniapp.data.repository.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.miempresa.comuniapp.core.utils.toUserMessage
import com.miempresa.comuniapp.domain.model.NotificationItem
import com.miempresa.comuniapp.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [NotificationRepository] que consulta y gestiona
 * notificaciones en Firebase Firestore en tiempo real.
 *
 * ⚠️ Las operaciones de creación (create) están delegadas exclusivamente a Cloud Functions
 * debido a las reglas de seguridad vigentes en Firestore.
 *
 * Índice compuesto requerido en Firestore:
 * Colección: notifications
 * Campo 1:   userId    → Ascending
 * Campo 2:   timestamp → Descending
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    private val collection = firestore.collection("notifications")

    /**
     * Emite en tiempo real las notificaciones del usuario ordenadas
     * por timestamp descendente desde Firestore.
     */
    override fun getNotifications(userId: String): Flow<List<NotificationItem>> =
        callbackFlow {
            Log.d("NotificationRepo", "Iniciando listener en tiempo real para userId: $userId")

            val listener = collection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        Log.e(
                            "NotificationRepo",
                            "Error en listener Firestore: [${error.code}] ${error.message}"
                        )
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val notificaciones = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(NotificationItem::class.java)?.apply { id = doc.id }
                    } ?: emptyList()

                    Log.d(
                        "NotificationRepo",
                        "Notificaciones actualizadas: ${notificaciones.size} encontradas para userId: $userId"
                    )

                    trySend(notificaciones)
                }

            awaitClose {
                Log.d("NotificationRepo", "Listener en tiempo real destruido para userId: $userId")
                listener.remove()
            }
        }

    /**
     * Actualiza el estado de lectura de la notificación a true.
     */
    override suspend fun markAsRead(notificationId: String) {
        try {
            collection.document(notificationId)
                .update("isRead", true)
                .await()
            Log.d("NotificationRepo", "Notificación $notificationId marcada como leída.")
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al marcar como leída: ${e.message}")
        }
    }

    /**
     * Elimina el documento de la notificación de manera definitiva.
     */
    override suspend fun deleteNotification(notificationId: String) {
        try {
            collection.document(notificationId)
                .delete()
                .await()
            Log.d("NotificationRepo", "Notificación $notificationId eliminada.")
        } catch (e: FirebaseFirestoreException) {
            throw Exception(e.toUserMessage())
        } catch (e: Exception) {
            throw Exception("Error al eliminar la notificación: ${e.message}")
        }
    }

    /**
     * Retorna el conteo dinámico de notificaciones no leídas del usuario.
     */
    override fun getUnreadCount(userId: String): Flow<Int> =
        getNotifications(userId).map { lista -> lista.count { !it.isRead } }
}