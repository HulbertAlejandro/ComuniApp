package com.miempresa.comuniapp.features.user.myevents

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.miempresa.comuniapp.core.notifications.NotificationSender
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.EventStatus
import com.miempresa.comuniapp.domain.model.NotificationType
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.model.VerificationStatus
import com.miempresa.comuniapp.domain.repository.CommentRepository
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyEventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository,
    private val notificationSender: NotificationSender,
    private val auth: FirebaseAuth
) : ViewModel() {

    /**
     * UID actual usando FirebaseAuth.
     */
    private val currentUserIdFlow =
        MutableStateFlow(auth.currentUser?.uid)

    /**
     * Cache de organizadores.
     */
    private val _usersMap =
        MutableStateFlow<Map<String, User>>(emptyMap())

    val usersMap: StateFlow<Map<String, User>> =
        _usersMap.asStateFlow()

    /**
     * Eventos creados pendientes.
     */
    val createdEvents: StateFlow<List<Event>> =
        currentUserIdFlow
            .filterNotNull()
            .flatMapLatest { userId ->

                eventRepository.events.map { events ->

                    events.filter {

                        it.ownerId == userId &&
                                it.eventStatus == EventStatus.CREATED &&
                                it.verificationStatus == VerificationStatus.PENDING

                    }.sortedByDescending { it.startDate }
                }
            }
            .flatMapLatest { events ->
                attachCommentCounts(events)
            }
            .onEach {
                preloadOrganizers(it)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

    /**
     * Eventos rechazados.
     */
    val rejectedEvents: StateFlow<List<Event>> =
        currentUserIdFlow
            .filterNotNull()
            .flatMapLatest { userId ->

                eventRepository.events.map { events ->

                    events.filter {

                        it.ownerId == userId &&
                                it.verificationStatus == VerificationStatus.REJECTED

                    }.sortedByDescending { it.startDate }
                }
            }
            .flatMapLatest { events ->
                attachCommentCounts(events)
            }
            .onEach {
                preloadOrganizers(it)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

    /**
     * Eventos activos.
     */
    val activeEvents: StateFlow<List<Event>> =
        currentUserIdFlow
            .filterNotNull()
            .flatMapLatest { userId ->

                eventRepository.events.map { events ->

                    events.filter {

                        it.ownerId == userId &&
                                it.verificationStatus == VerificationStatus.APPROVED &&
                                (
                                        it.eventStatus == EventStatus.ACTIVE ||
                                                it.eventStatus == EventStatus.FULL
                                        )

                    }.sortedByDescending { it.startDate }
                }
            }
            .flatMapLatest { events ->
                attachCommentCounts(events)
            }
            .onEach {
                preloadOrganizers(it)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

    /**
     * Eventos finalizados.
     */
    val finishedEvents: StateFlow<List<Event>> =
        currentUserIdFlow
            .filterNotNull()
            .flatMapLatest { userId ->

                eventRepository.events.map { events ->

                    events.filter {

                        it.ownerId == userId &&
                                it.eventStatus == EventStatus.FINISHED

                    }.sortedByDescending { it.startDate }
                }
            }
            .flatMapLatest { events ->
                attachCommentCounts(events)
            }
            .onEach {
                preloadOrganizers(it)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

    /**
     * Eventos marcados con interés.
     */
    val userInterests: StateFlow<Set<String>> =
        currentUserIdFlow
            .filterNotNull()
            .flatMapLatest { userId ->

                userRepository.users.map { users ->

                    users.find { it.id == userId }
                        ?.interestedEventIds
                        ?.toSet()
                        ?: emptySet()
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptySet()
            )

    /**
     * Toggle interés.
     */
    fun toggleInterest(eventId: String) {

        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {

            if (userInterests.value.contains(eventId)) {

                userRepository.removeInterestFromUser(
                    userId,
                    eventId
                )

                eventRepository.removeInterest(eventId)

            } else {

                userRepository.addInterestToUser(
                    userId,
                    eventId
                )

                eventRepository.addInterest(eventId)
            }
        }
    }

    /**
     * Finalizar evento.
     */
    fun finishEvent(eventId: String) {
        viewModelScope.launch {
            try {
                // 1. Buscamos el evento de forma asíncrona antes de mutarlo para extraer sus datos reales
                val targetEvent = eventRepository.findById(eventId)

                // 2. Marcamos el estado en Firestore como finalizado
                eventRepository.markAsFinished(eventId)

                // 3. Enviamos la notificación si el evento fue encontrado con éxito
                if (targetEvent != null) {
                    runCatching {
                        notificationSender.enviar(
                            destinatarioId = targetEvent.ownerId,
                            tipo           = NotificationType.NEW_COMMENT, // Modifica por tu enum correspondiente si tienes uno específico
                            titulo         = "🏁 Evento finalizado",
                            cuerpo         = "Tu evento \"${targetEvent.title}\" se ha cerrado satisfactoriamente.",
                            relatedEventId = eventId
                        )
                    }.onFailure { e ->
                        Log.e("MyEventsViewModel", "Error al enviar notificación push: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("MyEventsViewModel", "Error general al finalizar el evento: ${e.message}")
            }
        }
    }

    /**
     * Adjunta comentarios.
     */
    private fun attachCommentCounts(
        events: List<Event>
    ): Flow<List<Event>> = flow {

        val updatedEvents = events.map { event ->

            val comments = commentRepository
                .getCommentsByEvent(event.id)
                .first()

            event.copy(
                commentsCount = comments.size
            )
        }

        emit(updatedEvents)
    }

    /**
     * Precarga organizadores.
     */
    private suspend fun preloadOrganizers(
        events: List<Event>
    ) {

        val userIds = events
            .map { it.ownerId }
            .distinct()

        if (userIds.isNotEmpty()) {

            val users = userRepository
                .getUsersByIds(userIds)

            _usersMap.value =
                _usersMap.value + users.associateBy { it.id }
        }
    }
}