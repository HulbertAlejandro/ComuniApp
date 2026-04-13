package com.miempresa.comuniapp.domain.model

data class Attendance(
    val id: String,
    val eventId: String,
    val userId: String,
    val status: AttendanceStatus
)