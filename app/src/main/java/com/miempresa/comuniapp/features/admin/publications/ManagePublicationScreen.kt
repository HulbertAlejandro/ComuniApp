package com.miempresa.comuniapp.features.admin.publications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.EventStatus
import com.miempresa.comuniapp.domain.model.VerificationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePublicationsScreen(
    onNavigateBack : () -> Unit,
    onViewDetail   : (String) -> Unit,
    bottomPadding  : PaddingValues = PaddingValues(),   // ← recibe el bottom de la BottomBar
    viewModel      : ManagePublicationsViewModel = hiltViewModel()
) {
    val publications  by viewModel.filteredPublications.collectAsState()
    val activeFilter  by viewModel.activeFilter.collectAsState()
    val organizersMap by viewModel.organizersMap.collectAsState()

    // Estado del diálogo de rechazo
    var rejectTargetId by remember { mutableStateOf<String?>(null) }
    var rejectReason   by remember { mutableStateOf("") }

    // ✅ Diálogo de rechazo
    rejectTargetId?.let { eventId ->
        AlertDialog(
            onDismissRequest = { rejectTargetId = null; rejectReason = "" },
            title   = { Text("Motivo del rechazo") },
            text    = {
                OutlinedTextField(
                    value         = rejectReason,
                    onValueChange = { rejectReason = it },
                    placeholder   = { Text("Escribe el motivo...") },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 3,
                    isError       = rejectReason.isBlank()
                )
            },
            confirmButton = {
                Button(
                    onClick  = {
                        viewModel.rejectEvent(eventId, rejectReason)
                        rejectTargetId = null
                        rejectReason   = ""
                    },
                    enabled  = rejectReason.isNotBlank(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) { Text("Rechazar") }
            },
            dismissButton = {
                TextButton(onClick = { rejectTargetId = null; rejectReason = "" }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Publicaciones",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            FilterRow(
                activeFilter     = activeFilter,
                onFilterSelected = viewModel::onFilterSelected
            )

            if (publications.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text  = "No hay publicaciones",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(
                        start  = 16.dp,
                        end    = 16.dp,
                        top    = 8.dp,
                        // ✅ La última tarjeta no queda tapada por la BottomBar
                        bottom = 16.dp + bottomPadding.calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(publications, key = { it.id }) { event ->
                        PublicationCard(
                            event         = event,
                            organizerName = organizersMap[event.ownerId] ?: "ID: ${event.ownerId}",
                            onDetail      = { onViewDetail(event.id) },
                            onApprove     = { viewModel.approveEvent(event.id) },
                            onReject      = { rejectTargetId = event.id },
                            onFinish      = { viewModel.finishEvent(event.id) }   // ← nuevo
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    activeFilter     : PublicationFilter,
    onFilterSelected : (PublicationFilter) -> Unit
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
    event         : Event,
    organizerName : String,
    onDetail      : () -> Unit,
    onApprove     : () -> Unit,
    onReject      : () -> Unit,
    onFinish      : () -> Unit                                 // ← nuevo
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                modifier            = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text       = event.title,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
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

                Spacer(modifier = Modifier.height(8.dp))

                // Siempre visible
                OutlinedButton(
                    onClick  = onDetail,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ver detalle") }

                // ── Acciones según estado ─────────────────────────────────────

                when {
                    // Pendiente: puede aprobarse o rechazarse
                    event.verificationStatus == VerificationStatus.PENDING -> {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick  = onApprove,
                                modifier = Modifier.weight(1f),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("Verificar", color = Color.White) }

                            OutlinedButton(
                                onClick  = onReject,
                                modifier = Modifier.weight(1f),
                                colors   = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFF44336)
                                ),
                                border = ButtonDefaults.outlinedButtonBorder(true).copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF44336))
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("Rechazar") }
                        }
                    }

                    // Aprobado y ACTIVO o FULL: puede finalizarse
                    event.verificationStatus == VerificationStatus.APPROVED &&
                            event.eventStatus        != EventStatus.FINISHED -> {
                        Button(
                            onClick  = onFinish,
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF546E7A)   // gris azulado — acción destructiva suave
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Finalizar evento", color = Color.White)
                        }
                    }

                    // Finalizado o rechazado: solo lectura, sin botones extra
                    else -> { /* nada */ }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status   : VerificationStatus,
    modifier : Modifier = Modifier
) {
    val (label, color) = when (status) {
        VerificationStatus.PENDING  -> "Pendiente" to Color(0xFFFFB300)
        VerificationStatus.APPROVED -> "Activa"    to Color(0xFF4CAF50)
        VerificationStatus.REJECTED -> "Rechazada" to Color(0xFFF44336)
    }
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