package com.miempresa.comuniapp.features.dashboard.admin.publications.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.VerificationStatus
import com.miempresa.comuniapp.domain.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AdminEventDetailViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private val _result = MutableStateFlow<RequestResult?>(null)
    val result: StateFlow<RequestResult?> = _result.asStateFlow()

    fun loadEvent(id: String) {
        viewModelScope.launch {
            _result.value = RequestResult.Loading
            try {
                val foundEvent = eventRepository.findById(id)
                if (foundEvent != null) {
                    _event.value = foundEvent
                    _result.value = RequestResult.Success("Cargado")
                } else {
                    _result.value = RequestResult.Failure("Evento no encontrado")
                }
            } catch (e: Exception) {
                _result.value = RequestResult.Failure(e.message ?: "Error desconocido")
            }
        }
    }

    fun verifyEvent(reason: String) {
        val currentEvent = _event.value ?: return
        viewModelScope.launch {
            _result.value = RequestResult.Loading
            try {
                val today = LocalDate.now().format(dateFormatter)
                val updatedEvent = currentEvent.copy(
                    verificationStatus = VerificationStatus.APPROVED,
                    moderationDate = today
                )
                eventRepository.update(updatedEvent)
                _event.value = updatedEvent
                _result.value = RequestResult.Success("Publicación verificada")
            } catch (e: Exception) {
                _result.value = RequestResult.Failure(e.message ?: "Error al verificar")
            }
        }
    }

    fun rejectEvent(reason: String) {
        val currentEvent = _event.value ?: return
        viewModelScope.launch {
            _result.value = RequestResult.Loading
            try {
                val today = LocalDate.now().format(dateFormatter)
                val updatedEvent = currentEvent.copy(
                    verificationStatus = VerificationStatus.REJECTED,
                    moderationDate = today,
                    rejectionReason = reason
                )
                eventRepository.update(updatedEvent)
                _event.value = updatedEvent
                _result.value = RequestResult.Success("Publicación rechazada")
            } catch (e: Exception) {
                _result.value = RequestResult.Failure(e.message ?: "Error al rechazar")
            }
        }
    }

    fun resetResult() {
        _result.value = null
    }
}
