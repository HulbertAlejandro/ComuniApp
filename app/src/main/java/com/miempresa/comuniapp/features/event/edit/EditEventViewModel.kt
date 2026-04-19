package com.miempresa.comuniapp.features.event.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.resources.ResourceProvider
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ValidatedField
import com.miempresa.comuniapp.domain.model.*
import com.miempresa.comuniapp.domain.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EditEventViewModel @Inject constructor(
    private val repository: EventRepository,
    private val resources: ResourceProvider
) : ViewModel() {

    private var currentEvent: Event? = null

    // Campos validados alineados con CreateEvent
    val title = ValidatedField("") { if (it.isBlank()) resources.getString(R.string.edit_event_validation_title_required) else null }
    val description = ValidatedField("") { if (it.isBlank()) resources.getString(R.string.edit_event_validation_description_required) else null }
    val imageUrl = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.edit_event_validation_image_url_required) else if (!it.startsWith("http")) resources.getString(R.string.edit_event_validation_image_url_invalid) else null
    }
    val latitude = ValidatedField("") { it.toDoubleOrNull()?.let { null } ?: resources.getString(R.string.edit_event_validation_latitude_invalid) }
    val longitude = ValidatedField("") { it.toDoubleOrNull()?.let { null } ?: resources.getString(R.string.edit_event_validation_longitude_invalid) }

    var category by mutableStateOf(Category.DEPORTES)
    var maxAttendees by mutableStateOf("")
    var startDateMillis by mutableStateOf<Long?>(null)
    var endDateMillis by mutableStateOf<Long?>(null)

    private val _result = MutableStateFlow<RequestResult?>(null)
    val result: StateFlow<RequestResult?> = _result.asStateFlow()

    fun loadEvent(eventId: String) {
        if (currentEvent != null) return // Evita recargas infinitas

        viewModelScope.launch {
            repository.findById(eventId)?.let { ev ->
                currentEvent = ev

                // Usamos onChange para que el ValidatedField sepa que el valor cambió
                title.onChange(ev.title)
                description.onChange(ev.description)
                imageUrl.onChange(ev.imageUrl)
                latitude.onChange(ev.location.latitude.toString())
                longitude.onChange(ev.location.longitude.toString())

                category = ev.category
                maxAttendees = ev.maxAttendees?.toString() ?: ""

                startDateMillis = parseDate(ev.startDate)
                endDateMillis = parseDate(ev.endDate)
            }
        }
    }

    val isFormValid: Boolean
        get() {
            // Log para debug si fuera necesario:
            // println("T:${title.value.isNotBlank()} D:${description.value.isNotBlank()} I:${imageUrl.value.startsWith("http")} L:${latitude.value.isNotBlank()} DT:${startDateMillis != null}")

            val hasTitle = title.value.isNotBlank()
            val hasDesc = description.value.isNotBlank()
            val hasUrl = imageUrl.value.startsWith("http")
            val hasLat = latitude.value.isNotBlank() && latitude.value.toDoubleOrNull() != null
            val hasLng = longitude.value.isNotBlank() && longitude.value.toDoubleOrNull() != null

            val validDates = startDateMillis != null &&
                    endDateMillis != null &&
                    endDateMillis!! > startDateMillis!!

            // Verificamos que los campos obligatorios tengan contenido
            return hasTitle && hasDesc && hasUrl && hasLat && hasLng && validDates
        }

    fun updateEvent() {
        val event = currentEvent ?: return
        if (!isFormValid) return

        viewModelScope.launch {
            _result.value = RequestResult.Loading
            try {
                val updatedEvent = event.copy(
                    title = title.value,
                    description = description.value,
                    imageUrl = imageUrl.value,
                    category = category,
                    location = Location(latitude.value.toDouble(), longitude.value.toDouble()),
                    maxAttendees = maxAttendees.toIntOrNull(),
                    startDate = formatDate(startDateMillis!!),
                    endDate = formatDate(endDateMillis!!)
                )
                repository.update(updatedEvent)
                _result.value = RequestResult.Success(resources.getString(R.string.edit_event_update_success))
            } catch (e: Exception) {
                _result.value = RequestResult.Failure(e.message ?: resources.getString(R.string.edit_event_update_failure))
            }
        }
    }

    fun deleteEvent() {
        val id = currentEvent?.id ?: return
        viewModelScope.launch {
            _result.value = RequestResult.Loading
            try {
                repository.delete(id)
                _result.value = RequestResult.Success(resources.getString(R.string.edit_event_delete_success))
            } catch (e: Exception) {
                _result.value = RequestResult.Failure(resources.getString(R.string.edit_event_delete_failure))
            }
        }
    }

    fun resetResult() { _result.value = null }

    private fun formatDate(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

    private fun parseDate(date: String): Long? = try {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(date)?.time
    } catch (e: Exception) { null }
}