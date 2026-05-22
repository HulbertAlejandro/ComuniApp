package com.miempresa.comuniapp.features.event.list

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.core.notifications.NotificationSender
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.EventStatus
import com.miempresa.comuniapp.domain.model.NotificationType
import com.miempresa.comuniapp.domain.model.ReputationPoints
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.model.VerificationStatus
import com.miempresa.comuniapp.domain.repository.CommentRepository
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventListViewModel @Inject constructor(
    private val repository: EventRepository,
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository,
    private val notificationSender: NotificationSender,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    // ─────────────────────────────────────────────────────────────
    // UI STATE
    // ─────────────────────────────────────────────────────────────

    var selectedFilter by mutableStateOf<String?>(null)
    var selectedCategory by mutableStateOf<Category?>(null)
    var selectedDate by mutableStateOf<LocalDate?>(null)
    var showFiltersDialog by mutableStateOf(false)
    var showDatePicker by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    // ─────────────────────────────────────────────────────────────
    // SESSION
    // ─────────────────────────────────────────────────────────────

    private val _currentUserId: StateFlow<String?> =
        sessionDataStore.sessionFlow
            .map { it?.userId }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                null
            )

    // ─────────────────────────────────────────────────────────────
    // USERS
    // ─────────────────────────────────────────────────────────────

    private val _usersMap = MutableStateFlow<Map<String, User>>(emptyMap())
    val usersMap: StateFlow<Map<String, User>> = _usersMap.asStateFlow()

    // ─────────────────────────────────────────────────────────────
    // INTERESTS (OPTIMISTIC UI)
    // ─────────────────────────────────────────────────────────────

    private val _votedEventIds = MutableStateFlow<Set<String>>(emptySet())
    val votedEventIds: StateFlow<Set<String>> = _votedEventIds.asStateFlow()

    // ─────────────────────────────────────────────────────────────
    // FAVORITES FILTER
    // ─────────────────────────────────────────────────────────────

    private val _favoriteCategoriesFilter =
        MutableStateFlow(false)

    val favoriteCategoriesFilter =
        _favoriteCategoriesFilter.asStateFlow()

    private val _userFavoriteCategories:
            StateFlow<List<Category>> =

        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->

                userRepository.users.map { users ->

                    users.find {
                        it.id == session.userId
                    }?.favoriteCategories ?: emptyList()
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                emptyList()
            )

    // ─────────────────────────────────────────────────────────────
    // COMMENTS REALTIME
    // ─────────────────────────────────────────────────────────────

    val commentCountsByEvent:
            StateFlow<Map<String, Int>> =

        commentRepository.comments
            .map { comments ->

                comments
                    .groupingBy { it.eventId }
                    .eachCount()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyMap()
            )

    // ─────────────────────────────────────────────────────────────
    // EVENTS
    // ─────────────────────────────────────────────────────────────

    private val approvedEventsFlow =
        repository
            .getEventsByVerificationStatus(
                VerificationStatus.APPROVED
            )
            .map { events ->

                events.filterNot {

                    it.eventStatus == EventStatus.FULL ||
                            it.eventStatus == EventStatus.FINISHED
                }
            }
            .also { flow ->

                viewModelScope.launch {

                    flow.collect { events ->
                        preloadOrganizers(events)
                    }
                }
            }

    // ─────────────────────────────────────────────────────────────
    // EVENTS UI STATE
    // ─────────────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    val events: StateFlow<List<Event>> = combine(
        approvedEventsFlow,
        snapshotFlow { selectedCategory },
        snapshotFlow { selectedDate },
        snapshotFlow { searchQuery },
        combine(
            _favoriteCategoriesFilter,
            _userFavoriteCategories
        ) { active, cats ->
            active to cats
        },
        commentCountsByEvent
    ) { array ->

        val events =
            array[0] as List<Event>

        val category =
            array[1] as Category?

        val date =
            array[2] as LocalDate?

        val query =
            array[3] as String

        val (favActive, favCats) =
            array[4] as Pair<Boolean, List<Category>>

        val commentCounts =
            array[5] as Map<String, Int>

        // 🔥 SIEMPRE derivar commentsCount en memoria
        val enrichedEvents = events.map { event ->

            event.copy(
                commentsCount =
                    commentCounts[event.id] ?: 0
            )
        }

        applyFilters(
            enrichedEvents,
            category,
            date,
            query,
            favActive,
            favCats
        ).sortedByDescending {
            it.interestCount
        }

    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    // ─────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────

    init {

        observeCurrentUserInterests()
    }

    // ─────────────────────────────────────────────────────────────
    // OBSERVE USER INTERESTS
    // ─────────────────────────────────────────────────────────────

    private fun observeCurrentUserInterests() {

        viewModelScope.launch {

            sessionDataStore.sessionFlow
                .filterNotNull()
                .flatMapLatest { session ->

                    userRepository.users.map { users ->

                        users.find {
                            it.id == session.userId
                        }?.interestedEventIds?.toSet()
                            ?: emptySet()
                    }
                }
                .collect { interests ->

                    _votedEventIds.value = interests
                }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // INTEREST ACTION
    // ─────────────────────────────────────────────────────────────

    fun onInterested(eventId: String) {
        val userId = _currentUserId.value ?: return

        viewModelScope.launch {
            try {
                val alreadyInterested = _votedEventIds.value.contains(eventId)

                if (alreadyInterested) {
                    _votedEventIds.update { it - eventId }
                    repository.removeInterest(eventId)
                    userRepository.removeInterestFromUser(userId, eventId)
                } else {
                    // 1. Obtener el estado actual del evento DIRECTAMENTE de tu StateFlow local
                    // Esto garantiza que tenemos el valor exacto antes de que la red se ensucie
                    val currentEvent = events.value.find { it.id == eventId } ?: return@launch
                    val ownerId = currentEvent.ownerId

                    // 2. Aplicar la actualización optimista/local de inmediato para la lógica
                    val newCount = currentEvent.interestCount + 1
                    Log.d("EventList", "Intereses calculados con precisión: $newCount")

                    // 3. Modificar los datos en el servidor de forma asíncrona
                    _votedEventIds.update { it + eventId }
                    repository.addInterest(eventId)
                    userRepository.addInterestToUser(userId, eventId)

                    // 4. Lógica de Notificaciones
                    if (ownerId != userId) {
                        userRepository.addPoints(ownerId, ReputationPoints.INTEREST_ADDED)
                        userRepository.updateLevel(ownerId)

                        val interestedUser = userRepository.findById(userId)

                        // Notificación estándar de "Me interesa"
                        notificationSender.enviar(
                            destinatarioId = ownerId,
                            tipo = NotificationType.NEW_INTEREST,
                            titulo = "❤️ Nuevo interés",
                            cuerpo = "${interestedUser?.name ?: "Alguien"} marcó interés en \"${currentEvent.title}\".",
                            relatedEventId = eventId
                        )

                        // 🔥 Ahora la condición es 100% segura y libre de condiciones de carrera
                        if (newCount > 0 && newCount % 10 == 0) {
                            notificationSender.enviar(
                                destinatarioId = ownerId,
                                tipo = NotificationType.EVENT_FEATURED,
                                titulo = "🔥 ¡Tu evento es tendencia!",
                                cuerpo = "\"${currentEvent.title}\" alcanzó $newCount personas interesadas.",
                                relatedEventId = eventId
                            )
                            Log.d("EventList", "Notificación de tendencia enviada con éxito para el conteo: $newCount")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("EventList", "Error procesando interés: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // FILTERS
    // ─────────────────────────────────────────────────────────────

    fun filterByCategory(category: Category?) {

        selectedCategory = category

        selectedFilter =
            if (category != null)
                "Categoría"
            else
                null

        if (category != null) {

            _favoriteCategoriesFilter.value = false
        }
    }

    fun filterByDate(date: LocalDate?) {

        selectedDate = date

        selectedFilter =
            if (date != null)
                "Fecha"
            else
                null

        if (date != null) {

            _favoriteCategoriesFilter.value = false
        }
    }

    fun toggleFavoriteCategoriesFilter() {

        val nowActive =
            !_favoriteCategoriesFilter.value

        _favoriteCategoriesFilter.value =
            nowActive

        selectedFilter =
            if (nowActive)
                "Recomendados"
            else
                null

        if (nowActive) {

            selectedCategory = null
            selectedDate = null
        }
    }

    fun onSearchQueryChanged(query: String) {

        searchQuery = query
    }

    fun clearAllFilters() {

        selectedCategory = null
        selectedDate = null
        selectedFilter = null
        searchQuery = ""

        _favoriteCategoriesFilter.value = false
    }

    // ─────────────────────────────────────────────────────────────
    // FILTER ENGINE
    // ─────────────────────────────────────────────────────────────

    private fun applyFilters(
        events: List<Event>,
        category: Category?,
        date: LocalDate?,
        query: String,
        favoritesActive: Boolean,
        favoriteCategories: List<Category>
    ): List<Event> {

        return events.filter { event ->

            val categoryMatch =
                category == null ||
                        event.category == category

            val dateMatch =
                date == null ||
                        eventMatchesDate(event, date)

            val queryMatch =
                query.isBlank() ||
                        event.title.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        event.description.contains(
                            query,
                            ignoreCase = true
                        )

            val favoritesMatch =
                !favoritesActive ||
                        favoriteCategories.isEmpty() ||
                        event.category in favoriteCategories

            categoryMatch &&
                    dateMatch &&
                    queryMatch &&
                    favoritesMatch
        }
    }

    private fun eventMatchesDate(
        event: Event,
        date: LocalDate
    ): Boolean {

        return try {

            LocalDate.parse(
                event.startDate.split(" ")[0]
            ) == date

        } catch (_: Exception) {

            false
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ORGANIZERS CACHE
    // ─────────────────────────────────────────────────────────────

    private fun preloadOrganizers(
        events: List<Event>
    ) {

        events.map {
            it.ownerId
        }
            .distinct()
            .forEach { userId ->

                if (
                    !_usersMap.value.containsKey(userId)
                ) {

                    viewModelScope.launch {

                        val user =
                            userRepository.findById(userId)

                        if (user != null) {

                            _usersMap.update {
                                it + (userId to user)
                            }
                        }
                    }
                }
            }
    }
}