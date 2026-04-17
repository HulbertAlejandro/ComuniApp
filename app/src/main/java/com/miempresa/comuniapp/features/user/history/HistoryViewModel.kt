package com.miempresa.comuniapp.features.user.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.EventStatus
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.repository.AttendanceRepository
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.CommentRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val eventRepository: EventRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    private val _currentUserId = MutableStateFlow<String?>(null)

    // Mapa para almacenar los datos de los organizadores (incluyendo nivel)
    private val _usersMap = MutableStateFlow<Map<String, User>>(emptyMap())
    val usersMap: StateFlow<Map<String, User>> = _usersMap.asStateFlow()

    val historyEvents: StateFlow<List<Event>> = combine(
        sessionDataStore.sessionFlow.filterNotNull(),
        attendanceRepository.attendances,
        eventRepository.events,
        commentRepository.comments
    ) { session, attendances, events, allComments ->
        _currentUserId.value = session.userId

        val attendedEventIds = attendances
            .filter { it.userId == session.userId }
            .map { it.eventId }
            .toSet()

        val filteredEvents = events.filter {
            it.id in attendedEventIds && it.eventStatus == EventStatus.FINISHED
        }.map { event ->
            val cCount = allComments.count { it.eventId == event.id }
            event.copy(commentsCount = cCount)
        }.sortedByDescending { it.startDate }

        // Cargamos los datos de los dueños de estos eventos
        preloadOrganizers(filteredEvents)

        filteredEvents
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private suspend fun preloadOrganizers(events: List<Event>) {
        val userIds = events.map { it.ownerId }.distinct()
        if (userIds.isNotEmpty()) {
            val users = userRepository.getUsersByIds(userIds)
            val newMap = users.associateBy { it.id }
            _usersMap.update { it + newMap }
        }
    }
}