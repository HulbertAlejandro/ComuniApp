package com.miempresa.comuniapp.domain.repository

import com.miempresa.comuniapp.domain.model.Attendance
import com.miempresa.comuniapp.domain.model.AttendanceStatus
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
}