package com.miempresa.comuniapp.features.event.edit

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.notifications.NotificationSender
import com.miempresa.comuniapp.core.resources.ResourceProvider
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ValidatedField
import com.miempresa.comuniapp.domain.model.*
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.StorageRepository  // ← nueva importación
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel de la pantalla de edición y eliminación de eventos.
 *
 * Con la integración de Storage, [updateEvent] ahora:
 * 1. Clasifica cada URI como "local nueva" o "URL remota existente".
 * 2. Sube en paralelo solo las nuevas con [StorageRepository.uploadImage].
 * 3. Conserva las URLs de Firestore que el usuario no eliminó.
 * 4. Persiste el evento con la lista final de URLs en Firestore.
 */
@HiltViewModel
class EditEventViewModel @Inject constructor(
    private val repository: EventRepository,
    private val storageRepository: StorageRepository,   // ← nueva dependencia
    private val resources: ResourceProvider,
    private val notificationSender: NotificationSender
) : ViewModel() {

    private var currentEvent: Event? = null

    // ── Campos validados ─────────────────────────────────────────────────

    val title = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.edit_event_validation_title_required) else null
    }

    val description = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.edit_event_validation_description_required) else null
    }

    var category     by mutableStateOf(Category.DEPORTES)
    var maxAttendees by mutableStateOf("")
    var startDateMillis by mutableStateOf<Long?>(null)
    var endDateMillis   by mutableStateOf<Long?>(null)

    // ── Imágenes múltiples ───────────────────────────────────────────────

    private val _imageUris = MutableStateFlow<List<Uri>>(emptyList())
    val imageUris: StateFlow<List<Uri>> = _imageUris.asStateFlow()

    private var _currentCameraUri: Uri? = null

    fun createTempCameraUri(context: Context): Uri {
        val dir  = File(context.cacheDir, "event_images").also { it.mkdirs() }
        val file = File(dir, "camera_${UUID.randomUUID()}.jpg")
        val uri  = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        _currentCameraUri = uri
        return uri
    }

    fun onCameraImageCaptured(success: Boolean) {
        if (success) _currentCameraUri?.let { _imageUris.value += it }
        _currentCameraUri = null
    }

    fun onGalleryImagesSelected(uris: List<Uri>) {
        if (uris.isNotEmpty()) _imageUris.value += uris
    }

    fun removeImage(index: Int) {
        _imageUris.value = _imageUris.value
            .toMutableList()
            .also { it.removeAt(index) }
    }

    // ── Ubicación ────────────────────────────────────────────────────────

    private val _selectedLocation = MutableStateFlow<EventLocation?>(null)
    val selectedLocation: StateFlow<EventLocation?> = _selectedLocation.asStateFlow()

    val initialMapPoint: Point?
        get() = _selectedLocation.value?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        }

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
        if (currentEvent != null) return

        viewModelScope.launch {
            repository.findById(eventId)?.let { ev ->
                currentEvent        = ev
                title.onChange(ev.title)
                description.onChange(ev.description)
                category            = ev.category
                maxAttendees        = ev.maxAttendees?.toString() ?: ""
                startDateMillis     = parseDate(ev.startDate)
                endDateMillis       = parseDate(ev.endDate)
                _imageUris.value    = ev.imageUris.map { Uri.parse(it) }
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
                _imageUris.value.isNotEmpty() &&
                _selectedLocation.value != null &&
                startDateMillis != null &&
                endDateMillis   != null &&
                endDateMillis!! > startDateMillis!!

    // ── Actualización ────────────────────────────────────────────────────

    /**
     * Persiste los cambios del evento en Firestore.
     *
     * Flujo con Storage:
     * 1. Clasifica cada URI:
     *    - "https://..." → URL ya guardada en Storage, se conserva tal cual.
     *    - "content://" o "file://" → URI local nueva, se sube a Storage.
     * 2. Sube las URIs locales en paralelo con [async]/[awaitAll].
     * 3. Arma la lista final de URLs y llama a [EventRepository.update].
     * 4. Notifica al propietario del evento.
     */
    fun updateEvent() {
        val event    = currentEvent            ?: return
        val location = _selectedLocation.value ?: return
        if (!isFormValid) return

        viewModelScope.launch {
            _result.value = RequestResult.Loading
            try {
                val updatedTitle = title.value.trim()

                // ── PASO 1: resolver URLs finales ────────────────────────
                val imageUrls = uploadNewEventImages(_imageUris.value, event.id)

                // ── PASO 2: actualizar en Firestore ──────────────────────
                repository.update(
                    event.copy(
                        title         = updatedTitle,
                        description   = description.value.trim(),
                        imageUris     = imageUrls,          // ← URLs definitivas
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

                // ── PASO 3: notificación push ────────────────────────────
                runCatching {
                    notificationSender.enviar(
                        destinatarioId = event.ownerId,
                        tipo           = NotificationType.NEW_COMMENT,
                        titulo         = "✏️ Evento editado",
                        cuerpo         = "El evento \"$updatedTitle\" ha sido modificado con éxito.",
                        relatedEventId = event.id
                    )
                }.onFailure { e ->
                    Log.e("EditEventVM", "Error al notificar edición: ${e.message}")
                }

            } catch (e: Exception) {
                _result.value = RequestResult.Failure(
                    e.message ?: resources.getString(R.string.edit_event_update_failure)
                )
            }
        }
    }

    /**
     * Clasifica y procesa las URIs de imágenes del evento editado.
     *
     * - URIs HTTPS: ya son URLs de Storage del evento original → se conservan.
     * - URIs locales: imágenes nuevas agregadas en esta sesión → se suben.
     *
     * Usa [eventId] en la ruta para organizar las imágenes por evento
     * y evitar colisiones entre distintos eventos.
     *
     * @param uris    Lista actual de URIs en el ViewModel.
     * @param eventId ID del evento en Firestore (ya existe al editar).
     * @return Lista de URLs HTTPS listas para guardar en Firestore.
     */
    private suspend fun uploadNewEventImages(uris: List<Uri>, eventId: String): List<String> = kotlinx.coroutines.coroutineScope {
        uris.map { uri ->
            async {
                val uriString = uri.toString()
                if (uriString.startsWith("https")) {
                    // URL ya persistida en Storage: no se vuelve a subir
                    uriString
                } else {
                    // URI local nueva: subir a Storage bajo la carpeta del evento
                    val fileName = "event_images/$eventId/${UUID.randomUUID()}.jpg"
                    storageRepository.uploadImage(uri, fileName)
                }
            }
        }.awaitAll()
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

    // ── Helpers privados ─────────────────────────────────────────────────

    private fun formatDate(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

    private fun parseDate(date: String): Long? = try {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(date)?.time
    } catch (e: Exception) { null }
}