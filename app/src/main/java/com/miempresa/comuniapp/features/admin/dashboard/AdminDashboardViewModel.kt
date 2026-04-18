package com.miempresa.comuniapp.features.admin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.domain.model.EventStatus
import com.miempresa.comuniapp.domain.model.VerificationStatus
import com.miempresa.comuniapp.domain.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class AdminStats(
    val pendingCount  : Int = 0,
    val rejectedCount : Int = 0,
    val activeCount   : Int = 0,
    val approvedCount : Int = 0
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    /**
     * StateFlow completamente reactivo: cualquier cambio en el repositorio
     * de eventos (aprobación, rechazo, creación) actualiza las estadísticas
     * automáticamente sin necesidad de llamar a loadStats() manualmente.
     */
    val stats: StateFlow<AdminStats> = eventRepository.events
        .map { events ->
            AdminStats(
                pendingCount  = events.count {
                    it.verificationStatus == VerificationStatus.PENDING
                },
                rejectedCount = events.count {
                    it.verificationStatus == VerificationStatus.REJECTED
                },
                activeCount   = events.count {
                    it.eventStatus        == EventStatus.ACTIVE &&
                            it.verificationStatus == VerificationStatus.APPROVED
                },
                approvedCount = events.count {
                    it.verificationStatus == VerificationStatus.APPROVED
                }
            )
        }
        .stateIn(
            scope   = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AdminStats()
        )
}