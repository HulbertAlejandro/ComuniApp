package com.miempresa.comuniapp.features.event.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.domain.model.Category
import com.miempresa.comuniapp.features.event.components.EventCard
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    paddingValues: PaddingValues,
    onEventClick: (String) -> Unit,
    viewModel: EventListViewModel = hiltViewModel()
) {
    val events        by viewModel.events.collectAsState()
    val usersMap      by viewModel.usersMap.collectAsState()
    val votedEventIds by viewModel.votedEventIds.collectAsState()

    // ── DatePicker ────────────────────────────────────────────────────────────
    val datePickerState = rememberDatePickerState()
    if (viewModel.showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { viewModel.showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val date = java.time.LocalDate.ofEpochDay(millis / 86_400_000L)
                        viewModel.filterByDate(date)
                    }
                    viewModel.showDatePicker = false
                }) { Text(stringResource(R.string.event_list_dialog_date_apply)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.filterByDate(null)
                    viewModel.showDatePicker = false
                }) { Text(stringResource(R.string.event_list_dialog_date_clear)) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    // ── Category dialog ───────────────────────────────────────────────────────
    if (viewModel.showFiltersDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showFiltersDialog = false },
            title = { Text(stringResource(R.string.event_list_dialog_category_title)) },
            text = {
                Column {
                    Category.entries.forEach { category ->
                        val isSelected = viewModel.selectedCategory == category
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = category.name.lowercase().replaceFirstChar { it.uppercase() },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            modifier = Modifier
                                .clickable {
                                    viewModel.filterByCategory(category)
                                    viewModel.showFiltersDialog = false
                                }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.filterByCategory(null)
                    viewModel.showFiltersDialog = false
                }) { Text(stringResource(R.string.event_list_dialog_category_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showFiltersDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    // ── Scaffold ──────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                // Título centrado
                Text(
                    text = stringResource(R.string.event_list_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF212121),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 4.dp)
                )

                // SearchBar
                SearchBar(
                    query = viewModel.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onSearch = {},
                    active = false,
                    onActiveChange = {},
                    placeholder = { Text(stringResource(R.string.event_list_search_placeholder), color = Color(0xFF9E9E9E)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.event_list_search_icon_description), tint = Color(0xFF757575))
                    },
                    trailingIcon = {
                        AnimatedVisibility(visible = viewModel.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.event_list_clear_icon_description), tint = Color(0xFF757575))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    colors = SearchBarDefaults.colors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(14.dp)
                ) {}

                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE0E0E0))
            }
        },
        containerColor = Color.White
    ) { innerPadding ->

        Column(modifier = Modifier.padding(innerPadding)) {

            // ── Chips de filtro ───────────────────────────────────────────────
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    EventFilterChip(
                        label = if (viewModel.selectedFilter == stringResource(R.string.event_list_filter_nearby_label)) "📍 " + stringResource(R.string.event_list_filter_nearby_label) else stringResource(R.string.event_list_filter_nearby_label),
                        isSelected = viewModel.selectedFilter == stringResource(R.string.event_list_filter_nearby_label),
                        onClick = { viewModel.toggleProximityFilter() }
                    )
                }
                item {
                    val favActive by viewModel.favoriteCategoriesFilter.collectAsState()
                    EventFilterChip(
                        label = if (favActive) "⭐ " + stringResource(R.string.event_list_filter_recommended_label) else stringResource(R.string.event_list_filter_recommended_label),
                        isSelected = favActive,
                        onClick = { viewModel.toggleFavoriteCategoriesFilter() }
                    )
                }
                item {
                    val catLabel = viewModel.selectedCategory
                        ?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: stringResource(R.string.event_list_filter_category)
                    EventFilterChip(
                        label = catLabel,
                        isSelected = viewModel.selectedFilter == stringResource(R.string.event_list_filter_category),
                        onClick = { viewModel.showFiltersDialog = true }
                    )
                }
                item {
                    val dateLabel = viewModel.selectedDate
                        ?.format(DateTimeFormatter.ofPattern("d MMM", Locale("es", "ES")))
                        ?.replaceFirstChar { it.uppercase() } ?: stringResource(R.string.event_list_filter_date)
                    EventFilterChip(
                        label = dateLabel,
                        isSelected = viewModel.selectedFilter == stringResource(R.string.event_list_filter_date),
                        onClick = { viewModel.showDatePicker = true }
                    )
                }
                if (viewModel.selectedFilter != null || viewModel.searchQuery.isNotBlank()) {
                    item {
                        EventFilterChip(
                            label = "✕ " + stringResource(R.string.event_list_filter_clear),
                            isSelected = false,
                            onClick = { viewModel.clearAllFilters() },
                            tintWhenInactive = Color(0xFFC62828)
                        )
                    }
                }
            }

            // ── Lista / Estado vacío ──────────────────────────────────────────
            if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.event_list_empty), fontSize = 15.sp, color = Color(0xFF9E9E9E))
                        if (viewModel.selectedFilter != null || viewModel.searchQuery.isNotBlank()) {
                            TextButton(onClick = { viewModel.clearAllFilters() }) { Text(stringResource(R.string.event_list_clear_filters)) }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(events, key = { it.id }) { event ->
                        EventCard(
                            event = event,
                            organizer = usersMap[event.ownerId],
                            hasVoted = votedEventIds.contains(event.id),
                            onInterestedClick = { viewModel.onInterested(event.id) },
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

@Composable
fun EventFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tintWhenInactive: Color = Color(0xFF616161)
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF212121),
            selectedLabelColor = Color.White,
            containerColor = Color.White,
            labelColor = tintWhenInactive
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = if (isSelected) Color(0xFF212121) else Color(0xFFBDBDBD),
            selectedBorderColor = Color(0xFF212121),
            borderWidth = if (isSelected) 1.5.dp else 1.dp,
            selectedBorderWidth = 1.5.dp
        ),
        shape = RoundedCornerShape(50.dp)
    )
}