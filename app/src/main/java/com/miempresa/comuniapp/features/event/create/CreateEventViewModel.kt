package com.miempresa.comuniapp.features.event.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.resources.ResourceProvider
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ValidatedField
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.*
import com.miempresa.comuniapp.domain.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val repository: EventRepository,
    private val sessionDataStore: SessionDataStore,
    private val resources: ResourceProvider
) : ViewModel() {

    private val _ownerId = MutableStateFlow<String?>(null)
    private val _organizerName = MutableStateFlow<String?>(null)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    init {
        viewModelScope.launch {
            sessionDataStore.sessionFlow.collect { session ->
                _ownerId.value = session?.userId ?: "user_test_123"
                _organizerName.value = session?.name ?: "Usuario Anónimo"
            }
        }
    }

    // ── Campos con validación (sin cambios) ──────────────────────────────

    val title = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.validation_error_title_required) else null
    }

    val description = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.validation_error_description_required) else null
    }

    val imageUrl = ValidatedField("") {
        when {
            it.isBlank() -> resources.getString(R.string.validation_error_image_url_required)
            !it.startsWith("http") -> resources.getString(R.string.validation_error_image_url_invalid)
            else -> null
        }
    }

    val maxAttendees = ValidatedField("") {
        it.toIntOrNull()
            ?.let { num ->
                if (num <= 0) resources.getString(R.string.validation_error_max_attendees_min)
                else null
            }
            ?: resources.getString(R.string.validation_error_max_attendees_invalid)
    }

    // ── Ubicación — reemplaza los ValidatedField de lat/lon ──────────────
    //
    // El flujo es:
    //   MapBox.onMapClickListener → onMapPointSelected(Point) → _selectedLocation
    //
    // Usamos EventLocation como tipo de dominio; el Point de Mapbox
    // se convierte aquí para que la Screen no tenga lógica de dominio.

    private val _selectedLocation = MutableStateFlow<EventLocation?>(null)
    val selectedLocation: StateFlow<EventLocation?> = _selectedLocation.asStateFlow()

    /**
     * Llamado desde la Screen cuando el usuario toca el mapa.
     * Convierte el [Point] de Mapbox a [EventLocation] de dominio.
     */
    fun onMapPointSelected(point: Point) {
        _selectedLocation.value = EventLocation(
            latitude = point.latitude(),
            longitude = point.longitude()
            // direccionDisplay vacío por ahora;
            // el Paso 4 agrega reverse geocoding aquí
        )
    }

    // ── Categoría ────────────────────────────────────────────────────────

    var selectedCategory by mutableStateOf<Category?>(null)
    fun onCategorySelected(category: Category) { selectedCategory = category }

    // ── Fechas (lógica original intacta) ─────────────────────────────────

    var startDateMillis by mutableStateOf<Long?>(null)
    var endDateMillis by mutableStateOf<Long?>(null)

    fun updateDateTime(isStart: Boolean, dateMillis: Long?, hour: Int, minute: Int) {
        val base = dateMillis
            ?: (if (isStart) startDateMillis else endDateMillis)
            ?: System.currentTimeMillis()

        val zoned = Instant.ofEpochMilli(base)
            .atZone(ZoneId.of("America/Bogota"))
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)

        if (isStart) startDateMillis = zoned.toInstant().toEpochMilli()
        else endDateMillis = zoned.toInstant().toEpochMilli()
    }

    // ── Resultado ────────────────────────────────────────────────────────

    private val _result = MutableStateFlow<RequestResult?>(null)
    val result: StateFlow<RequestResult?> = _result

    // ── Validación ───────────────────────────────────────────────────────

    val isFormValid: Boolean
        get() = title.value.isNotBlank() &&
                description.value.isNotBlank() &&
                imageUrl.value.startsWith("http") &&
                (maxAttendees.value.toIntOrNull()?.let { it > 0 } ?: false) &&
                _selectedLocation.value != null &&
                startDateMillis != null &&
                endDateMillis != null &&
                endDateMillis!! > startDateMillis!! &&
                selectedCategory != null

    // ── Creación ─────────────────────────────────────────────────────────

    fun createEvent() {
        val owner    = _ownerId.value ?: return
        val location = _selectedLocation.value ?: return
        if (!isFormValid) return

        viewModelScope.launch {
            _result.value = RequestResult.Loading
            try {
                val start = Instant.ofEpochMilli(startDateMillis!!)
                    .atZone(ZoneId.systemDefault())
                val end = Instant.ofEpochMilli(endDateMillis!!)
                    .atZone(ZoneId.systemDefault())

                repository.save(
                    Event(
                        id               = UUID.randomUUID().toString(),
                        title            = title.value.trim(),
                        description      = description.value.trim(),
                        category         = selectedCategory!!,
                        imageUrl         = imageUrl.value.trim(),
                        eventLocation    = location,
                        startDate        = start.format(dateFormatter),
                        endDate          = end.format(dateFormatter),
                        maxAttendees     = maxAttendees.value.toIntOrNull(),
                        ownerId          = owner,
                        organizerName    = _organizerName.value
                            ?: resources.getString(R.string.default_organizer_name),
                        eventStatus      = EventStatus.CREATED,
                        verificationStatus = VerificationStatus.PENDING
                    )
                )
                clearForm()
                _result.value = RequestResult.Success(
                    resources.getString(R.string.create_event_success)
                )
            } catch (e: Exception) {
                _result.value = RequestResult.Failure(
                    e.message ?: resources.getString(R.string.create_event_failure)
                )
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun clearForm() {
        title.reset()
        description.reset()
        imageUrl.reset()
        maxAttendees.reset()
        _selectedLocation.value = null
        selectedCategory        = null
        startDateMillis         = null
        endDateMillis           = null
    }

    fun onImageUrlChange(url: String) { imageUrl.onChange(url) }
    fun resetResult()                 { _result.value = null }
}