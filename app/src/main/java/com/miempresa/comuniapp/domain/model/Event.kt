package com.miempresa.comuniapp.domain.model

data class Event(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val imageUrl: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val startDate: String,
    val endDate: String,
    val maxAttendees: Int?,
    val currentAttendees: Int,
    val organizerName: String,
    val organizerLevel: String,
    val status: String, // ACTIVO, LLENO, FINALIZADO
    val interestCount: Int = 0,
    val commentsCount: Int = 0
)
