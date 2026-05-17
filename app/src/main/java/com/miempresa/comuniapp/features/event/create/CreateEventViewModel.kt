package com.miempresa.comuniapp.features.event.create

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel de la pantalla de creación de eventos.
 *
 * Responsabilidades:
 * - Gestionar los campos del formulario con validación reactiva.
 * - Manejar la selección de imágenes desde cámara o galería.
 * - Capturar la ubicación seleccionada en el mapa.
 * - Delegar la persistencia al [EventRepository] (Firestore asigna el ID).
 * - Leer el ID y nombre del organizador desde [SessionDataStore].
 *
 * @param repository       Repositorio de eventos que persiste en Firestore.
 * @param sessionDataStore Almacén local de la sesión del usuario autenticado.
 * @param resources        Proveedor de strings localizados.
 */
@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val repository: EventRepository,
    private val sessionDataStore: SessionDataStore,
    private val resources: ResourceProvider
) : ViewModel() {

    private val _ownerId       = MutableStateFlow<String?>(null)
    private val _organizerName = MutableStateFlow<String?>(null)
    private val dateFormatter  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    init {
        // Observa la sesión activa para obtener el ID y nombre del organizador
        viewModelScope.launch {
            sessionDataStore.sessionFlow.collect { session ->
                _ownerId.value       = session?.userId ?: "user_test_123"
                _organizerName.value = session?.name   ?: "Usuario Anónimo"
            }
        }
    }

    // ── Campos con validación ────────────────────────────────────────────

    /** Título del evento: no puede estar vacío. */
    val title = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.validation_error_title_required) else null
    }

    /** Descripción del evento: no puede estar vacía. */
    val description = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.validation_error_description_required) else null
    }

    /** Capacidad máxima: debe ser un número entero positivo. */
    val maxAttendees = ValidatedField("") {
        it.toIntOrNull()
            ?.let { num -> if (num <= 0) resources.getString(R.string.validation_error_max_attendees_min) else null }
            ?: resources.getString(R.string.validation_error_max_attendees_invalid)
    }

    // ── Imágenes múltiples ───────────────────────────────────────────────

    /**
     * Lista reactiva de URIs seleccionadas por el usuario.
     * Se convierte a List<String> al momento de guardar en el repositorio.
     */
    private val _selectedImageUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImageUris: StateFlow<List<Uri>> = _selectedImageUris.asStateFlow()

    /** URI temporal del archivo de cámara en curso. Se limpia tras cada captura. */
    private var _currentCameraUri: Uri? = null

    /**
     * Crea un archivo temporal en caché y retorna su URI via FileProvider.
     * Debe llamarse justo antes de lanzar el launcher de cámara.
     *
     * @param context Contexto de la Activity para acceder a cacheDir y packageName.
     * @return URI expuesta via FileProvider para que la cámara escriba la foto.
     */
    fun createTempCameraUri(context: Context): Uri {
        val imagesDir = File(context.cacheDir, "event_images").also { it.mkdirs() }
        val tempFile  = File(imagesDir, "camera_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        _currentCameraUri = uri
        return uri
    }

    /**
     * Llamado cuando TakePicture devuelve resultado.
     * Solo agrega la URI a la lista si la foto fue confirmada por el usuario.
     *
     * @param success true si la foto fue guardada exitosamente.
     */
    fun onCameraImageCaptured(success: Boolean) {
        if (success) {
            _currentCameraUri?.let { _selectedImageUris.value += it }
        }
        _currentCameraUri = null
    }

    /**
     * Agrega las URIs devueltas por el selector de galería a la lista actual.
     *
     * @param uris Lista de URIs seleccionadas; se ignora si está vacía.
     */
    fun onGalleryImagesSelected(uris: List<Uri>) {
        if (uris.isNotEmpty()) _selectedImageUris.value += uris
    }

    /**
     * Elimina la imagen en la posición [index] de la lista.
     *
     * @param index Posición de la imagen a eliminar.
     */
    fun removeImage(index: Int) {
        _selectedImageUris.value = _selectedImageUris.value
            .toMutableList()
            .also { it.removeAt(index) }
    }

    // ── Ubicación ────────────────────────────────────────────────────────

    private val _selectedLocation = MutableStateFlow<EventLocation?>(null)
    val selectedLocation: StateFlow<EventLocation?> = _selectedLocation.asStateFlow()

    /**
     * Actualiza la ubicación del evento con el punto tocado en el mapa.
     *
     * @param point Punto geográfico seleccionado en Mapbox.
     */
    fun onMapPointSelected(point: com.mapbox.geojson.Point) {
        _selectedLocation.value = EventLocation(
            latitude  = point.latitude(),
            longitude = point.longitude()
        )
    }

    // ── Categoría ────────────────────────────────────────────────────────

    /** Categoría seleccionada por el usuario; null hasta que elija una. */
    var selectedCategory by mutableStateOf<Category?>(null)

    /** Actualiza la categoría seleccionada desde el diálogo de selección. */
    fun onCategorySelected(category: Category) { selectedCategory = category }

    // ── Fechas ───────────────────────────────────────────────────────────

    /** Timestamp en milisegundos de la fecha/hora de inicio. */
    var startDateMillis by mutableStateOf<Long?>(null)

    /** Timestamp en milisegundos de la fecha/hora de fin. */
    var endDateMillis by mutableStateOf<Long?>(null)

    /**
     * Combina una fecha seleccionada en el DatePicker con una hora del TimePicker.
     *
     * @param isStart   true si se actualiza la fecha de inicio; false para la de fin.
     * @param dateMillis Timestamp del día seleccionado (puede ser null si no cambió).
     * @param hour      Hora seleccionada en el TimePicker.
     * @param minute    Minuto seleccionado en el TimePicker.
     */
    fun updateDateTime(isStart: Boolean, dateMillis: Long?, hour: Int, minute: Int) {
        val base = dateMillis
            ?: (if (isStart) startDateMillis else endDateMillis)
            ?: System.currentTimeMillis()

        val zoned = Instant.ofEpochMilli(base)
            .atZone(ZoneId.of("America/Bogota"))
            .withHour(hour).withMinute(minute).withSecond(0)

        if (isStart) startDateMillis = zoned.toInstant().toEpochMilli()
        else         endDateMillis   = zoned.toInstant().toEpochMilli()
    }

    // ── Resultado ────────────────────────────────────────────────────────

    /** Estado del proceso de creación expuesto a la UI. */
    private val _result = MutableStateFlow<RequestResult?>(null)
    val result: StateFlow<RequestResult?> = _result.asStateFlow()

    // ── Validación ───────────────────────────────────────────────────────

    /**
     * Retorna true solo si todos los campos obligatorios están correctamente diligenciados.
     * Se evalúa en cada recomposición gracias a que los campos son observables.
     */
    val isFormValid: Boolean
        get() = title.value.isNotBlank() &&
                description.value.isNotBlank() &&
                _selectedImageUris.value.isNotEmpty() &&
                (maxAttendees.value.toIntOrNull()?.let { it > 0 } ?: false) &&
                _selectedLocation.value != null &&
                startDateMillis != null &&
                endDateMillis   != null &&
                endDateMillis!! > startDateMillis!! &&
                selectedCategory != null

    // ── Creación ─────────────────────────────────────────────────────────

    /**
     * Persiste el nuevo evento en Firestore.
     *
     * Flujo:
     * 1. Valida que el formulario sea correcto y que haya sesión activa.
     * 2. Emite [RequestResult.Loading].
     * 3. Construye el objeto [Event] con id vacío (Firestore lo asigna en el repositorio).
     * 4. Convierte las URIs locales a String en el límite ViewModel → Dominio.
     * 5. Llama a [EventRepository.save] y emite [RequestResult.Success].
     * 6. Cualquier excepción emite [RequestResult.Failure].
     */
    fun createEvent() {
        val owner    = _ownerId.value         ?: return
        val location = _selectedLocation.value ?: return
        if (!isFormValid) return

        viewModelScope.launch {
            _result.value = RequestResult.Loading
            try {
                val start = Instant.ofEpochMilli(startDateMillis!!)
                    .atZone(ZoneId.systemDefault())
                val end   = Instant.ofEpochMilli(endDateMillis!!)
                    .atZone(ZoneId.systemDefault())

                repository.save(
                    Event(
                        // ✅ id vacío: Firestore lo genera en EventRepositoryImpl.save()
                        id            = "",
                        title         = title.value.trim(),
                        description   = description.value.trim(),
                        category      = selectedCategory!!,
                        // ✅ Conversión Uri → String en el límite ViewModel → Dominio
                        imageUris     = _selectedImageUris.value.map { it.toString() },
                        eventLocation = location,
                        startDate     = start.format(dateFormatter),
                        endDate       = end.format(dateFormatter),
                        maxAttendees  = maxAttendees.value.toIntOrNull(),
                        ownerId       = owner,
                        organizerName = _organizerName.value
                            ?: resources.getString(R.string.default_organizer_name),
                        eventStatus        = EventStatus.CREATED,
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

    /** Reinicia todos los campos del formulario a sus valores iniciales. */
    private fun clearForm() {
        title.reset()
        description.reset()
        maxAttendees.reset()
        _selectedImageUris.value = emptyList()
        _selectedLocation.value  = null
        selectedCategory         = null
        startDateMillis          = null
        endDateMillis            = null
    }

    /** Reinicia el estado del resultado sin limpiar el formulario. */
    fun resetResult() { _result.value = null }
}