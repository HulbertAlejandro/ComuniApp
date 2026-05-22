package com.miempresa.comuniapp.domain.model

import com.google.firebase.firestore.PropertyName

/**
 * Representa una notificación push recibida por el usuario.
 *
 * Se persiste en Firestore bajo la colección "notifications"
 * filtrada por [userId], lo que permite mostrar el historial
 * en la pantalla de notificaciones de la app.
 *
 * Todos los campos tienen valores por defecto para satisfacer
 * el constructor vacío que exige Firestore al deserializar.
 *
 * @param id             Identificador único asignado por Firestore.
 * @param userId         ID del usuario destinatario de la notificación.
 * @param title          Título de la notificación (ej. "Evento aprobado").
 * @param body           Cuerpo del mensaje (ej. "Tu evento fue aprobado.").
 * @param timestamp      Momento de recepción en milisegundos Unix.
 * @param isRead         true si el usuario ya la visualizó dentro de la app.
 * @param type           Tipo de notificación para enrutar la navegación.
 * @param relatedEventId ID del evento relacionado (opcional, para navegar al detalle).
 */
data class NotificationItem(
    var id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val timestamp: Long = 0L,

    // ✅ Cambiado de 'val' a 'var' para que sea mutable y permita la anotación @set:
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,

    val type: NotificationType = NotificationType.GENERAL,
    val relatedEventId: String = ""
)