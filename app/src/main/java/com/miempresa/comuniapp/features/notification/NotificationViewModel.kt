package com.miempresa.comuniapp.features.notification

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.data.datastore.SessionDataStore
import com.miempresa.comuniapp.domain.model.NotificationItem
import com.miempresa.comuniapp.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel del historial de notificaciones del usuario.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository    : NotificationRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    private val _actionResult = MutableStateFlow<RequestResult?>(null)
    val actionResult: StateFlow<RequestResult?> = _actionResult.asStateFlow()

    val notifications: StateFlow<List<NotificationItem>> =
        sessionDataStore.sessionFlow
            .filterNotNull()
            .flatMapLatest { session ->
                Log.d("NotificationVM", "Suscribiendo notificaciones para: ${session.userId}")
                repository.getNotifications(session.userId)
            }
            .catch { e ->
                Log.e("NotificationVM", "Error en Flow de notificaciones: ${e.message}")
                emit(emptyList())
            }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

    val unreadCount: StateFlow<Int> =
        notifications
            .map { lista -> lista.count { !it.isRead } }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.Eagerly,
                initialValue = 0
            )

    fun markAsRead(notificationId: String) {
        val yaLeida = notifications.value
            .find { it.id == notificationId }
            ?.isRead == true
        if (yaLeida) return

        viewModelScope.launch {
            try {
                repository.markAsRead(notificationId)
            } catch (e: Exception) {
                Log.e("NotificationVM", "Error al marcar como leída: ${e.message}")
            }
        }
    }

    /**
     * Función suspendida para la UI que asegura completar la escritura antes de cambiar de pantalla.
     */
    suspend fun markAsReadSuspended(notificationId: String) {
        val yaLeida = notifications.value
            .find { it.id == notificationId }
            ?.isRead == true
        if (yaLeida) return

        try {
            repository.markAsRead(notificationId)
        } catch (e: Exception) {
            Log.e("NotificationVM", "Error en markAsReadSuspended: ${e.message}")
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            _actionResult.value = RequestResult.Loading
            try {
                repository.deleteNotification(notificationId)
                _actionResult.value = null
            } catch (e: Exception) {
                _actionResult.value = RequestResult.Failure(
                    e.message ?: "Error al eliminar la notificación"
                )
            }
        }
    }

    /**
     * Marca todas las notificaciones no leídas como leídas de forma atómica.
     * Gatillado únicamente al pulsar el botón de la barra superior.
     */
    fun markAllAsRead() {
        val noLeidas = notifications.value.filter { !it.isRead }
        if (noLeidas.isEmpty()) return

        viewModelScope.launch {
            noLeidas.forEach { notificacion ->
                runCatching { repository.markAsRead(notificacion.id) }
                    .onFailure { e ->
                        Log.e(
                            "NotificationVM",
                            "Error al marcar ${notificacion.id}: ${e.message}"
                        )
                    }
            }
        }
    }

    fun resetActionResult() { _actionResult.value = null }
}