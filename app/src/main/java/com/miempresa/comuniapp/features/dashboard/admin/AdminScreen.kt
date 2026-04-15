package com.miempresa.comuniapp.features.dashboard.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Close 
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miempresa.comuniapp.core.utils.RequestResult

@Composable
fun AdminScreen(
    onLogout: () -> Unit,
    onManagePublications: (String) -> Unit = {},
    onModerationHistory: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: AdminViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val pendingCount   by viewModel.pendingCount.collectAsState()
    val rejectedCount  by viewModel.rejectedCount.collectAsState()
    val activeCount    by viewModel.activeCount.collectAsState()
    val finalizedCount by viewModel.finalizedCount.collectAsState()
    val statsResult    by viewModel.statsResult.collectAsState()

    LaunchedEffect(statsResult) {
        if (statsResult is RequestResult.Failure) {
            snackbarHostState.showSnackbar(
                (statsResult as RequestResult.Failure).errorMessage
            )
            viewModel.resetStatsResult()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AdminHeader(onProfileClick = onNavigateToProfile)

            if (statsResult is RequestResult.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                StatsGrid(
                    pendingCount   = pendingCount,
                    rejectedCount  = rejectedCount,
                    activeCount    = activeCount,
                    finalizedCount = finalizedCount,
                    onCategoryClick = onManagePublications
                )
            }

            QuickAccessSection(
                onManagePublications = { onManagePublications("ALL") },
                onModerationHistory  = onModerationHistory,
                onLogout             = onLogout
            )
        }
    }
}

@Composable
private fun AdminHeader(onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Panel de Moderación",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        IconButton(onClick = onProfileClick) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Perfil",
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun StatsGrid(
    pendingCount: Int,
    rejectedCount: Int,
    activeCount: Int,
    finalizedCount: Int,
    onCategoryClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                count    = pendingCount,
                label    = "Publicaciones\npendientes",
                icon     = Icons.Default.List,
                onClick  = { onCategoryClick("PENDING") }
            )
            StatCard(
                modifier = Modifier.weight(1f),
                count    = rejectedCount,
                label    = "Publicaciones\nrechazadas",
                icon     = Icons.Default.Close,
                onClick  = { onCategoryClick("REJECTED") }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                count    = activeCount,
                label    = "Publicaciones\nactivas",
                icon     = Icons.Default.DateRange,
                onClick  = { onCategoryClick("APPROVED") }
            )
            StatCard(
                modifier = Modifier.weight(1f),
                count    = finalizedCount,
                label    = "Finalizadas\nhoy",
                icon     = Icons.Default.CheckCircle,
                onClick  = { /* No action specified for finalized */ }
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit = {}
) {
    Card(
        modifier  = modifier,
        onClick   = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                modifier           = Modifier.size(28.dp),
                tint               = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text       = count.toString(),
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickAccessSection(
    onManagePublications: () -> Unit,
    onModerationHistory: () -> Unit,
    onLogout: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text       = "ACCESO RÁPIDO",
            style      = MaterialTheme.typography.labelMedium,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        QuickAccessItem(
            label   = "Gestionar publicaciones",
            onClick = onManagePublications
        )
        QuickAccessItem(
            label   = "Historial de moderación",
            onClick = onModerationHistory
        )

        Spacer(modifier = Modifier.height(8.dp))

        QuickAccessItem(
            label         = "Cerrar sesión",
            onClick       = onLogout,
            isDestructive = true
        )
    }
}

@Composable
private fun QuickAccessItem(
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = ">",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}