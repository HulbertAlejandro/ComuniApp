package com.miempresa.comuniapp.features.report.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miempresa.comuniapp.domain.model.ReportStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportListScreen(
    onReportClick: (String) -> Unit,
    viewModel: com.miempresa.comuniapp.features.report.ReportViewModel = viewModel()
) {
    val reports by viewModel.reports.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista de Reportes") }
            )
        }
    ) { padding ->
        if (reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(reports) { report ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onReportClick(report.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = report.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Text(
                                text = report.description,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tipo: ${report.type}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                
                                ReportStatusChip(report.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportStatusChip(status: ReportStatus) {
    val (color, text) = when (status) {
        ReportStatus.PENDING -> MaterialTheme.colorScheme.error to "Pendiente"
        ReportStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary to "En Progreso"
        ReportStatus.RESOLVED -> MaterialTheme.colorScheme.primary to "Resuelto"
    }
    
    Surface(
        modifier = Modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
