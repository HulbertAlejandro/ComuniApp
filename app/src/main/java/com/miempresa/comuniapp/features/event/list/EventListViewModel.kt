package com.miempresa.comuniapp.features.event.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.EventLocation
import com.miempresa.comuniapp.domain.model.EventStatus
import com.miempresa.comuniapp.domain.model.ReputationPoints
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.model.VerificationStatus
import com.miempresa.comuniapp.domain.repository.CommentRepository
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.*

@OptIn(ExperimentalCoroutinesApi::class)          // ✅ flatMapLatest opt-in a nivel de clase
@HiltViewModel
class EventListViewModel @Inject constructor(
    private val repository: EventRepository,
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    // ── Filtros de UI (Compose state) ─────────────────────────────────────

    var selectedFilter   by mutableStateOf<String?>(null)
    var selectedCategory by mutableStateOf<Category?>(null)
    var selectedDate     by mutableStateOf<LocalDate?>(null)
    var showFiltersDialog by mutableStateOf(false)
    var showDatePicker    by mutableStateOf(false)
    var searchQuery       by mutableStateOf("")

    // ── Estado interno de sesión ──────────────────────────────────────────

    private val _currentUserId = MutableStateFlow<String?>(null)

    // Ubicación del usuario para el filtro "Cerca de mí".
    // Ya no viene de User.location (eliminado en Paso 1) sino de
    // EventLocation hardcodeado al barrio registrado — en el futuro
    // vendrá del GPS real vía MapViewModel.updateUserLocation().
    // Se mantiene privado porque la Screen no lo necesita directamente.
    private val _currentUserEventLocation = MutableStateFlow<EventLocation?>(null)

    // ── Carga de usuarios relacionados (organizadores) ────────────────────

    private val _usersMap = MutableStateFlow<Map<String, User>>(emptyMap())
    val usersMap: StateFlow<Map<String, User>> = _usersMap.asStateFlow()

    // ── Estado de carga ───────────────────────────────────────────────────
    // Interno: se pone a false cuando llega el primer batch de eventos.
    // La Screen observa este valor para mostrar el skeleton/spinner.
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Filtro por proximidad ─────────────────────────────────────────────
    private val _proximityActive = MutableStateFlow(false)
    val proximityActive: StateFlow<Boolean> = _proximityActive.asStateFlow()

    // Radio constante — minúsculas porque es propiedad, no companion object
    private val proximityRadiusKm = 5.0

    // ── Intereses del usuario (reactivo) ─────────────────────────────────

    val votedEventIds: StateFlow<Set<String>> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                userRepository.users.map { users ->
                    users.find { it.id == session.userId }?.interestedEventIds ?: emptySet()
                }
            }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptySet()
            )

    // ── Filtro por categorías favoritas ───────────────────────────────────

    private val _favoriteCategoriesFilter = MutableStateFlow(false)
    val favoriteCategoriesFilter: StateFlow<Boolean> = _favoriteCategoriesFilter.asStateFlow()

    private val _userFavoriteCategories: StateFlow<List<Category>> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                userRepository.users.map { list ->
                    list.find { it.id == session.userId }?.favoriteCategories ?: emptyList()
                }
            }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // ── Conteo de comentarios (reactivo) ──────────────────────────────────

    val commentCountsByEvent: StateFlow<Map<String, Int>> =
        commentRepository.comments
            .map { comments -> comments.groupingBy { it.eventId }.eachCount() }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyMap()
            )

    // ── Eventos aprobados base ────────────────────────────────────────────

    private val approvedEventsFlow = repository
        .getEventsByVerificationStatus(VerificationStatus.APPROVED)
        .map { events ->
            events.filterNot {
                it.eventStatus == EventStatus.FULL ||
                        it.eventStatus == EventStatus.FINISHED
            }
        }
        .onEach { events ->
            _isLoading.value = false
            preloadOrganizers(events)
        }

    // ── Flow combinado de eventos filtrados ───────────────────────────────

    @Suppress("UNCHECKED_CAST")
    val events: StateFlow<List<Event>> = combine(
        approvedEventsFlow,
        snapshotFlow { selectedCategory },
        snapshotFlow { selectedDate },
        snapshotFlow { searchQuery },
        combine(_proximityActive, _currentUserEventLocation) { active, loc -> active to loc },
        combine(_favoriteCategoriesFilter, _userFavoriteCategories) { active, cats -> active to cats },
        commentCountsByEvent
    ) { array ->
        val events                          = array[0] as List<Event>
        val category                        = array[1] as Category?
        val date                            = array[2] as LocalDate?
        val query                           = array[3] as String
        val (proximityActive, userLocation) = array[4] as Pair<Boolean, EventLocation?>
        val (favActive, favCats)            = array[5] as Pair<Boolean, List<Category>>
        val commentCounts                   = array[6] as Map<String, Int>

        val eventsWithCommentCounts = events.map { event ->
            event.copy(commentsCount = commentCounts[event.id] ?: 0)
        }

        applyFilters(
            eventsWithCommentCounts,
            category,
            date,
            query,
            proximityActive,
            userLocation,
            favActive,
            favCats
        ).sortedByDescending { it.interestCount }

    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // ── init ──────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            sessionDataStore.sessionFlow
                .filterNotNull()
                .collectLatest { session ->
                    val userId = session.userId
                    if (_currentUserId.value != userId) {
                        _currentUserId.value = userId

                        // ✅ CORRECCIÓN: User.location fue eliminado en el Paso 1.
                        // El usuario ya no tiene coordenadas propias.
                        // El filtro "Cerca de mí" requiere la posición GPS real,
                        // que llegará en una iteración futura vía FusedLocationProvider.
                        // Por ahora desactivamos la pre-carga de ubicación desde User.
                        _currentUserEventLocation.value = null
                    }
                }
        }
    }

    // ── Intereses ─────────────────────────────────────────────────────────

    fun onInterested(eventId: String) {
        val userId = _currentUserId.value ?: return

        viewModelScope.launch {
            if (votedEventIds.value.contains(eventId)) {
                repository.removeInterest(eventId)
                userRepository.removeInterestFromUser(userId, eventId)

                repository.findById(eventId)?.ownerId?.let { ownerId ->
                    if (ownerId != userId) {
                        userRepository.addPoints(ownerId, ReputationPoints.INTEREST_REMOVED)
                        userRepository.updateLevel(ownerId)
                    }
                }
            } else {
                repository.addInterest(eventId)
                userRepository.addInterestToUser(userId, eventId)

                repository.findById(eventId)?.ownerId?.let { ownerId ->
                    if (ownerId != userId) {
                        userRepository.addPoints(ownerId, ReputationPoints.INTEREST_ADDED)
                        userRepository.updateLevel(ownerId)
                    }
                }
            }
        }
    }

    // ── Filtros ───────────────────────────────────────────────────────────

    fun toggleProximityFilter() {
        val nowActive = !_proximityActive.value
        _proximityActive.value = nowActive
        selectedFilter = if (nowActive) "Cerca de mí" else null
        if (nowActive) {
            selectedCategory = null
            selectedDate     = null
            _favoriteCategoriesFilter.value = false
        }
    }

    fun filterByCategory(category: Category?) {
        selectedCategory = category
        selectedFilter   = if (category != null) "Categoría" else null
        if (category != null) {
            _proximityActive.value          = false
            _favoriteCategoriesFilter.value = false
        }
    }

    fun filterByDate(date: LocalDate?) {
        selectedDate   = date
        selectedFilter = if (date != null) "Fecha" else null
        if (date != null) {
            _proximityActive.value          = false
            _favoriteCategoriesFilter.value = false
        }
    }

    fun toggleFavoriteCategoriesFilter() {
        val nowActive = !_favoriteCategoriesFilter.value
        _favoriteCategoriesFilter.value = nowActive
        selectedFilter = if (nowActive) "Recomendados" else null
        if (nowActive) {
            selectedCategory = null
            selectedDate     = null
            _proximityActive.value = false
        }
    }

    fun onSearchQueryChanged(query: String) { searchQuery = query }

    fun clearAllFilters() {
        selectedCategory                = null
        selectedDate                    = null
        selectedFilter                  = null
        searchQuery                     = ""
        _proximityActive.value          = false
        _favoriteCategoriesFilter.value = false
    }

    // ── Helpers privados ──────────────────────────────────────────────────

    private fun applyFilters(
        events            : List<Event>,
        category          : Category?,
        date              : LocalDate?,
        query             : String,
        proximityActive   : Boolean,
        userEventLocation : EventLocation?,
        favoritesActive   : Boolean,
        favoriteCategories: List<Category>
    ): List<Event> = events.filter { event ->

        val categoryMatch = category == null || event.category == category

        val dateMatch = date == null || eventMatchesDate(event, date)

        val queryMatch = query.isBlank() ||
                event.title.contains(query, ignoreCase = true) ||
                event.description.contains(query, ignoreCase = true)

        // Si el filtro está activo pero no hay ubicación del usuario,
        // no excluimos ningún evento (fail-open) hasta tener GPS real.
        val proximityMatch = !proximityActive || userEventLocation == null ||
                haversineKm(
                    userEventLocation.latitude,
                    userEventLocation.longitude,
                    event.eventLocation.latitude,
                    event.eventLocation.longitude
                ) <= proximityRadiusKm

        val favoritesMatch = !favoritesActive ||
                favoriteCategories.isEmpty() ||
                event.category in favoriteCategories

        categoryMatch && dateMatch && queryMatch && proximityMatch && favoritesMatch
    }

    private fun eventMatchesDate(event: Event, date: LocalDate): Boolean =
        try {
            LocalDate.parse(event.startDate.split(" ")[0]) == date
        } catch (_: Exception) {
            // ✅ CORRECCIÓN: parámetro nombrado '_' para silenciar "never used"
            false
        }

    private fun haversineKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r    = 6_371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a    = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun preloadOrganizers(events: List<Event>) {
        events.map { it.ownerId }.distinct().forEach { userId ->
            if (!_usersMap.value.containsKey(userId)) {
                viewModelScope.launch {
                    val user = userRepository.findById(userId)
                    if (user != null) _usersMap.update { it + (userId to user) }
                }
            }
        }
    }
}