package com.miempresa.comuniapp.features.admin.publications.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.domain.model.VerificationStatus

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEventDetailScreen(
    eventId: String,
    onNavigateBack: () -> Unit,
    viewModel: AdminEventDetailViewModel = hiltViewModel()
) {
    val event by viewModel.event.collectAsState()
    val result by viewModel.result.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var isApproving by remember { mutableStateOf(true) }
    var reason by remember { mutableStateOf("") }

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    LaunchedEffect(result) {
        if (result is RequestResult.Success) {
            val message = (result as RequestResult.Success).message
            if (message == "Publicación verificada" || message == "Publicación rechazada") {
                snackbarHostState.showSnackbar(message)
                onNavigateBack() // Opcional: volver tras acción
            }
        } else if (result is RequestResult.Failure) {
            snackbarHostState.showSnackbar((result as RequestResult.Failure).errorMessage)
            viewModel.resetResult()
        }
    }

    if (showConfirmDialog) {
        Dialog(onDismissRequest = { showConfirmDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isApproving) "Confirmar Aprobación" else "Confirmar Rechazo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text(if (isApproving) "Razón / Comentario (Opcional)" else "Motivo del rechazo (Obligatorio)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        isError = !isApproving && reason.isBlank()
                    )

                    if (!isApproving) {
                        Text(
                            text = "* El usuario recibirá una notificación con este motivo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showConfirmDialog = false }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (isApproving) viewModel.verifyEvent(reason)
                                else viewModel.rejectEvent(reason)
                                showConfirmDialog = false
                            },
                            enabled = isApproving || reason.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isApproving) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        ) {
                            Text("Confirmar")
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Detalle de Publicación",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A237E)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            event?.let { e ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Imagen del evento
                    AsyncImage(
                        model = e.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Título
                    Text(
                        text = e.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1B2A),
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Badge de Estado
                    StatusBadgeDetail(status = e.verificationStatus)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Descripción
                    Text(
                        text = e.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF4A4A4A),
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(24.dp))

                    // Lista de detalles con iconos
                    DetailRow(icon = Icons.Default.Person, label = "Organizado por:", value = e.organizerName)
                    DetailRow(icon = Icons.Default.Category, label = "Categoría:", value = e.category.name)
                    DetailRow(icon = Icons.Default.Event, label = "Inicia:", value = e.startDate)
                    DetailRow(icon = Icons.Default.EventAvailable, label = "Finaliza:", value = e.endDate)
                    DetailRow(icon = Icons.Default.LocationOn, label = "Lugar:", value = "Parque Central, Calle 45") // Usando ejemplo de imagen o e.location
                    DetailRow(
                        icon = Icons.Default.Groups,
                        label = "Capacidad:",
                        value = "${e.maxAttendees ?: "Ilimitada"} asistentes"
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Botones de acción
                    if (e.verificationStatus == VerificationStatus.PENDING) {
                        Button(
                            onClick = { 
                                isApproving = true
                                reason = ""
                                showConfirmDialog = true 
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Aprobar publicación", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { 
                                isApproving = false
                                reason = ""
                                showConfirmDialog = true 
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                            border = ButtonDefaults.outlinedButtonBorder(true).copy(brush = SolidColor(Color(0xFFF44336))),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Text("Rechazar publicación", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Mensaje si ya no es pendiente
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Esta publicación ya ha sido gestionada.",
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }

            if (result is RequestResult.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun StatusBadgeDetail(status: VerificationStatus) {
    val (label, color) = when (status) {
        VerificationStatus.PENDING -> "Pendiente" to Color(0xFFFFB300)
        VerificationStatus.APPROVED -> "Activa" to Color(0xFF4CAF50)
        VerificationStatus.REJECTED -> "Rechazada" to Color(0xFFF44336)
    }
    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF78909C),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF546E7A)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF263238),
            fontWeight = FontWeight.Medium
        )
    }
}
