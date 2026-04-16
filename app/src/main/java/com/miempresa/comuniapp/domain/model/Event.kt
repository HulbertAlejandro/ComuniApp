package com.miempresa.comuniapp.domain.model

data class Event(
    val id: String,
    val title: String,
    val description: String,

    // Categoría
    val category: Category,

    val imageUrl: String,

    // Ubicación obligatoria
    val location: Location,

    // Fechas
    val startDate: String,
    val endDate: String,

    // Cupo
    val maxAttendees: Int? = null,
    val currentAttendees: Int = 0,

    // Relación con usuario creador
    val ownerId: String,
    val organizerName: String,

    // Estados del evento
    val eventStatus: EventStatus = EventStatus.CREATED,

    // Moderación
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING,
    val rejectionReason: String? = null,
    val moderationDate: String? = null,

    // Métricas sociales
    val interestCount: Int = 0,
    val commentsCount: Int = 0,


)


