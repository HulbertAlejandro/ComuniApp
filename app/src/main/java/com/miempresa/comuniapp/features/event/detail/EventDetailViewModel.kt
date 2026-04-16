package com.miempresa.comuniapp.features.event.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.Attendance
import com.miempresa.comuniapp.domain.model.AttendanceStatus
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.repository.AttendanceRepository
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val attendanceRepository: AttendanceRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private val _organizer = MutableStateFlow<User?>(null)
    val organizer: StateFlow<User?> = _organizer.asStateFlow()

    // ✅ Intereses del usuario logueado (para mostrar estado del botón)
    private val _interestedEventIds = MutableStateFlow<Set<String>>(emptySet())
    val interestedEventIds: StateFlow<Set<String>> = _interestedEventIds.asStateFlow()

    // ✅ NUEVO: Indica si el usuario está asistiendo al evento
    private val _isAttending = MutableStateFlow(false)
    val isAttending: StateFlow<Boolean> = _isAttending.asStateFlow()

    // ✅ NUEVO: ID del usuario logueado
    private val _currentUserId = MutableStateFlow<String?>(null)

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            // 1. Cargar el evento
            val loaded = eventRepository.findById(eventId)
            _event.value = loaded

            // 2. Cargar organizador
            loaded?.let {
                _organizer.value = userRepository.findById(it.ownerId)
            }

            // 3. ✅ Cargar intereses e información de asistencia desde el usuario de sesión
            sessionDataStore.sessionFlow
                .filterNotNull()
                .collectLatest { session ->
                    _currentUserId.value = session.userId

                    // Cargar intereses
                    val interestIds = userRepository.getUserInterestedEventIds(session.userId)
                    _interestedEventIds.value = interestIds

                    // ✅ NUEVO: Verificar si el usuario ya está asistiendo
                    _isAttending.value = attendanceRepository.isUserAttending(eventId, session.userId)
                }
        }
    }

    // ✅ NUEVO: Toggle de asistencia (crear/eliminar Attendance)
    fun toggleAttendance() {
        val eventId = _event.value?.id ?: return
        val userId = _currentUserId.value ?: return
        val event = _event.value ?: return

        viewModelScope.launch {
            if (_isAttending.value) {
                // El usuario YA está registrado → eliminar asistencia
                attendanceRepository.removeAttendance(eventId, userId)

                // Disminuir contador de asistentes
                val newCount = maxOf(0, event.currentAttendees - 1)
                eventRepository.updateAttendeesCount(eventId, newCount)

                _isAttending.value = false
            } else {
                // El usuario NO está registrado → crear Attendance
                val attendance = Attendance(
                    id = UUID.randomUUID().toString(),
                    eventId = eventId,
                    userId = userId,
                    status = AttendanceStatus.CONFIRMED
                )
                attendanceRepository.confirmAttendance(attendance)

                // Aumentar contador de asistentes
                val newCount = event.currentAttendees + 1
                eventRepository.updateAttendeesCount(eventId, newCount)

                _isAttending.value = true
            }
        }
    }
}