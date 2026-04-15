package com.miempresa.comuniapp.features.dashboard.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.domain.model.EventStatus
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
class AdminViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _pendingCount   = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _rejectedCount  = MutableStateFlow(0)
    val rejectedCount: StateFlow<Int> = _rejectedCount.asStateFlow()

    private val _activeCount    = MutableStateFlow(0)
    val activeCount: StateFlow<Int> = _activeCount.asStateFlow()

    private val _finalizedCount = MutableStateFlow(0)
    val finalizedCount: StateFlow<Int> = _finalizedCount.asStateFlow()

    private val _statsResult = MutableStateFlow<RequestResult?>(null)
    val statsResult: StateFlow<RequestResult?> = _statsResult.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _statsResult.value = RequestResult.Loading
            eventRepository.events.collect { events ->
                val today = LocalDate.now().format(dateFormatter)
                
                _pendingCount.value = events.count { it.verificationStatus == VerificationStatus.PENDING }
                _rejectedCount.value = events.count { it.verificationStatus == VerificationStatus.REJECTED }
                _activeCount.value = events.count { it.eventStatus == EventStatus.ACTIVE && it.verificationStatus == VerificationStatus.APPROVED }
                
                // Contador de moderaciones realizadas hoy (aprobadas o rechazadas hoy)
                _finalizedCount.value = events.count { it.moderationDate == today }

                _statsResult.value = RequestResult.Success("Estadísticas actualizadas")
            }
        }
    }

    fun resetStatsResult() {
        _statsResult.value = null
    }
}