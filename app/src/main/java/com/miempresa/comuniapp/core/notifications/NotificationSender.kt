package com.miempresa.comuniapp.core.notifications

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.miempresa.comuniapp.core.utils.toUserMessage
import com.miempresa.comuniapp.domain.model.NotificationType
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Componente que solicita el envío de notificaciones push al servidor.
 *
 * ── Arquitectura ─────────────────────────────────────────────────────────────
 * Android escribe en "notification_requests" → Cloud Function lo detecta →
 * guarda en "notifications" → envía push vía FCM Admin SDK.
 *
 * ── Convención de campos ─────────────────────────────────────────────────────
 * Los campos de "notification_requests" usan los MISMOS nombres en inglés
 * que la entidad [NotificationItem] y la Cloud Function. Esto elimina la
 * traducción español→inglés que hacía la versión anterior y evita que un
 * cambio en un lado rompa silenciosamente el otro.
 *
 * Campos de "notification_requests":
 *   userId         → ID del destinatario (coincide con NotificationItem.userId)
 *   title          → Título (coincide con NotificationItem.title)
 *   body           → Cuerpo (coincide con NotificationItem.body)
 *   type           → Tipo enum como String (coincide con NotificationItem.type)
 *   relatedEventId → ID del evento (coincide con NotificationItem.relatedEventId)
 *   timestamp      → Milisegundos Unix
 *   processed      → false; la Cloud Function lo marca true al procesar
 *   extraData      → Datos adicionales para el payload FCM
 */
@Singleton
class NotificationSender @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val requestsCollection = firestore.collection("notification_requests")

    /**
     * Solicita el envío de una notificación push escribiendo en Firestore.
     *
     * La operación es silenciosa: si falla, loguea el error pero no lanza
     * excepción, para no bloquear la acción de negocio que la originó
     * (aprobar evento, publicar comentario, etc.).
     *
     * @param userId         ID del usuario destinatario.
     * @param tipo           Tipo de notificación ([NotificationType]).
     * @param titulo         Título del mensaje push.
     * @param cuerpo         Cuerpo del mensaje push.
     * @param relatedEventId ID del evento relacionado (vacío si no aplica).
     * @param extraData      Datos adicionales para el payload FCM.
     */
    suspend fun enviar(
        destinatarioId: String,
        tipo          : NotificationType,
        titulo        : String,
        cuerpo        : String,
        relatedEventId: String              = "",
        extraData     : Map<String, String> = emptyMap()
    ) {
        try {
            // ✅ Mismos nombres de campo que NotificationItem y la Cloud Function
            val solicitud = mapOf(
                "userId"         to destinatarioId,
                "title"          to titulo,
                "body"           to cuerpo,
                "type"           to tipo.name,
                "relatedEventId" to relatedEventId,
                "timestamp"      to System.currentTimeMillis(),
                "procesado"      to false,
                "extraData"      to extraData
            )
            requestsCollection.add(solicitud).await()
            Log.d("NotificationSender", "Solicitud creada para: $destinatarioId tipo: ${tipo.name}")

        } catch (e: FirebaseFirestoreException) {
            Log.e("NotificationSender", "Error Firestore: ${e.toUserMessage()}")
        } catch (e: Exception) {
            Log.e("NotificationSender", "Error inesperado: ${e.message}")
        }
    }
}