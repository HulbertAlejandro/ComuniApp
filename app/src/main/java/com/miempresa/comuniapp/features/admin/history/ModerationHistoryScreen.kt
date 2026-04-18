package com.miempresa.comuniapp.features.admin.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.VerificationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationHistoryScreen(
    onNavigateBack: () -> Unit,
    // ✅ Agregamos el parámetro que faltaba
    bottomPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: ModerationHistoryViewModel = hiltViewModel()
) {
    val searchText by viewModel.searchText.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val events by viewModel.filteredEvents.collectAsState()
    val organizersMap by viewModel.organizersMap.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = viewModel::onSearchTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )

            FilterRow(
                activeFilter = activeFilter,
                onFilterSelected = viewModel::onFilterSelected
            )

            if (events.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay publicaciones", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    // ✅ Combinamos el padding de 16.dp con el padding de la barra inferior
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp + bottomPadding.calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(events, key = { it.id }) { event ->
                        HistoryCard(
                            event = event,
                            organizerName = organizersMap[event.ownerId] ?: "ID: ${event.ownerId}",
                            currentFilter = activeFilter
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    activeFilter     : HistoryFilter,
    onFilterSelected : (HistoryFilter) -> Unit
) {
    val filters = listOf(
        HistoryFilter.ALL      to "Todas",
        HistoryFilter.VERIFIED to "Verificadas",
        HistoryFilter.REJECTED to "Rechazadas"
    )
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { (filter, label) ->
            FilterChip(
                selected = activeFilter == filter,
                onClick  = { onFilterSelected(filter) },
                label    = { Text(label) }
            )
        }
    }
}

@Composable
private fun HistoryCard(
    event         : Event,
    organizerName : String,
    currentFilter : HistoryFilter
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (currentFilter == HistoryFilter.ALL || currentFilter == HistoryFilter.REJECTED) {
                StatusBadge(status = event.verificationStatus)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text       = event.title,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            // ✅ nombre resuelto desde el mapa
            Text(
                text  = organizerName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text  = event.startDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (currentFilter == HistoryFilter.REJECTED &&
                !event.rejectionReason.isNullOrBlank()
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color    = MaterialTheme.colorScheme.errorContainer,
                    shape    = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text       = "MOTIVO DEL RECHAZO:",
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text  = event.rejectionReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: VerificationStatus) {
    val (label, color, icon) = when (status) {
        VerificationStatus.APPROVED -> Triple("VERIFICADO", MaterialTheme.colorScheme.primary,   Icons.Default.CheckCircle)
        VerificationStatus.REJECTED -> Triple("RECHAZADO",  MaterialTheme.colorScheme.error,     Icons.Default.Cancel)
        VerificationStatus.PENDING  -> Triple("PENDIENTE",  MaterialTheme.colorScheme.tertiary,  Icons.Default.Search)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium,
            color = color, fontWeight = FontWeight.Bold)
    }
}