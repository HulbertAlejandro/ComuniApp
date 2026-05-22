package com.miempresa.comuniapp.features.event.create

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.notifications.NotificationSender
import com.miempresa.comuniapp.core.resources.ResourceProvider
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.core.utils.ValidatedField
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.*
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.StorageRepository
import com.miempresa.comuniapp.domain.service.CategorySuggestionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

/** Estado del proceso de sugerencia de categoría por IA. */
sealed interface SuggestionState {
    data object Idle : SuggestionState
    data object Loading : SuggestionState
    data class Success(val category: Category) : SuggestionState
    data class Error(val message: String) : SuggestionState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val repository: EventRepository,
    private val storageRepository: StorageRepository,
    private val categorySuggestionService: CategorySuggestionService,
    private val sessionDataStore: SessionDataStore,
    private val notificationSender: NotificationSender,
    private val resources: ResourceProvider
) : ViewModel() {

    // ── 1. Propiedades de Estado y Campos Validados (Siempre primero en memoria) ──────────────────

    private val _ownerId = MutableStateFlow<String?>(null)
    private val _organizerName = MutableStateFlow<String?>(null)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    val title = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.validation_error_title_required) else null
    }

    val description = ValidatedField("") {
        if (it.isBlank()) resources.getString(R.string.validation_error_description_required) else null
    }

    val maxAttendees = ValidatedField("") {
        it.toIntOrNull()
            ?.let { num ->
                if (num <= 0) resources.getString(R.string.validation_error_max_attendees_min)
                else null
            }
            ?: resources.getString(R.string.validation_error_max_attendees_invalid)
    }

    private val _selectedImageUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImageUris: StateFlow<List<Uri>> = _selectedImageUris.asStateFlow()
    private var _currentCameraUri: Uri? = null

    private val _selectedLocation = MutableStateFlow<EventLocation?>(null)
    val selectedLocation: StateFlow<EventLocation?> = _selectedLocation.asStateFlow()

    var selectedCategory by mutableStateOf<Category?>(null)

    private val _suggestionState = MutableStateFlow<SuggestionState>(SuggestionState.Idle)
    val suggestionState: StateFlow<SuggestionState> = _suggestionState.asStateFlow()

    var startDateMillis by mutableStateOf<Long?>(null)
    var endDateMillis by mutableStateOf<Long?>(null)

    private val _result = MutableStateFlow<RequestResult?>(null)
    val result: StateFlow<RequestResult?> = _result.asStateFlow()

    // ── 2. Bloque de Inicialización (Ahora lee variables ya creadas con seguridad) ───────────────

    init {
        viewModelScope.launch {
            sessionDataStore.sessionFlow.collect { session ->
                _ownerId.value = session?.userId ?: "user_test_123"
                _organizerName.value = session?.name ?: "Usuario Anónimo"
            }
        }

        // ── Pipeline reactivo de sugerencia IA con soporte de cancelación nativa ──────────────────────────
        snapshotFlow { title.value to description.value }
            .debounce(2000L) // Pausa prudente del teclado
            .map { (t, d) -> t.trim() to d.trim() }
            .distinctUntilChanged() // Evita repetir llamadas si el texto final no varió
            .flatMapLatest { (t, d) ->
                // flatMapLatest cancela automáticamente el bloque previo si llega una nueva emisión
                flow {
                    if (t.length >= 5 && d.length >= 30) {
                        emit(SuggestionState.Loading)
                        try {
                            val suggested = categorySuggestionService.suggestCategory(t, d)
                            if (suggested != null) {
                                emit(SuggestionState.Success(suggested))
                            } else {
                                emit(SuggestionState.Idle)
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            emit(SuggestionState.Error(e.message ?: "Error al obtener sugerencia"))
                        }
                    } else {
                        emit(SuggestionState.Idle)
                    }
                }
            }
            .onEach { state ->
                _suggestionState.value = state
            }
            .launchIn(viewModelScope)
    }

    // ── 3. Funciones y Lógica de Negocio ─────────────────────────────────────────────────────────

    fun createTempCameraUri(context: Context): Uri {
        val imagesDir = File(context.cacheDir, "event_images").also { it.mkdirs() }
        val tempFile = File(imagesDir, "camera_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", tempFile
        )
        _currentCameraUri = uri
        return uri
    }

    fun onCameraImageCaptured(success: Boolean) {
        if (success) _currentCameraUri?.let { _selectedImageUris.value += it }
        _currentCameraUri = null
    }

    fun onGalleryImagesSelected(uris: List<Uri>) {
        if (uris.isNotEmpty()) _selectedImageUris.value += uris
    }

    fun removeImage(index: Int) {
        _selectedImageUris.value = _selectedImageUris.value
            .toMutableList().also { it.removeAt(index) }
    }

    fun onMapPointSelected(point: com.mapbox.geojson.Point) {
        _selectedLocation.value = EventLocation(
            latitude = point.latitude(),
            longitude = point.longitude()
        )
    }

    fun onCategorySelected(category: Category) {
        selectedCategory = category
        dismissSuggestion()
    }

    fun acceptSuggestion() {
        val state = _suggestionState.value
        if (state is SuggestionState.Success) {
            selectedCategory = state.category
            _suggestionState.value = SuggestionState.Idle
        }
    }

    fun dismissSuggestion() {
        _suggestionState.value = SuggestionState.Idle
    }

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

    val isFormValid: Boolean
        get() = title.value.isNotBlank() &&
                description.value.isNotBlank() &&
                _selectedImageUris.value.isNotEmpty() &&
                (maxAttendees.value.toIntOrNull()?.let { it > 0 } ?: false) &&
                _selectedLocation.value != null &&
                startDateMillis != null &&
                endDateMillis != null &&
                endDateMillis!! > startDateMillis!! &&
                selectedCategory != null

    fun createEvent() {
        val owner = _ownerId.value ?: return
        val location = _selectedLocation.value ?: return
        if (!isFormValid) return

        viewModelScope.launch {
            _result.value = RequestResult.Loading
            try {
                val start = Instant.ofEpochMilli(startDateMillis!!)
                    .atZone(ZoneId.systemDefault())
                val end = Instant.ofEpochMilli(endDateMillis!!)
                    .atZone(ZoneId.systemDefault())

                val eventTitleClean = title.value.trim()
                val imageUrls = uploadEventImages(_selectedImageUris.value)

                val nuevoEventId = repository.save(
                    Event(
                        id = "",
                        title = eventTitleClean,
                        description = description.value.trim(),
                        category = selectedCategory!!,
                        imageUris = imageUrls,
                        eventLocation = location,
                        startDate = start.format(dateFormatter),
                        endDate = end.format(dateFormatter),
                        maxAttendees = maxAttendees.value.toIntOrNull(),
                        ownerId = owner,
                        organizerName = _organizerName.value
                            ?: resources.getString(R.string.default_organizer_name),
                        eventStatus = EventStatus.CREATED,
                        verificationStatus = VerificationStatus.PENDING
                    )
                )

                runCatching {
                    notificationSender.enviar(
                        destinatarioId = owner,
                        tipo = NotificationType.EVENT_UNDER_REVIEW,
                        titulo = "📋 Evento enviado a revisión",
                        cuerpo = "\"$eventTitleClean\" está siendo revisado.",
                        relatedEventId = nuevoEventId
                    )
                }.onFailure { e ->
                    android.util.Log.e("CreateEventVM", "Error al notificar: ${e.message}")
                }

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

    private suspend fun uploadEventImages(uris: List<Uri>): List<String> =
        kotlinx.coroutines.coroutineScope {
            uris.map { uri ->
                async {
                    val s = uri.toString()
                    if (s.startsWith("https")) s
                    else storageRepository.uploadImage(uri, "event_images/${UUID.randomUUID()}.jpg")
                }
            }.awaitAll()
        }

    private fun clearForm() {
        title.reset()
        description.reset()
        maxAttendees.reset()
        _selectedImageUris.value = emptyList()
        _selectedLocation.value = null
        selectedCategory = null
        startDateMillis = null
        endDateMillis = null
        _suggestionState.value = SuggestionState.Idle
    }

    fun resetResult() { _result.value = null }
}