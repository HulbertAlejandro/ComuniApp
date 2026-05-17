package com.miempresa.comuniapp.domain.model

/**
 * Registro de asistencia de un usuario a un evento específico.
 *
 * @param id      Identificador único (asignado por Firestore).
 * @param eventId ID del evento.
 * @param userId  ID del usuario asistente.
 * @param status  Estado actual de la asistencia ([AttendanceStatus]).
 */
data class Attendance(
    var id: String = "",
    val eventId: String = "",
    val userId: String = "",
    val status: AttendanceStatus = AttendanceStatus.PENDING
)