package com.miempresa.comuniapp.features.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.domain.model.Location
import com.miempresa.comuniapp.domain.model.Report
import com.miempresa.comuniapp.domain.model.ReportStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor() : ViewModel() {

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    init {
        fetchReports()
    }

    private fun fetchReports() {
        viewModelScope.launch {
            // Simulación de carga de datos
            delay(1000)
            _reports.value = listOf(
                Report(
                    id = "1",
                    title = "Alcantarilla tapada en calle principal",
                    description = "La alcantarilla ubicada en la esquina de la calle principal con avenida central está completamente tapada, causando inundaciones cuando llueve.",
                    location = Location(40.4168, -3.7038),
                    status = ReportStatus.PENDING,
                    type = "Mantenimiento",
                    photoUrl = "https://via.placeholder.com/300x200/4CAF50/FFFFFF?text=Alcantarilla",
                    ownerId = "1"
                ),
                Report(
                    id = "2",
                    title = "Luz de calle fundida en parque central",
                    description = "Una de las luces del parque central no funciona, lo que crea una zona oscura peligrosa por la noche.",
                    location = Location(40.4168, -3.7038),
                    status = ReportStatus.IN_PROGRESS,
                    type = "Alumbrado",
                    photoUrl = "https://via.placeholder.com/300x200/FFC107/000000?text=Luz+Fundida",
                    ownerId = "2"
                ),
                Report(
                    id = "3",
                    title = "Bache reparado en avenida principal",
                    description = "El bache que fue reportado la semana pasada ya ha sido completamente reparado por el equipo municipal.",
                    location = Location(40.4168, -3.7038),
                    status = ReportStatus.RESOLVED,
                    type = "Carreteras",
                    photoUrl = "https://via.placeholder.com/300x200/4CAF50/FFFFFF?text=Bache+Reparado",
                    ownerId = "1"
                )
            )
        }
    }

    fun findById(id: String): Report? {
        return _reports.value.find { it.id == id }
    }

    fun updateReportStatus(reportId: String, newStatus: ReportStatus) {
        val currentReports = _reports.value.toMutableList()
        val reportIndex = currentReports.indexOfFirst { it.id == reportId }
        if (reportIndex != -1) {
            currentReports[reportIndex] = currentReports[reportIndex].copy(status = newStatus)
            _reports.value = currentReports
        }
    }
}
