package com.miempresa.comuniapp.features.user.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miempresa.comuniapp.features.event.EventViewModel
import com.miempresa.comuniapp.features.event.components.EventCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    paddingValues: PaddingValues,
    onUserClick: (String) -> Unit,
    viewModel: EventViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Título de la sección
        Text(
            text = "Eventos cerca de ti",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Fila de Filtros (Mockup)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = true,
                    onClick = { },
                    label = { Text("Cerca de mí") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = { },
                    label = { Text("Categoría") }
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = { },
                    label = { Text("Fecha") }
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = { },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Filtros")
                        }
                    }
                )
            }
        }

        // Lista de Eventos
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(events) { event ->
                EventCard(
                    event = event,
                    onInterestedClick = {
                        // Acción simulada
                    }
                )
            }
        }
    }
}
