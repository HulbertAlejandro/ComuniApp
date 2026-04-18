package com.miempresa.comuniapp.features.user.savedevents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.repository.CommentRepository
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SavedEventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository, // ✅ Inyectado
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    private val _currentUserId = MutableStateFlow<String?>(null)

    // Mapa de usuarios para mostrar nombres y niveles del organizador
    private val _usersMap = MutableStateFlow<Map<String, User>>(emptyMap())
    val usersMap: StateFlow<Map<String, User>> = _usersMap.asStateFlow()

    val userInterests: StateFlow<Set<String>> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                _currentUserId.value = session.userId
                userRepository.users.map { users ->
                    users.find { it.id == session.userId }?.interestedEventIds ?: emptySet()
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // ✅ REACTIVO: Filtra, carga contadores de comentarios y precarga organizadores
    val savedEvents: StateFlow<List<Event>> = combine(
        userInterests,
        eventRepository.events
    ) { interestedIds, allEvents ->
        allEvents.filter { it.id in interestedIds }
    }
        .flatMapLatest { events -> attachCommentCounts(events) } // ✅ Cargar comentarios
        .onEach { preloadOrganizers(it) } // ✅ Cargar datos de organizadores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ✅ Función para adjuntar el conteo de comentarios (usando el bucle seguro)
    private fun attachCommentCounts(events: List<Event>): Flow<List<Event>> = flow {
        val updatedList = mutableListOf<Event>()
        for (event in events) {
            val comments = commentRepository.getCommentsByEvent(event.id).first()
            updatedList += event.copy(commentsCount = comments.size)
        }
        emit(updatedList)
    }

    private suspend fun preloadOrganizers(events: List<Event>) {
        val userIds = events.mapNotNull { it.ownerId }.distinct()
        if (userIds.isNotEmpty()) {
            val users = userRepository.getUsersByIds(userIds)
            _usersMap.value = _usersMap.value + users.associateBy { it.id }
        }
    }

    fun removeInterest(eventId: String) {
        val userId = _currentUserId.value ?: return
        viewModelScope.launch {
            userRepository.removeInterestFromUser(userId, eventId)
            eventRepository.removeInterest(eventId)
        }
    }
}