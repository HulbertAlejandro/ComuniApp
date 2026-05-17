package com.miempresa.comuniapp.features.user.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.AttendanceStatus
import com.miempresa.comuniapp.domain.model.User
import com.miempresa.comuniapp.domain.repository.AttendanceRepository
import com.miempresa.comuniapp.domain.repository.EventRepository
import com.miempresa.comuniapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel de la pantalla de perfil del usuario.
 *
 * Todos los datos se derivan reactivamente de la sesión activa en [SessionDataStore],
 * lo que garantiza que cualquier cambio (edición de perfil, nuevas asistencias, etc.)
 * se refleje automáticamente en la UI sin necesidad de recargar manualmente.
 *
 * @param repository           Repositorio de usuarios (Firestore).
 * @param eventRepository      Repositorio de eventos para contar los creados.
 * @param attendanceRepository Repositorio de asistencias para contar las confirmadas.
 * @param sessionDataStore     Almacén local de la sesión del usuario autenticado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UserRepository,
    private val eventRepository: EventRepository,
    private val attendanceRepository: AttendanceRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    /**
     * Usuario actualmente autenticado, derivado reactivamente del repositorio.
     * Se actualiza automáticamente cuando [UserEditViewModel] llama a [UserRepository.update].
     */
    val user: StateFlow<User?> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                repository.users.map { list -> list.find { it.id == session.userId } }
            }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    /**
     * Número de eventos creados por el usuario actual.
     * Se actualiza reactivamente cuando cambia la colección de eventos en Firestore.
     */
    val createdEventsCount: StateFlow<Int> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                eventRepository.events.map { events ->
                    events.count { it.ownerId == session.userId }
                }
            }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    /**
     * Número de eventos a los que el usuario confirmó su asistencia.
     * Filtra únicamente las asistencias con estado [AttendanceStatus.CONFIRMED].
     */
    val attendedEventsCount: StateFlow<Int> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                attendanceRepository.attendances.map { attendances ->
                    attendances.count {
                        it.userId == session.userId &&
                                it.status == AttendanceStatus.CONFIRMED
                    }
                }
            }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    /**
     * Número de eventos guardados (marcados como "me interesa") por el usuario.
     * Se deriva de la lista de IDs de interés almacenada en el documento del usuario.
     */
    val savedEventsCount: StateFlow<Int> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                repository.users.map { users ->
                    users.find { it.id == session.userId }
                        ?.interestedEventIds
                        ?.size ?: 0
                }
            }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    /**
     * Puntos de reputación del usuario actual.
     * Derivado de [user] para evitar duplicar la consulta a Firestore.
     */
    val points: StateFlow<Int> =
        user
            .map { it?.reputation?.points ?: 0 }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    /**
     * Cierra la sesión del usuario limpiando el [SessionDataStore].
     * La navegación al login es responsabilidad de la Screen.
     */
    fun logout() {
        viewModelScope.launch {
            sessionDataStore.clearSession()
        }
    }
}