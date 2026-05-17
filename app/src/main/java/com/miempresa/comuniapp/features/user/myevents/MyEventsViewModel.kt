package com.miempresa.comuniapp.features.user.myevents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.EventStatus
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.model.VerificationStatus
import com.miempresa.comuniapp.domain.repository.CommentRepository
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de la pantalla "Mis Eventos".
 *
 * Expone cuatro listas reactivas según el estado del evento:
 * - [createdEvents]:  Creados y pendientes de aprobación (PENDING).
 * - [rejectedEvents]: Rechazados por el moderador (REJECTED).
 * - [activeEvents]:   Aprobados y disponibles (ACTIVE o FULL).
 * - [finishedEvents]: Finalizados (FINISHED).
 *
 * Todas las listas adjuntan el conteo de comentarios y pre-cargan
 * los datos de los organizadores para la UI.
 *
 * @param eventRepository   Repositorio de eventos (Firestore).
 * @param userRepository    Repositorio de usuarios (Firestore).
 * @param commentRepository Repositorio de comentarios para contar por evento.
 * @param sessionDataStore  Almacén local de la sesión del usuario autenticado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyEventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    /** ID del usuario en sesión; se actualiza al suscribirse a [userInterests]. */
    private val _currentUserId = MutableStateFlow<String?>(null)

    /** Mapa de organizadores pre-cargados para evitar consultas repetidas desde la UI. */
    private val _usersMap = MutableStateFlow<Map<String, User>>(emptyMap())
    val usersMap: StateFlow<Map<String, User>> = _usersMap.asStateFlow()

    /**
     * Eventos recién creados que están pendientes de aprobación moderación.
     * Solo muestra los que tienen [VerificationStatus.PENDING].
     */
    val createdEvents: StateFlow<List<Event>> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                eventRepository.events.map { events ->
                    events.filter {
                        it.ownerId == session.userId &&
                                it.eventStatus == EventStatus.CREATED &&
                                it.verificationStatus == VerificationStatus.PENDING
                    }.sortedByDescending { it.startDate }
                }
            }
            .flatMapLatest { events -> attachCommentCounts(events) }
            .onEach { preloadOrganizers(it) }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Eventos rechazados por el moderador.
     * Incluye el motivo de rechazo en [Event.rejectionReason].
     */
    val rejectedEvents: StateFlow<List<Event>> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                eventRepository.events.map { events ->
                    events.filter {
                        it.ownerId == session.userId &&
                                it.verificationStatus == VerificationStatus.REJECTED
                    }.sortedByDescending { it.startDate }
                }
            }
            .flatMapLatest { events -> attachCommentCounts(events) }
            .onEach { preloadOrganizers(it) }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Eventos aprobados y disponibles para la comunidad.
     * Incluye tanto los de estado [EventStatus.ACTIVE] como [EventStatus.FULL].
     */
    val activeEvents: StateFlow<List<Event>> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                eventRepository.events.map { events ->
                    events.filter {
                        it.ownerId == session.userId &&
                                it.verificationStatus == VerificationStatus.APPROVED &&
                                (it.eventStatus == EventStatus.ACTIVE ||
                                        it.eventStatus == EventStatus.FULL)
                    }.sortedByDescending { it.startDate }
                }
            }
            .flatMapLatest { events -> attachCommentCounts(events) }
            .onEach { preloadOrganizers(it) }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Eventos finalizados del usuario.
     * Un evento puede marcarse como finalizado manualmente desde esta pantalla.
     */
    val finishedEvents: StateFlow<List<Event>> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                eventRepository.events.map { events ->
                    events.filter {
                        it.ownerId == session.userId &&
                                it.eventStatus == EventStatus.FINISHED
                    }.sortedByDescending { it.startDate }
                }
            }
            .flatMapLatest { events -> attachCommentCounts(events) }
            .onEach { preloadOrganizers(it) }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Conjunto de IDs de eventos marcados como "me interesa" por el usuario actual.
     * Reactivo al documento del usuario en Firestore.
     */
    val userInterests: StateFlow<Set<String>> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                _currentUserId.value = session.userId
                userRepository.users.map { users ->
                    users.find { it.id == session.userId }
                        ?.interestedEventIds
                        ?.toSet()
                        ?: emptySet()
                }
            }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5000),
                initialValue = emptySet()
            )

    /**
     * Alterna el interés del usuario en un evento.
     * Si ya le interesa, lo quita; si no, lo agrega.
     * Actualiza tanto [UserRepository] como [EventRepository] en Firestore.
     *
     * @param eventId ID del evento a alternar.
     */
    fun toggleInterest(eventId: String) {
        val userId = _currentUserId.value ?: return
        viewModelScope.launch {
            if (userInterests.value.contains(eventId)) {
                userRepository.removeInterestFromUser(userId, eventId)
                eventRepository.removeInterest(eventId)
            } else {
                userRepository.addInterestToUser(userId, eventId)
                eventRepository.addInterest(eventId)
            }
        }
    }

    /**
     * Marca un evento como finalizado en Firestore.
     * Solo disponible para eventos en estado [EventStatus.ACTIVE].
     *
     * @param eventId ID del evento a finalizar.
     */
    fun finishEvent(eventId: String) {
        viewModelScope.launch {
            eventRepository.markAsFinished(eventId)
        }
    }

    /**
     * Adjunta el conteo de comentarios a cada evento de la lista.
     * Consulta secuencial con [Flow.first] por ser seguro con [callbackFlow] de Firestore.
     *
     * @param events Lista de eventos a enriquecer con [Event.commentsCount].
     */
    private fun attachCommentCounts(events: List<Event>): Flow<List<Event>> = flow {
        val updatedEvents = events.map { event ->
            val comments = commentRepository.getCommentsByEvent(event.id).first()
            event.copy(commentsCount = comments.size)
        }
        emit(updatedEvents)
    }

    /**
     * Pre-carga los datos de los organizadores de los eventos dados.
     * Evita consultas duplicadas revisando el mapa antes de llamar a Firestore.
     *
     * @param events Lista de eventos cuyos organizadores pre-cargar.
     */
    private suspend fun preloadOrganizers(events: List<Event>) {
        val userIds = events.mapNotNull { it.ownerId }.distinct()
        if (userIds.isNotEmpty()) {
            val users  = userRepository.getUsersByIds(userIds)
            _usersMap.value = _usersMap.value + users.associateBy { it.id }
        }
    }
}