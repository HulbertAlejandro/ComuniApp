package com.miempresa.comuniapp.features.user.myevents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.EventStatus
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.model.VerificationStatus
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyEventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val commentRepository: com.miempresa.comuniapp.domain.repository.CommentRepository, // ✅ Inyectado para contar comentarios
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    private val currentSession = sessionDataStore.sessionFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 1. CREADOS: Ahora solo muestra los que están en proceso de verificación (PENDING)
    val createdEvents: StateFlow<List<Event>> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                eventRepository.events.map { events ->
                    events.filter {
                        it.ownerId == session.userId &&
                                it.eventStatus == EventStatus.CREATED &&
                                it.verificationStatus == VerificationStatus.PENDING // ✅ Solo pendientes
                    }.sortedByDescending { it.startDate }
                }
            }
            .flatMapLatest { events -> attachCommentCounts(events) } // ✅ Cargar contadores
            .onEach { preloadOrganizers(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. RECHAZADOS: Nueva sección para eventos con status REJECTED
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
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. ACTIVOS: Solo aprobados
    val activeEvents: StateFlow<List<Event>> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                eventRepository.events.map { events ->
                    events.filter {
                        it.ownerId == session.userId &&
                                it.verificationStatus == VerificationStatus.APPROVED &&
                                (it.eventStatus == EventStatus.ACTIVE || it.eventStatus == EventStatus.FULL)
                    }.sortedByDescending { it.startDate }
                }
            }
            .flatMapLatest { events -> attachCommentCounts(events) }
            .onEach { preloadOrganizers(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. FINALIZADOS: Incluye mapeo de comentarios
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
            .flatMapLatest { events -> attachCommentCounts(events) } // ✅ Corregido el contador
            .onEach { preloadOrganizers(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ✅ Función auxiliar para actualizar el contador de comentarios en la lista
    private fun attachCommentCounts(events: List<Event>): Flow<List<Event>> = flow {
        val updatedEvents = events.map { event ->
            // ✅ .first() obtiene la lista actual de comentarios y permite sacar el .size (Int)
            val comments = commentRepository.getCommentsByEvent(event.id).first()
            event.copy(commentsCount = comments.size)
        }
        emit(updatedEvents)
    }

    // Intereses del usuario actual (para verificar estado)
    private val _currentUserId = MutableStateFlow<String?>(null)
    val userInterests: StateFlow<Set<String>> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                _currentUserId.value = session.userId
                userRepository.users.map { users ->
                    users.find { it.id == session.userId }?.interestedEventIds ?: emptySet()
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptySet()
            )

    // Mapa de usuarios para mostrar organizadores
    private val _usersMap = MutableStateFlow<Map<String, User>>(emptyMap())
    val usersMap: StateFlow<Map<String, User>> = _usersMap.asStateFlow()

    // Función toggle de interés (solo para CREADOS y ACTIVOS)
    fun toggleInterest(eventId: String) {
        val userId = currentSession.value?.userId ?: return

        viewModelScope.launch {
            val isInterested = userInterests.value.contains(eventId)
            if (isInterested) {
                userRepository.removeInterestFromUser(userId, eventId)
                eventRepository.removeInterest(eventId)
            } else {
                userRepository.addInterestToUser(userId, eventId)
                eventRepository.addInterest(eventId)
            }
        }
    }

    // Función para finalizar evento
    fun finishEvent(eventId: String) {
        viewModelScope.launch {
            eventRepository.markAsFinished(eventId)
        }
    }

    // Preload de organizadores para eventos
    private suspend fun preloadOrganizers(events: List<Event>) {
        val userIds = events.mapNotNull { it.ownerId }.distinct()
        if (userIds.isNotEmpty()) {
            val users = userRepository.getUsersByIds(userIds)
            val usersMap = users.associateBy { it.id }
            _usersMap.value = _usersMap.value + usersMap
        }
    }
}
