package com.miempresa.comuniapp.domain.model

data class Event(
    val id: String,
    val title: String,
    val description: String,

    val category: String,
    val imageUrl: String,

    // Ubicación unificada
    val location: Location,

    // Fechas
    val startDate: String,
    val endDate: String,

    // Asistentes
    val maxAttendees: Int?,
    val currentAttendees: Int,

    // Organizador
    val organizerName: String,
    val organizerLevel: String,

    // Estados separados
    val eventStatus: EventStatus,
    val verificationStatus: VerificationStatus,

    // Interacción social
    val interestCount: Int = 0,
    val commentsCount: Int = 0,

    val ownerId: String
)