package com.miempresa.comuniapp.features.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.VerificationStatus
import com.miempresa.comuniapp.domain.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
class MapViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    // ── Todos los eventos aprobados ───────────────────────────────────────
    val approvedEvents: StateFlow<List<Event>> = eventRepository.events
        .map { list ->
            list.filter { it.verificationStatus == VerificationStatus.APPROVED }
                .distinctBy { it.id } // <--- ESTE ES EL CAMBIO: Filtra duplicados por ID
        }
        .stateIn(
            scope            = viewModelScope,
            started          = SharingStarted.WhileSubscribed(5_000),
            initialValue     = emptyList()
        )

    // ── Evento seleccionado al tocar un marcador ──────────────────────────
    private val _selectedEvent = MutableStateFlow<Event?>(null)
    val selectedEvent: StateFlow<Event?> = _selectedEvent.asStateFlow()

    // ── Eventos filtrados por cercanía ────────────────────────────────────
    // Se actualiza cuando el ViewModel recibe la posición del usuario.
    // radiusKm configurable — usamos 10 km como valor razonable para
    // eventos comunitarios en una ciudad.

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)

    val nearbyEvents: StateFlow<List<Event>> = combine(
        approvedEvents,
        _userLocation
    ) { events, location ->
        if (location == null) events           // sin ubicación → todos
        else events.filter { event ->
            haversineKm(
                lat1 = location.first,
                lon1 = location.second,
                lat2 = event.eventLocation.latitude,
                lon2 = event.eventLocation.longitude
            ) <= NEARBY_RADIUS_KM
        }
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // ── API pública ───────────────────────────────────────────────────────

    /** Llamado desde la Screen cuando el marcador de un evento es tocado. */
    fun onMarkerClick(eventId: String) {
        _selectedEvent.value = approvedEvents.value.find { it.id == eventId }
    }

    /** Llamado cuando el usuario toca fuera de la card o del marcador. */
    fun clearSelection() {
        _selectedEvent.value = null
    }

    /**
     * Actualiza la posición del usuario para calcular eventos cercanos.
     * Llamado desde la Screen cuando el LocationComponent reporta posición.
     */
    fun updateUserLocation(latitude: Double, longitude: Double) {
        _userLocation.value = latitude to longitude
    }

    // ── Helpers privados ──────────────────────────────────────────────────

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

    companion object {
        private const val NEARBY_RADIUS_KM = 10.0
    }
}