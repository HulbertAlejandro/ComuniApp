package com.miempresa.comuniapp.features.user.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.features.event.components.EventCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    paddingValues: PaddingValues,
    onEventClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val historyEvents by viewModel.historyEvents.collectAsState()
    val usersMap by viewModel.usersMap.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF212121)
                        )
                    }
                    Text(
                        text = "Historial de Eventos",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF212121),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (historyEvents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aún no tienes eventos finalizados en tu historial",
                        fontSize = 15.sp,
                        color = Color(0xFF9E9E9E),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(historyEvents, key = { it.id }) { event ->
                        // Buscamos al organizador en el mapa para que aparezca nombre y nivel
                        val organizer = usersMap[event.ownerId]

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEventClick(event.id) }
                        ) {
                            EventCard(
                                event = event,
                                organizer = organizer, // Ya no es null, ahora tiene los datos
                                hasVoted = false,
                                onInterestedClick = {},
                                showInterestButton = false
                            )
                        }
                    }
                }
            }
        }
    }
}