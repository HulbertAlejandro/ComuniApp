package com.miempresa.comuniapp.features.event.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ResourceProvider
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
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    init {
        viewModelScope.launch {
            sessionDataStore.sessionFlow.collect { session ->
                _ownerId.value = session?.userId ?: "user_test_123"
            }
        }
    }

    // --- Campos con Validación ---
    val title = ValidatedField("") { if (it.isBlank()) resources.getString(R.string.title_required) else null }
    val description = ValidatedField("") { if (it.isBlank()) resources.getString(R.string.description_required) else null }
    val imageUrl = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.image_url_required)
        else if (!it.startsWith("http")) resources.getString(R.string.image_url_invalid) else null
    }
    val maxAttendees = ValidatedField("") {
        it.toIntOrNull()?.let { num -> if (num <= 0) resources.getString(R.string.min_attendees) else null } ?: resources.getString(R.string.invalid_number)
    }
    val latitude = ValidatedField("") { it.toDoubleOrNull()?.let { null } ?: resources.getString(R.string.latitude_invalid) }
    val longitude = ValidatedField("") { it.toDoubleOrNull()?.let { null } ?: resources.getString(R.string.longitude_invalid) }

    // --- Categoría ---
    var selectedCategory by mutableStateOf<Category?>(null)
    fun onCategorySelected(category: Category) { selectedCategory = category }

    // --- Fechas (Corrección de Bug de Desfase) ---
    var startDateMillis by mutableStateOf<Long?>(null)
    var endDateMillis by mutableStateOf<Long?>(null)

    fun updateDateTime(isStart: Boolean, dateMillis: Long?, hour: Int, minute: Int) {
        // Usamos UTC para evitar el desfase del día al seleccionar en el DatePicker
        val base = dateMillis ?: (if (isStart) startDateMillis else endDateMillis) ?: System.currentTimeMillis()
        val zonedDateTime = Instant.ofEpochMilli(base)
            .atZone(ZoneId.of("America/Bogota"))
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)

        if (isStart) startDateMillis = zonedDateTime.toInstant().toEpochMilli()
        else endDateMillis = zonedDateTime.toInstant().toEpochMilli()
    }

    private val _result = MutableStateFlow<RequestResult?>(null)
    val result: StateFlow<RequestResult?> = _result

    val isFormValid: Boolean
        get() {
            val hasTitle = title.value.isNotBlank()
            val hasDesc = description.value.isNotBlank()
            val hasUrl = imageUrl.value.startsWith("http")
            val hasAttendees = maxAttendees.value.toIntOrNull()?.let { it > 0 } ?: false
            val hasLoc = latitude.value.isNotBlank() && longitude.value.isNotBlank()
            val hasDates = startDateMillis != null && endDateMillis != null && endDateMillis!! > startDateMillis!!
            val hasCat = selectedCategory != null

            return hasTitle && hasDesc && hasUrl && hasAttendees && hasLoc && hasDates && hasCat
        }
    fun createEvent() {
        val owner = _ownerId.value ?: return
        if (!isFormValid) return

        viewModelScope.launch {
            _result.value = RequestResult.Loading
            try {
                val start = Instant.ofEpochMilli(startDateMillis!!).atZone(ZoneId.systemDefault())
                val end = Instant.ofEpochMilli(endDateMillis!!).atZone(ZoneId.systemDefault())

                val event = Event(
                    id = UUID.randomUUID().toString(),
                    title = title.value,
                    description = description.value,
                    category = selectedCategory!!,
                    imageUrl = imageUrl.value,
                    location = Location(latitude.value.toDouble(), longitude.value.toDouble()),
                    startDate = start.format(dateFormatter),
                    endDate = end.format(dateFormatter),
                    maxAttendees = maxAttendees.value.toIntOrNull(),
                    ownerId = owner,
                    eventStatus = EventStatus.CREATED,
                    verificationStatus = VerificationStatus.PENDING
                )
                repository.save(event)
                clearForm()
                _result.value = RequestResult.Success(resources.getString(R.string.event_created_success))
            } catch (e: Exception) {
                _result.value = RequestResult.Failure(e.message ?: resources.getString(R.string.save_error))
            }
        }
    }

    private fun clearForm() {
        title.reset(); description.reset(); imageUrl.reset()
        maxAttendees.reset(); latitude.reset(); longitude.reset()
        selectedCategory = null; startDateMillis = null; endDateMillis = null
    }

    fun onImageUrlChange(url: String) { imageUrl.onChange(url) }
    fun onLatitudeChange(lat: String) { latitude.onChange(lat) }
    fun onLongitudeChange(lng: String) { longitude.onChange(lng) }
    fun resetResult() { _result.value = null }
}