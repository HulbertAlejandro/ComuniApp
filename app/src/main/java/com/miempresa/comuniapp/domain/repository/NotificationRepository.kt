package com.miempresa.comuniapp.domain.repository

import com.miempresa.comuniapp.domain.model.NotificationItem
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acceso a datos de notificaciones.
 *
 * Las notificaciones se gestionan en el servidor mediante Cloud Functions y se persisten
 * en Firestore bajo la colección "notifications" filtrada por [userId].
 * El cliente Android solo tiene permisos para consumir, marcar lectura y eliminar historial.
 */
interface NotificationRepository {

    /**
     * Emite en tiempo real la lista de notificaciones del usuario,
     * ordenadas por [NotificationItem.timestamp] descendente (más recientes primero).
     *
     * El listener de Firestore se cancela automáticamente cuando el colector
     * deja de escuchar (ej. al salir de la pantalla de notificaciones).
     *
     * @param userId ID del usuario cuyas notificaciones observar.
     */
    fun getNotifications(userId: String): Flow<List<NotificationItem>>

    /**
     * Marca una notificación como leída actualizando solo el campo [isRead].
     * Llamado al tocar una notificación en la pantalla de historial.
     *
     * @param notificationId ID del documento de la notificación.
     * @throws Exception si falla la escritura.
     */
    suspend fun markAsRead(notificationId: String)

    /**
     * Elimina una notificación del historial del usuario en Firestore.
     * Llamado al deslizar (swipe) una notificación en la pantalla.
     *
     * @param notificationId ID del documento a eliminar.
     * @throws Exception si falla la eliminación.
     */
    suspend fun deleteNotification(notificationId: String)

    /**
     * Retorna el número de notificaciones no leídas del usuario.
     * Útil para mostrar el badge (punto rojo) en el ícono de la campana.
     *
     * NO es suspend porque retorna un Flow de observación continua.
     *
     * @param userId ID del usuario.
     */
    fun getUnreadCount(userId: String): Flow<Int>
}