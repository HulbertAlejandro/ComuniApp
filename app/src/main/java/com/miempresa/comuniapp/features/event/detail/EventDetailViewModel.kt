package com.miempresa.comuniapp.features.event.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.core.notifications.NotificationSender
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.Attendance
import com.miempresa.comuniapp.domain.model.AttendanceStatus
import com.miempresa.comuniapp.domain.model.Comment
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.EventStatus
import com.miempresa.comuniapp.domain.model.NotificationType
import com.miempresa.comuniapp.domain.model.ReputationPoints
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.model.UserRole
import com.miempresa.comuniapp.domain.repository.AttendanceRepository
import com.miempresa.comuniapp.domain.repository.CommentRepository
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel del detalle de un evento.
 *
 * ── Correcciones aplicadas ───────────────────────────────────────────────────
 *
 * 1. [comments]: el flatMapLatest ahora llama a [CommentRepository.getCommentsByEvent]
 *    que ya no es suspend. Esto garantiza que el listener de Firestore permanece
 *    activo mientras el Flow tiene colectores.
 *
 * 2. [isAttending]: usa [AttendanceRepository.observeUserAttendance] que tiene su
 *    propio listener filtrado, eliminando el rebote visual del botón "Asistir".
 *
 * 3. [toggleAttendance]: ya no modifica [_isAttending] manualmente. El listener
 *    reactivo de [observeUserAttendance] actualiza el estado automáticamente
 *    cuando Firestore confirma la escritura.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val eventRepository      : EventRepository,
    private val userRepository       : UserRepository,
    private val attendanceRepository : AttendanceRepository,
    private val commentRepository    : CommentRepository,
    private val notificationSender   : NotificationSender,
    private val sessionDataStore     : SessionDataStore
) : ViewModel() {

    private val _eventId = MutableStateFlow<String?>(null)

    /**
     * Evento actual reactivo al repositorio global.
     * Cualquier cambio en Firestore (asistentes, intereses) se refleja automáticamente.
     */
    val event: StateFlow<Event?> = _eventId
        .filterNotNull()
        .flatMapLatest { id ->
            eventRepository.events.map { list -> list.find { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isAdmin: StateFlow<Boolean> =
        sessionDataStore.sessionFlow
            .map { session -> session?.role == UserRole.MODERATOR }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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

    /**
     * Lista reactiva de comentarios del evento actual.
     *
     * ── Por qué funciona ahora ───────────────────────────────────────────────
     * [CommentRepository.getCommentsByEvent] ya no es suspend, por lo que
     * flatMapLatest puede pasarle el ID y recibir un Flow activo que mantiene
     * su listener de Firestore mientras este StateFlow tiene colectores.
     *
     * Antes (con suspend): el listener se cancelaba al retornar el Flow.
     * Ahora (sin suspend): el listener vive mientras el flatMapLatest está activo.
     */
    val comments: StateFlow<List<Comment>> = _eventId
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { id ->
            commentRepository.getCommentsByEvent(id)  // ← ya no es suspend
        }
        .onEach { listaComentarios ->
            actualizarAutoresMapeados(listaComentarios)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commentsCount: StateFlow<Int> = comments
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var sessionJob   : Job? = null
    private var attendanceJob: Job? = null
    private var organizerJob : Job? = null

    /**
     * Carga el evento e inicia los listeners reactivos.
     *
     * La guarda [if (_eventId.value == eventId) return] evita reiniciar
     * los listeners en recomposiciones del Composable.
     */
    fun loadEvent(eventId: String) {
        if (_eventId.value == eventId) return
        _eventId.value = eventId

        organizerJob?.cancel()
        organizerJob = viewModelScope.launch {
            val loaded = eventRepository.findById(eventId)
            loaded?.let { _organizer.value = userRepository.findById(it.ownerId) }
        }

        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            sessionDataStore.sessionFlow
                .filterNotNull()
                .collectLatest { session ->
                    _currentUserId.value = session.userId
                    _currentUser.value   = userRepository.findById(session.userId)
                    _interestedEventIds.value =
                        userRepository.getUserInterestedEventIds(session.userId)

                    // Cancelar el listener anterior antes de crear el nuevo.
                    // collectLatest ya cancela la coroutine anterior al cambiar la sesión,
                    // pero el attendanceJob interno necesita cancelación explícita
                    // porque vive fuera del scope de collectLatest.
                    attendanceJob?.cancel()
                    attendanceJob = viewModelScope.launch {
                        attendanceRepository
                            .observeUserAttendance(eventId, session.userId)
                            .collect { attending ->
                                _isAttending.value = attending
                            }
                    }
                }
        }
    }

    private fun actualizarAutoresMapeados(list: List<Comment>) {
        viewModelScope.launch {
            val knownIds = _commentAuthorsMap.value.keys
            val newIds   = list.map { it.authorId }.distinct().filter { it !in knownIds }
            newIds.forEach { authorId ->
                val user = userRepository.findById(authorId)
                if (user != null) {
                    _commentAuthorsMap.value = _commentAuthorsMap.value + (authorId to user)
                }
            }
        }
    }

    /**
     * Publica un comentario en el evento actual.
     *
     * El ID se deja vacío porque [CommentRepositoryImpl.addComment] asigna
     * el ID de Firestore en el momento de persistir.
     *
     * La lista de comentarios se actualiza automáticamente vía el listener
     * de [getCommentsByEvent]; no es necesario actualizar estado local.
     */
    fun postComment(content: String) {
        val eventId = _eventId.value       ?: return
        val userId  = _currentUserId.value ?: return
        if (content.isBlank()) return
        if (event.value?.eventStatus == EventStatus.FINISHED) return

        viewModelScope.launch {
            try {
                commentRepository.addComment(
                    Comment(
                        id        = "",
                        eventId   = eventId,
                        authorId  = userId,
                        content   = content.trim(),
                        timestamp = System.currentTimeMillis()
                    )
                )

                val currentEv    = event.value ?: return@launch
                val ownerId      = currentEv.ownerId
                val eventoNombre = currentEv.title

                if (ownerId != userId) {
                    runCatching {
                        userRepository.addPoints(ownerId, ReputationPoints.COMMENT_ADDED)
                        userRepository.updateLevel(ownerId)
                    }.onFailure { e ->
                        Log.e("EventDetail", "Error en reputación por comentario: ${e.message}")
                    }

                    runCatching {
                        val autor = userRepository.findById(userId)
                        notificationSender.enviar(
                            destinatarioId = ownerId,
                            tipo           = NotificationType.NEW_COMMENT,
                            titulo         = "💬 Nuevo comentario",
                            cuerpo         = "${autor?.name ?: "Alguien"} comentó en \"$eventoNombre\".",
                            relatedEventId = eventId
                        )
                    }.onFailure { e ->
                        Log.e("EventDetail", "Error al notificar comentario: ${e.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e("EventDetail", "Error al publicar comentario: ${e.message}")
            }
        }
    }

    /**
     * Alterna la asistencia del usuario al evento.
     *
     * ── Por qué NO modificamos _isAttending manualmente ──────────────────────
     * [observeUserAttendance] tiene un listener de Firestore que actualiza
     * [_isAttending] automáticamente cuando la escritura se confirma.
     * Modificar [_isAttending] antes de que Firestore confirme (optimistic update)
     * causaría el rebote visual si la escritura falla o tarda.
     *
     * ── Notificación por hito de asistentes ─────────────────────────────────
     * Al confirmar asistencia, si el nuevo total es múltiplo de 10, se envía
     * una notificación push al organizador.
     */
    fun toggleAttendance() {

        val eventId      = _eventId.value       ?: return
        val userId       = _currentUserId.value ?: return
        val currentEvent = event.value          ?: return

        viewModelScope.launch {

            try {

                if (_isAttending.value) {

                    // ─────────────────────────────
                    // CANCELAR ASISTENCIA
                    // ─────────────────────────────

                    attendanceRepository.removeAttendance(
                        eventId,
                        userId
                    )

                    // IMPORTANTE:
                    // ahora enviamos -1, NO el total
                    eventRepository.updateAttendeesCount(
                        eventId,
                        -1
                    )

                    eventRepository.updateEventStatus(eventId)

                } else {

                    // Evitar cupo excedido
                    val isFull =
                        currentEvent.maxAttendees != null &&
                                currentEvent.currentAttendees >= currentEvent.maxAttendees

                    if (isFull) {
                        Log.w(
                            "EventDetail",
                            "Evento lleno"
                        )
                        return@launch
                    }

                    // ─────────────────────────────
                    // CONFIRMAR ASISTENCIA
                    // ─────────────────────────────

                    attendanceRepository.confirmAttendance(
                        Attendance(
                            id      = "",
                            eventId = eventId,
                            userId  = userId,
                            status  = AttendanceStatus.CONFIRMED
                        )
                    )

                    // IMPORTANTE:
                    // ahora enviamos +1, NO el total
                    eventRepository.updateAttendeesCount(
                        eventId,
                        1
                    )

                    eventRepository.updateEventStatus(eventId)

                    // ─────────────────────────────
                    // NOTIFICACIÓN CADA 10
                    // ─────────────────────────────

                    val updatedEvent =
                        eventRepository.findById(eventId)
                            ?: return@launch

                    val attendeesCount =
                        updatedEvent.currentAttendees

                    val ownerId =
                        updatedEvent.ownerId

                    if (
                        ownerId != userId &&
                        attendeesCount > 0 &&
                        attendeesCount % 10 == 0
                    ) {

                        runCatching {

                            notificationSender.enviar(
                                destinatarioId = ownerId,
                                tipo           = NotificationType.NEW_PARTICIPANT,
                                titulo         = "🚀 ¡Hito alcanzado!",
                                cuerpo         =
                                    "\"${updatedEvent.title}\" ya tiene $attendeesCount asistentes confirmados.",
                                relatedEventId = eventId
                            )

                        }.onFailure { e ->

                            Log.e(
                                "EventDetail",
                                "Error al notificar hito: ${e.message}"
                            )
                        }
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    "EventDetail",
                    "Error al cambiar asistencia: ${e.message}"
                )
            }
        }
    }
}