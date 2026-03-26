package com.miempresa.comuniapp.features.report

import androidx.lifecycle.ViewModel
import com.miempresa.comuniapp.domain.model.Report
import com.miempresa.comuniapp.domain.model.ReportStatus
import com.miempresa.comuniapp.domain.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: ReportRepository
) : ViewModel() {

    val reports: StateFlow<List<Report>> = repository.reports

    fun findById(id: String): Report? {
        return repository.findById(id)
    }

    fun save(report: Report) {
        repository.save(report)
    }
}
