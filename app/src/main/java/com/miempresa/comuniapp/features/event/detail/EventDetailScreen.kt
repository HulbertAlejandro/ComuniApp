package com.miempresa.comuniapp.features.event.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.miempresa.comuniapp.features.event.EventViewModel
import com.miempresa.comuniapp.features.event.components.EventStatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    paddingValues: PaddingValues,
    onNavigateBack: () -> Unit,
    viewModel: EventViewModel = hiltViewModel()
) {
    val event = remember(eventId) { viewModel.findById(eventId) }

    event?.let {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            AsyncImage(
                model = it.imageUrl,
                contentDescription = "Imagen del evento",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )

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

                    DetailItem("Descripción", it.description)
                    DetailItem("Categoría", it.category)
                    DetailItem("Ubicación", it.location.toString())
                    DetailItem("Fecha inicio", it.startDate)
                    DetailItem("Fecha fin", it.endDate)

                    DetailItem(
                        "Asistentes",
                        "${it.currentAttendees} / ${it.maxAttendees ?: "∞"}"
                    )

                    DetailItem(
                        "Organizador",
                        "${it.organizerName} - ${it.organizerLevel}"
                    )

                    EventStatusChip(it.eventStatus)
                }
            }

            Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver")
            }
        }
    } ?: Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Text("Evento no encontrado")
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}