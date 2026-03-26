package com.miempresa.comuniapp.domain.repository

import com.miempresa.comuniapp.domain.model.Report
import kotlinx.coroutines.flow.StateFlow

interface ReportRepository {
    val reports: StateFlow<List<Report>>
    fun save(report: Report)
    fun findById(id: String): Report?
}
