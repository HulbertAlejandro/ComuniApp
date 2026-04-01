package com.miempresa.comuniapp.features.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    val events: StateFlow<List<Event>> = repository.events
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun findById(id: String): Event? {
        return repository.findById(id)
    }
}
