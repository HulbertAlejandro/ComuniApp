package com.miempresa.comuniapp.features.user.savedevents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.Event
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SavedEventsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    // Intereses del usuario actual (para verificar estado)
    private val _currentUserId = MutableStateFlow<String?>(null)
    val userInterests: StateFlow<Set<String>> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                _currentUserId.value = session.userId
                userRepository.users.map { users ->
                    users.find { it.id == session.userId }?.interestedEventIds ?: emptySet()
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptySet()
            )

    // ✅ REACTIVO: Observa cambios en interestedEventIds y en repository.events
    val savedEvents: StateFlow<List<Event>> = combine(
        userInterests,  // Cambios en intereses del usuario
        eventRepository.events  // Cambios en eventos (como interestCount actualizado)
    ) { interestedIds, allEvents ->
        allEvents.filter { it.id in interestedIds }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Función para quitar interés (elimina de la lista por reactividad)
    fun removeInterest(eventId: String) {
        val userId = _currentUserId.value ?: return

        viewModelScope.launch {
            userRepository.removeInterestFromUser(userId, eventId)
            eventRepository.removeInterest(eventId)
        }
    }
}
