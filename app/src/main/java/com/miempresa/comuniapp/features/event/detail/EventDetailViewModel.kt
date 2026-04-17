package com.miempresa.comuniapp.features.event.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.Attendance
import com.miempresa.comuniapp.domain.model.AttendanceStatus
import com.miempresa.comuniapp.domain.model.Comment
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.EventStatus
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.repository.AttendanceRepository
import com.miempresa.comuniapp.domain.repository.CommentRepository
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val attendanceRepository: AttendanceRepository,
    private val commentRepository: CommentRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    private val _eventId = MutableStateFlow<String?>(null)

    // ✅ Reactivo al repositorio: cualquier cambio en events (asistentes, etc.)
    // se refleja automáticamente en la UI
    val event: StateFlow<Event?> = _eventId
        .filterNotNull()
        .flatMapLatest { id ->
            eventRepository.events.map { list -> list.find { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _organizer = MutableStateFlow<User?>(null)
    val organizer: StateFlow<User?> = _organizer.asStateFlow()

    private val _interestedEventIds = MutableStateFlow<Set<String>>(emptySet())
    val interestedEventIds: StateFlow<Set<String>> = _interestedEventIds.asStateFlow()

    private val _isAttending = MutableStateFlow(false)
    val isAttending: StateFlow<Boolean> = _isAttending.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _commentAuthorsMap = MutableStateFlow<Map<String, User>>(emptyMap())
    val commentAuthorsMap: StateFlow<Map<String, User>> = _commentAuthorsMap.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    val commentsCount: StateFlow<Int> = _comments
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ✅ FIX PRINCIPAL: Jobs individuales para poder cancelarlos antes de relanzar.
    // Así evitamos coroutines duplicadas al cambiar de sesión o reinvocar loadEvent.
    private var sessionJob: Job? = null
    private var commentsJob: Job? = null
    private var organizerJob: Job? = null

    fun loadEvent(eventId: String) {
        // Si ya está cargado el mismo evento, no relanzar nada
        if (_eventId.value == eventId) return
        _eventId.value = eventId

        // ── Coroutine 1: organizador (puntual, no necesita cancelación) ──────
        organizerJob?.cancel()
        organizerJob = viewModelScope.launch {
            val loaded = eventRepository.findById(eventId)
            loaded?.let { _organizer.value = userRepository.findById(it.ownerId) }
        }

        // ── Coroutine 2: sesión reactiva ──────────────────────────────────────
        // Cancelamos la anterior antes de lanzar una nueva para evitar que
        // dos colectores de sesión actualicen _currentUserId al mismo tiempo.
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            sessionDataStore.sessionFlow
                .filterNotNull()
                .collectLatest { session ->
                    _currentUserId.value = session.userId
                    _currentUser.value = userRepository.findById(session.userId)
                    _interestedEventIds.value =
                        userRepository.getUserInterestedEventIds(session.userId)
                    _isAttending.value =
                        attendanceRepository.isUserAttending(eventId, session.userId)
                }
        }

        // ── Coroutine 3: comentarios reactivos ────────────────────────────────
        // Cancelamos la anterior para que no haya dos colectores del mismo Flow
        // sobreescribiendo _comments en paralelo.
        commentsJob?.cancel()
        commentsJob = viewModelScope.launch {
            commentRepository.getCommentsByEvent(eventId).collect { list ->
                _comments.value = list

                // Resolver nombres de autores desconocidos
                val knownIds = _commentAuthorsMap.value.keys
                val newIds = list.map { it.authorId }.distinct().filter { it !in knownIds }
                newIds.forEach { authorId ->
                    val user = userRepository.findById(authorId)
                    if (user != null) {
                        _commentAuthorsMap.value = _commentAuthorsMap.value + (authorId to user)
                    }
                }
            }
        }
    }

    fun postComment(content: String) {
        val eventId = _eventId.value ?: return
        val userId = _currentUserId.value ?: return
        if (content.isBlank()) return

        // Verificación: No permitir comentarios en eventos finalizados
        if (event.value?.eventStatus == EventStatus.FINISHED) return

        viewModelScope.launch {
            val comment = Comment(
                id = UUID.randomUUID().toString(),
                eventId = eventId,
                authorId = userId,
                content = content,
                timestamp = System.currentTimeMillis()
            )
            commentRepository.addComment(comment)
        }
    }

    fun toggleAttendance() {
        val eventId = _eventId.value ?: return
        val userId = _currentUserId.value ?: return
        val currentEvent = event.value ?: return

        viewModelScope.launch {
            if (_isAttending.value) {
                attendanceRepository.removeAttendance(eventId, userId)
                val newCount = maxOf(0, currentEvent.currentAttendees - 1)
                eventRepository.updateAttendeesCount(eventId, newCount)
                _isAttending.value = false
            } else {
                val attendance = Attendance(
                    id = UUID.randomUUID().toString(),
                    eventId = eventId,
                    userId = userId,
                    status = AttendanceStatus.CONFIRMED
                )
                attendanceRepository.confirmAttendance(attendance)
                val newCount = currentEvent.currentAttendees + 1
                eventRepository.updateAttendeesCount(eventId, newCount)
                _isAttending.value = true
            }
        }
    }
}