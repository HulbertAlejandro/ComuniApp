package com.miempresa.comuniapp.features.user.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.model.AttendanceStatus
import com.miempresa.comuniapp.domain.repository.AttendanceRepository
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UserRepository,
    private val eventRepository: EventRepository,
    private val attendanceRepository: AttendanceRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    // Observa el repositorio reactivamente: cualquier update() en UserEditViewModel
    // se refleja aquí automáticamente sin necesidad de recargar.
    val user: StateFlow<User?> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                repository.users.map { list -> list.find { it.id == session.userId } }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    // Contadores reactivos
    val createdEventsCount: StateFlow<Int> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                eventRepository.events.map { events ->
                    events.count { it.ownerId == session.userId }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    val attendedEventsCount: StateFlow<Int> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                attendanceRepository.attendances.map { attendances ->
                    attendances.count { it.userId == session.userId && it.status == AttendanceStatus.CONFIRMED }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    val savedEventsCount: StateFlow<Int> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                repository.users.map { users ->
                    users.find { it.id == session.userId }?.interestedEventIds?.size ?: 0
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    val points: StateFlow<Int> =
        user.map { it?.reputation?.points ?: 0 }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    fun logout() {
        viewModelScope.launch {
            sessionDataStore.clearSession()
        }
    }
}