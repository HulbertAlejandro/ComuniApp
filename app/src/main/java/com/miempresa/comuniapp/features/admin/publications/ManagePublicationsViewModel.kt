package com.miempresa.comuniapp.features.admin.publications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.VerificationStatus
import com.miempresa.comuniapp.domain.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PublicationFilter {
    ALL, PENDING, APPROVED, REJECTED
}

@HiltViewModel
class ManagePublicationsViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _allPublications = MutableStateFlow<List<Event>>(emptyList())

    private val _filteredPublications = MutableStateFlow<List<Event>>(emptyList())
    val filteredPublications: StateFlow<List<Event>> = _filteredPublications.asStateFlow()

    private val _activeFilter = MutableStateFlow(PublicationFilter.ALL)
    val activeFilter: StateFlow<PublicationFilter> = _activeFilter.asStateFlow()

    private val _result = MutableStateFlow<RequestResult?>(null)
    val result: StateFlow<RequestResult?> = _result.asStateFlow()

    init {
        loadPublications()
    }

    fun loadPublications() {
        viewModelScope.launch {
            _result.value = RequestResult.Loading
            eventRepository.events.collect { events ->
                _allPublications.value = events
                applyFilter(_activeFilter.value)
                _result.value = RequestResult.Success("Publicaciones cargadas")
            }
        }
    }

    fun onFilterSelected(filter: PublicationFilter) {
        _activeFilter.value = filter
        applyFilter(filter)
    }

    private fun applyFilter(filter: PublicationFilter) {
        _filteredPublications.value = when (filter) {
            PublicationFilter.ALL      -> _allPublications.value
            PublicationFilter.PENDING  -> _allPublications.value
                .filter { it.verificationStatus == VerificationStatus.PENDING }
            PublicationFilter.APPROVED -> _allPublications.value
                .filter { it.verificationStatus == VerificationStatus.APPROVED }
            PublicationFilter.REJECTED -> _allPublications.value
                .filter { it.verificationStatus == VerificationStatus.REJECTED }
        }
    }

    fun resetResult() {
        _result.value = null
    }
}