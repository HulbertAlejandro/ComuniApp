package com.miempresa.comuniapp.features.event.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miempresa.comuniapp.features.event.EventViewModel
import com.miempresa.comuniapp.features.event.components.EventCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    paddingValues: PaddingValues,
    onEventClick: (String) -> Unit,
    viewModel: EventViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState(initial = emptyList())

    if (events.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(events) { event ->
                EventCard(
                    event = event,
                    onInterestedClick = {},
                    modifier = Modifier
                        .clickable { onEventClick(event.id) }
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}