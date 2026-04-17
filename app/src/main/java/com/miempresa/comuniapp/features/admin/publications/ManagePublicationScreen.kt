package com.miempresa.comuniapp.features.admin.publications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.VerificationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePublicationsScreen(
    initialFilter: String = "ALL",
    onNavigateBack: () -> Unit,
    onViewDetail: (String) -> Unit,
    viewModel: ManagePublicationsViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val publications by viewModel.filteredPublications.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val result       by viewModel.result.collectAsState()

    LaunchedEffect(initialFilter) {
        val filter = try {
            PublicationFilter.valueOf(initialFilter)
        } catch (e: Exception) {
            PublicationFilter.ALL
        }
        viewModel.onFilterSelected(filter)
    }

    LaunchedEffect(result) {
        if (result is RequestResult.Failure) {
            snackbarHostState.showSnackbar(
                (result as RequestResult.Failure).errorMessage
            )
            viewModel.resetResult()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Publicaciones",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FilterRow(
                activeFilter     = activeFilter,
                onFilterSelected = viewModel::onFilterSelected
            )

            when {
                result is RequestResult.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                publications.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = "No hay publicaciones",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(publications) { event ->
                            PublicationCard(
                                event    = event,
                                onDetail = { onViewDetail(event.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    activeFilter: PublicationFilter,
    onFilterSelected: (PublicationFilter) -> Unit
) {
    val filters = listOf(
        PublicationFilter.ALL      to "Todas",
        PublicationFilter.PENDING  to "Pendientes",
        PublicationFilter.APPROVED to "Verificadas",
        PublicationFilter.REJECTED to "Rechazadas"
    )

    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { (filter, label) ->
            FilterChip(
                selected = activeFilter == filter,
                onClick  = { onFilterSelected(filter) },
                label    = { Text(label) },
                colors   = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                    selectedLabelColor     = MaterialTheme.colorScheme.background
                )
            )
        }
    }
}

@Composable
private fun PublicationCard(
    event: Event,
    onDetail: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model              = event.imageUrl,
                    contentDescription = event.title,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale       = ContentScale.Crop
                )
                StatusBadge(
                    status   = event.verificationStatus,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text       = event.title,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = event.organizerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = event.startDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick  = onDetail,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver detalle")
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: VerificationStatus,
    modifier: Modifier = Modifier
) {
    val result: Pair<String, Color> = when (status) {
        VerificationStatus.PENDING  -> "Pendiente"  to Color(0xFFFFB300) // Amarillo
        VerificationStatus.APPROVED -> "Activa"     to Color(0xFF4CAF50) // Verde
        VerificationStatus.REJECTED -> "Rechazada"  to Color(0xFFF44336) // Rojo
    }
    val (label, color) = result

    Surface(
        modifier = modifier.clip(MaterialTheme.shapes.small),
        color    = color
    ) {
        Text(
            text     = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = Color.White
        )
    }
}