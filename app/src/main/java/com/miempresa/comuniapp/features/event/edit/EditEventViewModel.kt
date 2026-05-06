package com.miempresa.comuniapp.features.event.edit

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
import com.miempresa.comuniapp.domain.model.*
import com.miempresa.comuniapp.domain.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

sealed interface UserEditUiEvent {
    data class ShowMessage(val message: String) : UserEditUiEvent
    data object NavigateBack : UserEditUiEvent
}

@HiltViewModel
class EditEventViewModel @Inject constructor(
    private val repository: EventRepository,
    private val resources: ResourceProvider
) : ViewModel() {

    private var currentEvent: Event? = null

    // ── Campos validados (sin cambios) ───────────────────────────────────

    val title = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.edit_event_validation_title_required)
        else null
    }

    val description = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.edit_event_validation_description_required)
        else null
    }

    val imageUrl = ValidatedField("") {
        when {
            it.isBlank() ->
                resources.getString(R.string.edit_event_validation_image_url_required)
            !it.startsWith("http") ->
                resources.getString(R.string.edit_event_validation_image_url_invalid)
            else -> null
        }
    }

    var category     by mutableStateOf(Category.DEPORTES)
    var maxAttendees by mutableStateOf("")
    var startDateMillis by mutableStateOf<Long?>(null)
    var endDateMillis   by mutableStateOf<Long?>(null)

    // ── Ubicación — reemplaza los ValidatedField de lat/lon ──────────────
    //
    // Se pre-carga en loadEvent() con la ubicación existente del evento.
    // Se actualiza cuando el usuario mueve el marcador en el mapa.

    private val _selectedLocation = MutableStateFlow<EventLocation?>(null)
    val selectedLocation: StateFlow<EventLocation?> = _selectedLocation.asStateFlow()

    /**
     * Expone la ubicación actual como [Point] de Mapbox para que la Screen
     * pueda pasárselo a [MapBox] vía [initialPoint] sin tener lógica de
     * conversión en la UI.
     */
    val initialMapPoint: Point?
        get() = _selectedLocation.value?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        }

    /**
     * Llamado desde la Screen cuando el usuario reposiciona el marcador.
     */
    fun onMapPointSelected(point: Point) {
        _selectedLocation.value = EventLocation(
            latitude  = point.latitude(),
            longitude = point.longitude()
        )
    }

    // ── Resultado ────────────────────────────────────────────────────────

    private val _result = MutableStateFlow<RequestResult?>(null)
    val result: StateFlow<RequestResult?> = _result.asStateFlow()

    // ── Carga del evento ─────────────────────────────────────────────────

    fun loadEvent(eventId: String) {
        if (currentEvent != null) return   // evita recargas en recomposición

        viewModelScope.launch {
            repository.findById(eventId)?.let { ev ->
                currentEvent = ev

                title.onChange(ev.title)
                description.onChange(ev.description)
                imageUrl.onChange(ev.imageUrl)

                // ✅ Pre-carga la ubicación existente en el StateFlow
                // La Screen la leerá vía viewModel.initialMapPoint
                _selectedLocation.value = ev.eventLocation

                category        = ev.category
                maxAttendees    = ev.maxAttendees?.toString() ?: ""
                startDateMillis = parseDate(ev.startDate)
                endDateMillis   = parseDate(ev.endDate)
            }
        }
    }

    // ── Validación ───────────────────────────────────────────────────────

    val isFormValid: Boolean
        get() = title.value.isNotBlank() &&
                description.value.isNotBlank() &&
                imageUrl.value.startsWith("http") &&
                _selectedLocation.value != null &&
                startDateMillis != null &&
                endDateMillis   != null &&
                endDateMillis!! > startDateMillis!!

    // ── Actualización ────────────────────────────────────────────────────

    fun updateEvent() {
        val event    = currentEvent          ?: return
        val location = _selectedLocation.value ?: return
        if (!isFormValid) return

        viewModelScope.launch {
            _result.value = RequestResult.Loading
            try {
                repository.update(
                    event.copy(
                        title         = title.value.trim(),
                        description   = description.value.trim(),
                        imageUrl      = imageUrl.value.trim(),
                        category      = category,
                        eventLocation = location,
                        maxAttendees  = maxAttendees.toIntOrNull(),
                        startDate     = formatDate(startDateMillis!!),
                        endDate       = formatDate(endDateMillis!!)
                    )
                )
                _result.value = RequestResult.Success(
                    resources.getString(R.string.edit_event_update_success)
                )
            } catch (e: Exception) {
                _result.value = RequestResult.Failure(
                    e.message ?: resources.getString(R.string.edit_event_update_failure)
                )
            }
        }
    }

    // ── Eliminación ──────────────────────────────────────────────────────

    fun deleteEvent() {
        val id = currentEvent?.id ?: return
        viewModelScope.launch {
            _result.value = RequestResult.Loading
            try {
                repository.delete(id)
                _result.value = RequestResult.Success(
                    resources.getString(R.string.edit_event_delete_success)
                )
            } catch (e: Exception) {
                _result.value = RequestResult.Failure(
                    resources.getString(R.string.edit_event_delete_failure)
                )
            }
        }
    }

    fun resetResult() { _result.value = null }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun formatDate(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

    private fun parseDate(date: String): Long? = try {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(date)?.time
    } catch (e: Exception) { null }
}