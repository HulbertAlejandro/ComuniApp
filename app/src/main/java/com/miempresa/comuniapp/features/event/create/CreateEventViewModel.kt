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

    // ── Campos con validación ────────────────────────────────────────────

    val title = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.validation_error_title_required) else null
    }

    val description = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.validation_error_description_required) else null
    }

    val maxAttendees = ValidatedField("") {
        it.toIntOrNull()
            ?.let { num -> if (num <= 0) resources.getString(R.string.validation_error_max_attendees_min) else null }
            ?: resources.getString(R.string.validation_error_max_attendees_invalid)
    }

    // ── Imágenes múltiples ───────────────────────────────────────────────

    /**
     * Lista reactiva de URIs seleccionadas (como String para la UI).
     * Se convierte a List<String> al guardar en el repositorio.
     */
    private val _selectedImageUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImageUris: StateFlow<List<Uri>> = _selectedImageUris.asStateFlow()

    /**
     * URI temporal para la foto de cámara ACTUAL.
     * Se genera antes de lanzar el intent; se reutiliza el mismo
     * slot hasta que el usuario confirma la foto.
     */
    private var _currentCameraUri: Uri? = null

    /**
     * Crea (o reutiliza) un archivo temporal en cache y devuelve su URI
     * via FileProvider. Llama a esto justo antes de lanzar [cameraLauncher].
     *
     * Patrón:
     *   1. Crear File vacío en cacheDir/images/
     *   2. Exponer via FileProvider (authority = "${packageName}.provider")
     *   3. Pasar URI al TakePicture contract
     *   4. En onResult(true) → agregar a _selectedImageUris
     */
    fun createTempCameraUri(context: Context): Uri {
        val imagesDir = File(context.cacheDir, "event_images").also { it.mkdirs() }
        // Nombre único por disparo para soportar múltiples fotos de cámara
        val tempFile = File(imagesDir, "camera_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        _currentCameraUri = uri
        return uri
    }

    /** Llamado cuando TakePicture devuelve true (foto guardada). */
    fun onCameraImageCaptured(success: Boolean) {
        if (success) {
            _currentCameraUri?.let { uri ->
                _selectedImageUris.value += uri
            }
        }
        // Si falla, descartamos silenciosamente; el archivo temporal
        // se limpiará junto con el caché del sistema.
        _currentCameraUri = null
    }

    /** Llamado desde el launcher de galería (puede ser múltiple). */
    fun onGalleryImagesSelected(uris: List<Uri>) {
        if (uris.isNotEmpty()) {
            _selectedImageUris.value += uris
        }
    }

    /** Elimina una imagen de la lista por índice. */
    fun removeImage(index: Int) {
        _selectedImageUris.value = _selectedImageUris.value
            .toMutableList()
            .also { it.removeAt(index) }
    }

    // ── Ubicación ────────────────────────────────────────────────────────

    private val _selectedLocation = MutableStateFlow<EventLocation?>(null)
    val selectedLocation: StateFlow<EventLocation?> = _selectedLocation.asStateFlow()

    fun onMapPointSelected(point: com.mapbox.geojson.Point) {
        _selectedLocation.value = EventLocation(
            latitude = point.latitude(),
            longitude = point.longitude()
        )
    }

    // ── Categoría ────────────────────────────────────────────────────────

    var selectedCategory by mutableStateOf<Category?>(null)
    fun onCategorySelected(category: Category) { selectedCategory = category }

    // ── Fechas ───────────────────────────────────────────────────────────

    var startDateMillis by mutableStateOf<Long?>(null)
    var endDateMillis by mutableStateOf<Long?>(null)

    fun updateDateTime(isStart: Boolean, dateMillis: Long?, hour: Int, minute: Int) {
        val base = dateMillis
            ?: (if (isStart) startDateMillis else endDateMillis)
            ?: System.currentTimeMillis()

        val zoned = Instant.ofEpochMilli(base)
            .atZone(ZoneId.of("America/Bogota"))
            .withHour(hour).withMinute(minute).withSecond(0)

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
                _selectedImageUris.value.isNotEmpty() &&   // al menos 1 imagen
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
                        id            = UUID.randomUUID().toString(),
                        title         = title.value.trim(),
                        description   = description.value.trim(),
                        category      = selectedCategory!!,
                        // ✅ Convertir Uri → String aquí, en el límite ViewModel→Dominio
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

    fun resetResult() { _result.value = null }
}