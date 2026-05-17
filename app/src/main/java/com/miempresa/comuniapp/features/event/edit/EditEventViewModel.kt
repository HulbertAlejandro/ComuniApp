package com.miempresa.comuniapp.features.event.edit

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel de la pantalla de edición y eliminación de eventos.
 *
 * Responsabilidades:
 * - Cargar el evento existente desde Firestore por su ID.
 * - Pre-poblar los campos editables con los datos actuales del evento.
 * - Gestionar la edición de imágenes (añadir cámara/galería, eliminar existentes).
 * - Persistir los cambios o eliminar el evento en Firestore.
 *
 * @param repository Repositorio de eventos que persiste en Firestore.
 * @param resources  Proveedor de strings localizados.
 */
@HiltViewModel
class EditEventViewModel @Inject constructor(
    private val repository: EventRepository,
    private val resources: ResourceProvider
) : ViewModel() {

    /** Evento actualmente en edición; null hasta que [loadEvent] lo cargue. */
    private var currentEvent: Event? = null

    // ── Campos validados ─────────────────────────────────────────────────

    /** Título del evento: no puede estar vacío. */
    val title = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.edit_event_validation_title_required) else null
    }

    /** Descripción del evento: no puede estar vacía. */
    val description = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.edit_event_validation_description_required) else null
    }

    /** Categoría seleccionada; se inicializa con el valor del evento cargado. */
    var category by mutableStateOf(Category.DEPORTES)

    /** Capacidad máxima como String para el campo de texto; puede ser vacío. */
    var maxAttendees by mutableStateOf("")

    /** Timestamp de inicio en milisegundos; null hasta que el evento cargue. */
    var startDateMillis by mutableStateOf<Long?>(null)

    /** Timestamp de fin en milisegundos; null hasta que el evento cargue. */
    var endDateMillis by mutableStateOf<Long?>(null)

    // ── Imágenes múltiples ───────────────────────────────────────────────

    /**
     * Lista de URIs de imágenes del evento.
     * Al cargar el evento: String → Uri.
     * Al guardar: Uri → String de vuelta al dominio.
     */
    private val _imageUris = MutableStateFlow<List<Uri>>(emptyList())
    val imageUris: StateFlow<List<Uri>> = _imageUris.asStateFlow()

    /** URI temporal del disparo de cámara actual. */
    private var _currentCameraUri: Uri? = null

    /**
     * Genera un archivo temporal único en caché para cada foto de cámara.
     * Debe llamarse inmediatamente antes de lanzar el TakePicture launcher.
     *
     * @param context Contexto para acceder a cacheDir y packageName.
     * @return URI expuesta via FileProvider.
     */
    fun createTempCameraUri(context: Context): Uri {
        val dir  = File(context.cacheDir, "event_images").also { it.mkdirs() }
        val file = File(dir, "camera_${UUID.randomUUID()}.jpg")
        val uri  = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        _currentCameraUri = uri
        return uri
    }

    /**
     * Llamado cuando TakePicture devuelve resultado.
     * Solo agrega la URI si la foto fue confirmada.
     *
     * @param success true si la foto fue capturada exitosamente.
     */
    fun onCameraImageCaptured(success: Boolean) {
        if (success) _currentCameraUri?.let { _imageUris.value += it }
        _currentCameraUri = null
    }

    /**
     * Agrega las URIs de galería a la lista actual.
     *
     * @param uris Lista devuelta por el selector de galería.
     */
    fun onGalleryImagesSelected(uris: List<Uri>) {
        if (uris.isNotEmpty()) _imageUris.value += uris
    }

    /**
     * Elimina la imagen en la posición [index].
     *
     * @param index Posición de la imagen a eliminar.
     */
    fun removeImage(index: Int) {
        _imageUris.value = _imageUris.value
            .toMutableList()
            .also { it.removeAt(index) }
    }

    // ── Ubicación ────────────────────────────────────────────────────────

    private val _selectedLocation = MutableStateFlow<EventLocation?>(null)
    val selectedLocation: StateFlow<EventLocation?> = _selectedLocation.asStateFlow()

    /**
     * Punto inicial del mapa calculado desde la ubicación almacenada del evento.
     * Retorna null si el evento aún no ha cargado.
     */
    val initialMapPoint: Point?
        get() = _selectedLocation.value?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        }

    /**
     * Actualiza la ubicación del evento con el punto tocado en el mapa.
     *
     * @param point Punto geográfico seleccionado en Mapbox.
     */
    fun onMapPointSelected(point: Point) {
        _selectedLocation.value = EventLocation(
            latitude  = point.latitude(),
            longitude = point.longitude()
        )
    }

    // ── Resultado ────────────────────────────────────────────────────────

    /** Estado del proceso de guardado/eliminación expuesto a la UI. */
    private val _result = MutableStateFlow<RequestResult?>(null)
    val result: StateFlow<RequestResult?> = _result.asStateFlow()

    // ── Carga del evento ─────────────────────────────────────────────────

    /**
     * Carga el evento desde Firestore y pre-popula todos los campos editables.
     * Incluye una guarda para evitar recargas en recomposiciones del Composable.
     *
     * @param eventId ID del documento Firestore a cargar.
     */
    fun loadEvent(eventId: String) {
        if (currentEvent != null) return // Evita recargas innecesarias

        viewModelScope.launch {
            repository.findById(eventId)?.let { ev ->
                currentEvent = ev
                title.onChange(ev.title)
                description.onChange(ev.description)
                category        = ev.category
                maxAttendees    = ev.maxAttendees?.toString() ?: ""
                startDateMillis = parseDate(ev.startDate)
                endDateMillis   = parseDate(ev.endDate)

                // Pre-cargar las URIs existentes convirtiendo String → Uri
                _imageUris.value    = ev.imageUris.map { Uri.parse(it) }
                // Pre-cargar la ubicación almacenada
                _selectedLocation.value = ev.eventLocation
            }
        }
    }

    /**
     * Combina una fecha del DatePicker con una hora del TimePicker.
     *
     * @param isStart    true para actualizar la fecha de inicio; false para la de fin.
     * @param dateMillis Timestamp del día seleccionado.
     * @param hour       Hora seleccionada.
     * @param minute     Minuto seleccionado.
     */
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

    /** Retorna true solo si todos los campos obligatorios son válidos para guardar. */
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
     * Flujo:
     * 1. Valida el formulario y que el evento esté cargado.
     * 2. Emite [RequestResult.Loading].
     * 3. Convierte las URIs a String en el límite ViewModel → Dominio.
     * 4. Llama a [EventRepository.update] y emite [RequestResult.Success].
     * 5. Cualquier excepción emite [RequestResult.Failure].
     */
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

    /**
     * Elimina el evento actual de Firestore.
     *
     * Flujo:
     * 1. Emite [RequestResult.Loading].
     * 2. Llama a [EventRepository.delete] con el ID del evento actual.
     * 3. Emite [RequestResult.Success] para que la UI navegue hacia atrás.
     * 4. Cualquier excepción emite [RequestResult.Failure].
     */
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

    /** Reinicia el estado del resultado. */
    fun resetResult() { _result.value = null }

    // ── Helpers privados ──────────────────────────────────────────────────

    /** Formatea un timestamp en milisegundos al formato "yyyy-MM-dd HH:mm". */
    private fun formatDate(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

    /** Parsea una fecha en formato "yyyy-MM-dd HH:mm" a timestamp en milisegundos. */
    private fun parseDate(date: String): Long? = try {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(date)?.time
    } catch (e: Exception) { null }
}