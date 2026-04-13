package com.miempresa.comuniapp.features.event.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val repository: EventRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private val _organizer = MutableStateFlow<User?>(null)
    val organizer: StateFlow<User?> = _organizer.asStateFlow()

    // ✅ CORREGIDO: Colectar el Flow correctamente para SOLO LEER
    private val _interestedEventIds = MutableStateFlow<Set<String>>(emptySet())
    val interestedEventIds: StateFlow<Set<String>> = _interestedEventIds.asStateFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            val loaded = repository.findById(eventId)
            _event.value = loaded

            loaded?.let {
                _organizer.value = userRepository.findById(it.ownerId)
            }

            // ✅ SOLUCIÓN: Colectar el Flow UNA VEZ al cargar
            repository.getInterestedEventIds().collect { ids ->
                _interestedEventIds.value = ids
            }
        }
    }

    // ✅ NO agregar toggleInterest() - solo lectura
}