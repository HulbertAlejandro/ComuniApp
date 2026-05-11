package com.miempresa.comuniapp.domain.model

data class Event(
    val id: String,
    val title: String,
    val description: String,
    val category: Category,
    val imageUris: List<String> = emptyList(),
    val eventLocation: EventLocation,
    val startDate: String,
    val endDate: String,
    val maxAttendees: Int? = null,
    val currentAttendees: Int = 0,
    val ownerId: String,
    val organizerName: String,
    val eventStatus: EventStatus = EventStatus.CREATED,
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING,
    val rejectionReason: String? = null,
    val moderationDate: String? = null,
    val interestCount: Int = 0,
    val commentsCount: Int = 0,
)