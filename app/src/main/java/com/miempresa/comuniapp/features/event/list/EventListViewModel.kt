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
import com.miempresa.comuniapp.domain.model.Location
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.model.VerificationStatus
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
class EventListViewModel @Inject constructor(
    private val repository: EventRepository,
    private val userRepository: UserRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    var selectedFilter by mutableStateOf<String?>(null)
    var selectedCategory by mutableStateOf<Category?>(null)
    var selectedDate by mutableStateOf<LocalDate?>(null)
    var showFiltersDialog by mutableStateOf(false)
    var showDatePicker by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    private val _currentUserLocation = MutableStateFlow<Location?>(null)
    val currentUserLocation: StateFlow<Location?> = _currentUserLocation.asStateFlow()

    private val _usersMap = MutableStateFlow<Map<String, User>>(emptyMap())
    val usersMap: StateFlow<Map<String, User>> = _usersMap.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _proximityActive = MutableStateFlow(false)
    val proximityActive: StateFlow<Boolean> = _proximityActive.asStateFlow()

    // ✅ ID del usuario actualmente logueado (para cambio de sesión)
    private val _currentUserId = MutableStateFlow<String?>(null)

    // ✅ Intereses cargados desde el usuario de sesión (no global)
    private val _votedEventIds = MutableStateFlow<Set<String>>(emptySet())
    val votedEventIds: StateFlow<Set<String>> = _votedEventIds.asStateFlow()

    private val PROXIMITY_RADIUS_KM = 5.0

    private val approvedEventsFlow = repository
        .getEventsByVerificationStatus(VerificationStatus.APPROVED)
        .onEach { events ->
            _isLoading.value = false
            preloadOrganizers(events)
        }

    val events: StateFlow<List<Event>> = combine(
        approvedEventsFlow,
        snapshotFlow { selectedCategory },
        snapshotFlow { selectedDate },
        snapshotFlow { searchQuery },
        combine(_proximityActive, _currentUserLocation) { active, loc -> active to loc }
    ) { events, category, date, query, (proximityActive, userLocation) ->
        applyFilters(events, category, date, query, proximityActive, userLocation)
            .sortedByDescending { it.interestCount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // ✅ Observa cambios de sesión para recargar intereses dinámicamente
        viewModelScope.launch {
            sessionDataStore.sessionFlow
                .filterNotNull()
                .collectLatest { session ->
                    val userId = session.userId

                    // Si cambió el usuario, recargamos todo
                    if (_currentUserId.value != userId) {
                        _currentUserId.value = userId

                        // Cargar ubicación del nuevo usuario
                        val user = userRepository.findById(userId)
                        _currentUserLocation.value = user?.location

                        // ✅ Cargar intereses del nuevo usuario desde su propio arreglo
                        _votedEventIds.value =
                            userRepository.getUserInterestedEventIds(userId)
                    }
                }
        }
    }

    // ✅ Toggle: agrega o quita de ambos lados (User + Event)
    fun onInterested(eventId: String) {
        val userId = _currentUserId.value ?: return

        viewModelScope.launch {
            if (_votedEventIds.value.contains(eventId)) {
                // Ya votó → quitar
                repository.removeInterest(eventId)
                userRepository.removeInterestFromUser(userId, eventId)
                _votedEventIds.update { it - eventId }
            } else {
                // No ha votado → agregar
                repository.addInterest(eventId)
                userRepository.addInterestToUser(userId, eventId)
                _votedEventIds.update { it + eventId }
            }
        }
    }

    // ── Filtros (sin cambios) ─────────────────────────────────────────────────

    fun toggleProximityFilter() {
        val nowActive = !_proximityActive.value
        _proximityActive.value = nowActive
        selectedFilter = if (nowActive) "Cerca de mí" else null
        if (nowActive) { selectedCategory = null; selectedDate = null }
    }

    fun filterByCategory(category: Category?) {
        selectedCategory = category
        selectedFilter = if (category != null) "Categoría" else null
        if (category != null) _proximityActive.value = false
    }

    fun filterByDate(date: LocalDate?) {
        selectedDate = date
        selectedFilter = if (date != null) "Fecha" else null
        if (date != null) _proximityActive.value = false
    }

    fun onSearchQueryChanged(query: String) { searchQuery = query }

    fun clearAllFilters() {
        selectedCategory = null
        selectedDate = null
        selectedFilter = null
        searchQuery = ""
        _proximityActive.value = false
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun applyFilters(
        events: List<Event>,
        category: Category?,
        date: LocalDate?,
        query: String,
        proximityActive: Boolean,
        userLocation: Location?
    ): List<Event> = events.filter { event ->
        val categoryMatch = category == null || event.category == category
        val dateMatch = date == null || eventMatchesDate(event, date)
        val queryMatch = query.isBlank() ||
                event.title.contains(query, ignoreCase = true) ||
                event.description.contains(query, ignoreCase = true)
        val proximityMatch = !proximityActive || userLocation == null ||
                haversineKm(
                    userLocation.latitude, userLocation.longitude,
                    event.location.latitude, event.location.longitude
                ) <= PROXIMITY_RADIUS_KM
        categoryMatch && dateMatch && queryMatch && proximityMatch
    }

    private fun eventMatchesDate(event: Event, date: LocalDate): Boolean = try {
        LocalDate.parse(event.startDate.split(" ")[0]) == date
    } catch (e: Exception) { false }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
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