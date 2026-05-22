package com.miempresa.comuniapp.domain.repository

import com.miempresa.comuniapp.domain.model.Attendance
import com.miempresa.comuniapp.domain.model.AttendanceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AttendanceRepository {

    val attendances: StateFlow<List<Attendance>>

    suspend fun confirmAttendance(attendance: Attendance)

    suspend fun updateAttendanceStatus(
        eventId: String,
        userId: String,
        status: AttendanceStatus
    )

    suspend fun getAttendanceByEvent(eventId: String): List<Attendance>

    suspend fun getAttendanceByUser(userId: String): List<Attendance>

    suspend fun isUserAttending(eventId: String, userId: String): Boolean

    suspend fun removeAttendance(eventId: String, userId: String)

    /**
     * Observa en tiempo real si un usuario específico está asistiendo a un evento.
     *
     * NO es suspend porque retorna un Flow de observación continua.
     * Usa su propio listener de Firestore filtrado por eventId + userId
     * para evitar el rebote visual causado por el estado global.
     */
    fun observeUserAttendance(eventId: String, userId: String): Flow<Boolean>
}