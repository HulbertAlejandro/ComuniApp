package com.miempresa.comuniapp.features.admin.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.VerificationStatus
import com.miempresa.comuniapp.domain.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class HistoryFilter {
    ALL, VERIFIED, REJECTED
}

@HiltViewModel
class ModerationHistoryViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _activeFilter = MutableStateFlow(HistoryFilter.ALL)
    val activeFilter = _activeFilter.asStateFlow()

    private val _result = MutableStateFlow<RequestResult?>(null)
    val result = _result.asStateFlow()

    val filteredEvents: StateFlow<List<Event>> = combine(
        eventRepository.events,
        _searchText,
        _activeFilter
    ) { events, query, filter ->
        events.filter { event ->
            val matchesStatus = when (filter) {
                HistoryFilter.ALL -> event.verificationStatus != VerificationStatus.PENDING
                HistoryFilter.VERIFIED -> event.verificationStatus == VerificationStatus.APPROVED
                HistoryFilter.REJECTED -> event.verificationStatus == VerificationStatus.REJECTED
            }
            val matchesSearch = event.title.contains(query, ignoreCase = true)
            matchesStatus && matchesSearch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    fun onFilterSelected(filter: HistoryFilter) {
        _activeFilter.value = filter
    }

    fun resetResult() {
        _result.value = null
    }
}
