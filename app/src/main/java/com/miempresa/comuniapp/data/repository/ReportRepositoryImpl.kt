package com.miempresa.comuniapp.data.repository

import com.miempresa.comuniapp.domain.model.Location
import com.miempresa.comuniapp.domain.model.Report
import com.miempresa.comuniapp.domain.model.ReportStatus
import com.miempresa.comuniapp.domain.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor() : ReportRepository {

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    override val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    init {
        _reports.value = fetchReports()
    }

    override fun save(report: Report) {
        _reports.value += report
    }

    override fun findById(id: String): Report? {
        return _reports.value.firstOrNull { it.id == id }
    }

    private fun fetchReports(): List<Report> {
        return listOf(
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
                title = "Bache peligroso en avenida principal",
                description = "Existe un bache de gran tamaño en la avenida principal que representa un riesgo para los vehículos y peatones.",
                location = Location(40.4168, -3.7038),
                status = ReportStatus.RESOLVED,
                type = "Vialidad",
                photoUrl = "https://via.placeholder.com/300x200/2196F3/FFFFFF?text=Bache",
                ownerId = "3"
            )
        )
    }
}
