package com.miempresa.comuniapp.data.repository.memory

import com.miempresa.comuniapp.domain.model.*
import com.miempresa.comuniapp.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepositoryImpl @Inject constructor() : AttendanceRepository {

    private val _attendances = MutableStateFlow<List<Attendance>>(emptyList())
    override val attendances: StateFlow<List<Attendance>> = _attendances.asStateFlow()

    override suspend fun confirmAttendance(attendance: Attendance) {
        if (isUserAttending(attendance.eventId, attendance.userId)) return
        _attendances.value += attendance.copy(status = AttendanceStatus.CONFIRMED)
    }

    override suspend fun updateAttendanceStatus(
        eventId: String,
        userId: String,
        status: AttendanceStatus
    ) {
        _attendances.value = _attendances.value.map {
            if (it.eventId == eventId && it.userId == userId) {
                it.copy(status = status)
            } else it
        }
    }

    override suspend fun getAttendanceByEvent(eventId: String): List<Attendance> =
        _attendances.value.filter { it.eventId == eventId }

    override suspend fun getAttendanceByUser(userId: String): List<Attendance> =
        _attendances.value.filter { it.userId == userId }

    override suspend fun isUserAttending(eventId: String, userId: String): Boolean =
        _attendances.value.any { it.eventId == eventId && it.userId == userId }

    override suspend fun removeAttendance(eventId: String, userId: String) {
        _attendances.value =
            _attendances.value.filterNot { it.eventId == eventId && it.userId == userId }
    }
}