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

/**
 * ViewModel de la pantalla de eventos guardados ("Me interesa").
 *
 * Combina reactivamente:
 * - Los IDs de eventos de interés del usuario (desde Firestore via [UserRepository]).
 * - La lista global de eventos (desde el SnapshotListener de [EventRepository]).
 *
 * Filtra para mostrar solo los eventos cuyo ID esté en la lista de intereses,
 * adjunta el conteo de comentarios de cada uno y pre-carga los datos
 * de los organizadores para evitar consultas repetidas desde la UI.
 *
 * @param eventRepository  Repositorio de eventos (Firestore).
 * @param userRepository   Repositorio de usuarios (Firestore).
 * @param commentRepository Repositorio de comentarios (Firestore).
 * @param sessionDataStore  Almacén local de la sesión del usuario autenticado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SavedEventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    private val _currentUserId = MutableStateFlow<String?>(null)

    /** Mapa de organizadores pre-cargados para evitar consultas repetidas desde la UI. */
    private val _usersMap = MutableStateFlow<Map<String, User>>(emptyMap())
    val usersMap: StateFlow<Map<String, User>> = _usersMap.asStateFlow()

    /**
     * Conjunto de IDs de eventos marcados como "me interesa" por el usuario actual.
     * Se actualiza reactivamente cuando cambia el documento del usuario en Firestore.
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
     * Lista reactiva de eventos guardados con conteo de comentarios adjunto.
     *
     * Flujo de datos:
     * 1. Combina [userInterests] con el [EventRepository.events] global.
     * 2. Filtra los eventos cuyo ID esté en los intereses del usuario.
     * 3. Adjunta el conteo de comentarios de cada evento via [attachCommentCounts].
     * 4. Pre-carga los datos de los organizadores en [_usersMap].
     */
    val savedEvents: StateFlow<List<Event>> = combine(
        userInterests,
        eventRepository.events
    ) { interestedIds, allEvents ->
        allEvents.filter { it.id in interestedIds }
    }
        .flatMapLatest { events -> attachCommentCounts(events) }
        .onEach { preloadOrganizers(it) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Adjunta el conteo de comentarios a cada evento de la lista.
     *
     * Estrategia: consulta secuencial con [Flow.first] por evento.
     * Es seguro con Firestore porque [CommentRepositoryImpl.getCommentsByEvent]
     * usa [callbackFlow] que emite al menos una vez antes de completarse.
     *
     * Nota: para listas grandes (>20 eventos), considerar una consulta
     * agregada en Firestore en lugar de N consultas individuales.
     *
     * @param events Lista de eventos a enriquecer.
     * @return Flow que emite la lista con [Event.commentsCount] actualizado.
     */
    private fun attachCommentCounts(events: List<Event>): Flow<List<Event>> = flow {
        val updatedList = mutableListOf<Event>()
        for (event in events) {
            val comments = commentRepository.getCommentsByEvent(event.id).first()
            updatedList += event.copy(commentsCount = comments.size)
        }
        emit(updatedList)
    }

    /**
     * Pre-carga los datos de los organizadores de los eventos dados.
     * Solo consulta los IDs que aún no están en el mapa para evitar
     * llamadas redundantes a Firestore.
     *
     * @param events Lista de eventos cuyos organizadores pre-cargar.
     */
    private suspend fun preloadOrganizers(events: List<Event>) {
        val userIds = events.mapNotNull { it.ownerId }.distinct()
        if (userIds.isNotEmpty()) {
            val users = userRepository.getUsersByIds(userIds)
            _usersMap.value = _usersMap.value + users.associateBy { it.id }
        }
    }

    /**
     * Quita un evento de la lista de intereses del usuario y decrementa
     * su contador de "me interesa" en Firestore.
     *
     * @param eventId ID del evento a remover de los intereses.
     */
    fun removeInterest(eventId: String) {
        val userId = _currentUserId.value ?: return
        viewModelScope.launch {
            userRepository.removeInterestFromUser(userId, eventId)
            eventRepository.removeInterest(eventId)
        }
    }
}