package com.miempresa.comuniapp.domain.model

/**
 * Representa un evento comunitario publicado en la aplicación.
 * Todos los campos tienen valores por defecto para el constructor vacío de Firestore.
 *
 * @param id                 Identificador único (asignado por Firestore).
 * @param title              Título del evento.
 * @param description        Descripción detallada.
 * @param category           Categoría temática ([Category]).
 * @param imageUris          Lista de URLs de imágenes del evento.
 * @param eventLocation      Coordenadas geográficas ([EventLocation]).
 * @param startDate          Fecha y hora de inicio (formato "yyyy-MM-dd HH:mm").
 * @param endDate            Fecha y hora de fin.
 * @param maxAttendees       Cupo máximo (null = sin límite).
 * @param currentAttendees   Asistentes confirmados actualmente.
 * @param ownerId            ID del usuario que creó el evento.
 * @param organizerName      Nombre visible del organizador.
 * @param eventStatus        Estado operativo del evento ([EventStatus]).
 * @param verificationStatus Estado de moderación ([VerificationStatus]).
 * @param rejectionReason    Motivo de rechazo (solo cuando [verificationStatus] = REJECTED).
 * @param moderationDate     Fecha en que fue moderado.
 * @param interestCount      Contador de "me interesa".
 * @param commentsCount      Contador de comentarios.
 */
data class Event(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val category: Category = Category.SOCIAL,
    val imageUris: List<String> = emptyList(),
    val eventLocation: EventLocation = EventLocation(),
    val startDate: String = "",
    val endDate: String = "",
    val maxAttendees: Int? = null,
    val currentAttendees: Int = 0,
    val ownerId: String = "",
    val organizerName: String = "",
    val eventStatus: EventStatus = EventStatus.CREATED,
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING,
    val rejectionReason: String? = null,
    val moderationDate: String? = null,
    val interestCount: Int = 0,
    val commentsCount: Int = 0
)