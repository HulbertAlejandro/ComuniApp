package com.miempresa.comuniapp.features.event.edit

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.FileProvider
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
import java.io.File
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

    // ── Campos validados ─────────────────────────────────────────────────

    val title = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.edit_event_validation_title_required) else null
    }

    val description = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.edit_event_validation_description_required) else null
    }

    var category        by mutableStateOf(Category.DEPORTES)
    var maxAttendees    by mutableStateOf("")
    var startDateMillis by mutableStateOf<Long?>(null)
    var endDateMillis   by mutableStateOf<Long?>(null)

    // ── Imágenes múltiples ───────────────────────────────────────────────
    //
    // Al cargar el evento, pre-populamos con las URIs existentes (String→Uri).
    // El usuario puede añadir nuevas (cámara/galería) o eliminar las existentes.
    // Al guardar, convertimos de vuelta a List<String>.

    private val _imageUris = MutableStateFlow<List<Uri>>(emptyList())
    val imageUris: StateFlow<List<Uri>> = _imageUris.asStateFlow()

    /** URI temporal del disparo de cámara actual. */
    private var _currentCameraUri: Uri? = null

    /**
     * Genera un archivo temporal único en caché para cada foto de cámara.
     * Llama a esto inmediatamente antes de lanzar el TakePicture launcher.
     */
    fun createTempCameraUri(context: Context): Uri {
        val dir  = File(context.cacheDir, "event_images").also { it.mkdirs() }
        val file = File(dir, "camera_${UUID.randomUUID()}.jpg")
        val uri  = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        _currentCameraUri = uri
        return uri
    }

    /** Llamado cuando TakePicture devuelve. Solo agrega si la foto fue confirmada. */
    fun onCameraImageCaptured(success: Boolean) {
        if (success) _currentCameraUri?.let { _imageUris.value += it }
        _currentCameraUri = null
    }

    /** Llamado cuando el selector de galería devuelve URIs. */
    fun onGalleryImagesSelected(uris: List<Uri>) {
        if (uris.isNotEmpty()) _imageUris.value += uris
    }

    /** Elimina la imagen en la posición [index]. */
    fun removeImage(index: Int) {
        _imageUris.value = _imageUris.value
            .toMutableList()
            .also { it.removeAt(index) }
    }

    // ── Ubicación ────────────────────────────────────────────────────────

    private val _selectedLocation = MutableStateFlow<EventLocation?>(null)
    val selectedLocation: StateFlow<EventLocation?> = _selectedLocation.asStateFlow()

    val initialMapPoint: Point?
        get() = _selectedLocation.value?.let { Point.fromLngLat(it.longitude, it.latitude) }

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
                category        = ev.category
                maxAttendees    = ev.maxAttendees?.toString() ?: ""
                startDateMillis = parseDate(ev.startDate)
                endDateMillis   = parseDate(ev.endDate)

                // ✅ Pre-cargar las URIs existentes como Uri (String → Uri)
                _imageUris.value = ev.imageUris.map { Uri.parse(it) }

                // ✅ Pre-cargar ubicación existente
                _selectedLocation.value = ev.eventLocation
            }
        }
    }

    fun updateDateTime(isStart: Boolean, dateMillis: Long?, hour: Int, minute: Int) {
        val base = dateMillis
            ?: (if (isStart) startDateMillis else endDateMillis)
            ?: System.currentTimeMillis()

        val zoned = java.time.Instant.ofEpochMilli(base)
            .atZone(java.time.ZoneId.of("America/Bogota"))
            .withHour(hour).withMinute(minute).withSecond(0)

        if (isStart) startDateMillis = zoned.toInstant().toEpochMilli()
        else         endDateMillis   = zoned.toInstant().toEpochMilli()
    }

    // ── Validación ───────────────────────────────────────────────────────

    val isFormValid: Boolean
        get() = title.value.isNotBlank() &&
                description.value.isNotBlank() &&
                _imageUris.value.isNotEmpty() &&   // al menos 1 imagen
                _selectedLocation.value != null &&
                startDateMillis != null &&
                endDateMillis   != null &&
                endDateMillis!! > startDateMillis!!

    // ── Actualización ────────────────────────────────────────────────────

    fun updateEvent() {
        val event    = currentEvent            ?: return
        val location = _selectedLocation.value ?: return
        if (!isFormValid) return

        viewModelScope.launch {
            _result.value = RequestResult.Loading
            try {
                repository.update(
                    event.copy(
                        title         = title.value.trim(),
                        description   = description.value.trim(),
                        // ✅ Convertir Uri → String en el límite ViewModel→Dominio
                        imageUris     = _imageUris.value.map { it.toString() },
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