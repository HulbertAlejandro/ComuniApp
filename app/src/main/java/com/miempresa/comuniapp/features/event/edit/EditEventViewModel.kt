package com.miempresa.comuniapp.features.event.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val repository: EventRepository
) : ViewModel() {

    // =========================
    // STATE
    // =========================

    private var currentEvent: Event? = null

    val title = ValidatedField("") {
        if (it.isBlank()) "Título obligatorio" else null
    }

    val description = ValidatedField("") {
        if (it.isBlank()) "Descripción obligatoria" else null
    }

    var category by mutableStateOf(Category.DEPORTES)

    var maxAttendees by mutableStateOf("")

    var startDateMillis by mutableStateOf<Long?>(null)
    var endDateMillis by mutableStateOf<Long?>(null)

    // =========================
    // RESULT
    // =========================

    private val _result = MutableStateFlow<RequestResult?>(null)
    val result: StateFlow<RequestResult?> = _result.asStateFlow()

    // =========================
    // LOAD EVENT
    // =========================

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            val event = repository.findById(eventId)

            event?.let {
                currentEvent = it

                title.onChange(it.title)
                description.onChange(it.description)
                category = it.category
                maxAttendees = it.maxAttendees?.toString() ?: ""

                startDateMillis = parseDate(it.startDate)
                endDateMillis = parseDate(it.endDate)
            }
        }
    }

    // =========================
    // VALIDATION
    // =========================

    val isFormValid: Boolean
        get() {
            val max = maxAttendees.toIntOrNull()
            val validMax = max == null || max > 0

            val validDates =
                startDateMillis != null &&
                        endDateMillis != null &&
                        endDateMillis!! > startDateMillis!!

            return title.isValid &&
                    description.isValid &&
                    validMax &&
                    validDates
        }

    // =========================
    // UPDATE
    // =========================

    fun updateEvent() {
        val event = currentEvent ?: return

        if (!isFormValid) return

        viewModelScope.launch {
            _result.value = RequestResult.Loading

            try {
                val updatedEvent = event.copy(
                    title = title.value,
                    description = description.value,
                    category = category,
                    maxAttendees = maxAttendees.toIntOrNull(),
                    startDate = formatDate(startDateMillis!!),
                    endDate = formatDate(endDateMillis!!)
                )

                repository.update(updatedEvent)

                _result.value = RequestResult.Success("Evento actualizado")

            } catch (e: Exception) {
                _result.value = RequestResult.Failure(
                    e.message ?: "Error al actualizar"
                )
            }
        }
    }

    fun resetResult() {
        _result.value = null
    }

    // =========================
    // DATE HELPERS
    // =========================

    private fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun parseDate(date: String): Long? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sdf.parse(date)?.time
        } catch (e: Exception) {
            null
        }
    }
}