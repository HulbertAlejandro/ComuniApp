package com.miempresa.comuniapp.features.report.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.miempresa.comuniapp.domain.model.ReportStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    reportId: String,
    onNavigateBack: () -> Unit,
    viewModel: com.miempresa.comuniapp.features.report.ReportViewModel = hiltViewModel()
) {
    val report = remember(reportId) { viewModel.findById(reportId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Reporte") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        report?.let {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Foto del reporte
                AsyncImage(
                    model = it.photoUrl,
                    contentDescription = "Foto del reporte",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )

                // Información del reporte
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = it.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        ReportDetailItem("Descripción", it.description)
                        ReportDetailItem("Tipo", it.type)
                        ReportDetailItem("ID del Propietario", it.ownerId)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ReportDetailItem("Estado", it.status.name)
                            ReportStatusChip(it.status)
                        }
                        
                        ReportDetailItem("Ubicación", "Lat: ${it.location.latitude}, Lon: ${it.location.longitude}")
                    }
                }
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Reporte no encontrado")
            }
        }
    }
}

@Composable
private fun ReportDetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
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
