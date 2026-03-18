package com.miempresa.comuniapp.features.dashboard.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miempresa.comuniapp.domain.model.ReportStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHomeScreen(
    onNavigateToUsers: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    // Datos de ejemplo (en una app real vendrían de ViewModels)
    val stats = remember {
        listOf(
            StatItem("Usuarios Activos", "156", Icons.Default.People, androidx.compose.ui.graphics.Color.Unspecified),
            StatItem("Reportes Totales", "89", Icons.Default.Assignment, androidx.compose.ui.graphics.Color.Unspecified),
            StatItem("Pendientes", "23", Icons.Default.Pending, androidx.compose.ui.graphics.Color.Unspecified),
            StatItem("Resueltos", "66", Icons.Default.CheckCircle, androidx.compose.ui.graphics.Color.Unspecified)
        )
    }.map { 
        it.copy(color = when(it.label) {
            "Usuarios Activos" -> MaterialTheme.colorScheme.primary
            "Reportes Totales" -> MaterialTheme.colorScheme.secondary
            "Pendientes" -> MaterialTheme.colorScheme.error
            "Resueltos" -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.primary
        })
    }

    val recentReports = remember {
        listOf(
            RecentReport("Alcantarilla tapada", ReportStatus.PENDING),
            RecentReport("Luz fundida", ReportStatus.IN_PROGRESS),
            RecentReport("Bache reparado", ReportStatus.RESOLVED)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Panel de Control",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            // Estadísticas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stats.forEach { stat ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = stat.color.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = stat.icon,
                                contentDescription = null,
                                tint = stat.color,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stat.value,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = stat.color
                            )
                            Text(
                                text = stat.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        item {
            // Acciones rápidas
            Text(
                text = "Acciones Rápidas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.People,
                    text = "Ver Usuarios",
                    onClick = onNavigateToUsers
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Assignment,
                    text = "Ver Reportes",
                    onClick = onNavigateToReports
                )
            }
        }

        item {
            // Reportes recientes
            Text(
                text = "Reportes Recientes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
        }

        items(recentReports) { report ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = report.title,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    ReportStatusChip(report.status)
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
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

data class StatItem(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color
)

data class RecentReport(
    val title: String,
    val status: ReportStatus
)
