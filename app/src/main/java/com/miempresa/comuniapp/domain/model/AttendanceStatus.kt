package com.miempresa.comuniapp.domain.model

/**
 * Estado de la asistencia de un usuario a un evento.
 * - PENDING:   registrado pero no confirmado.
 * - CONFIRMED: asistencia confirmada.
 * - CANCELLED: asistencia cancelada.
 */
enum class AttendanceStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}