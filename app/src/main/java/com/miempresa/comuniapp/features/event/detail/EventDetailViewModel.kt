package com.miempresa.comuniapp.features.event.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val repository: EventRepository,
    private val userRepository: UserRepository,
    private val sessionDataStore: SessionDataStore   // ✅ Inyectar sesión
) : ViewModel() {

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private val _organizer = MutableStateFlow<User?>(null)
    val organizer: StateFlow<User?> = _organizer.asStateFlow()

    // ✅ Intereses del usuario logueado (para mostrar estado del botón)
    private val _interestedEventIds = MutableStateFlow<Set<String>>(emptySet())
    val interestedEventIds: StateFlow<Set<String>> = _interestedEventIds.asStateFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            // 1. Cargar el evento (interestCount total viene del propio Event)
            val loaded = repository.findById(eventId)
            _event.value = loaded

            // 2. Cargar organizador
            loaded?.let {
                _organizer.value = userRepository.findById(it.ownerId)
            }

            // 3. ✅ Cargar intereses desde el usuario de sesión (no global)
            sessionDataStore.sessionFlow
                .filterNotNull()
                .collectLatest { session ->
                    val ids = userRepository.getUserInterestedEventIds(session.userId)
                    _interestedEventIds.value = ids
                }
        }
    }
}