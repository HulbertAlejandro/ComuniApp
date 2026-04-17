package com.miempresa.comuniapp.features.user.savedevents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.features.event.components.EventCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedEventsScreen(
    paddingValues: PaddingValues,
    onEventClick: (String) -> Unit,
    viewModel: SavedEventsViewModel = hiltViewModel()
) {
    val events by viewModel.savedEvents.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                Text(
                    text = "Eventos Guardados",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF212121),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp)
                )
            }
        },
        containerColor = Color.White
    ) { innerPadding ->

        Column(modifier = Modifier.padding(innerPadding)) {
            if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No has guardado eventos aún", fontSize = 15.sp, color = Color(0xFF9E9E9E))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(events, key = { it.id }) { event ->
                        EventCard(
                            event = event,
                            organizer = null,
                            hasVoted = true, // Siempre true ya que están guardados
                            onInterestedClick = { viewModel.removeInterest(event.id) }, // Quitar interés
                            modifier = Modifier.clickable {
                                onEventClick(event.id)
                            }
                        )
                    }
                }
            }
        }
    }
}
